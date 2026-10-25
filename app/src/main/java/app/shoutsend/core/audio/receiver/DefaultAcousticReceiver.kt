package app.shoutsend.core.audio.receiver

import android.os.Process
import app.shoutsend.core.audio.AudioConfig
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.demod.ReceiverPipeline
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.spectrum.AnalysisBand
import app.shoutsend.modem.spectrum.SpectrumAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

/**
 * Production implementation of [AcousticReceiver].
 *
 * Employs a dual-thread architecture:
 * 1. High-priority real-time audio capture thread reading [PcmSource] into a lock-free [SpscRingBuffer].
 * 2. Asynchronous processing loop consuming the ring buffer, feeding the [ReceiverPipeline] and [SpectrumAnalyzer],
 *    evaluating hardware silence and acoustic clipping, and emitting diagnostics.
 */
class DefaultAcousticReceiver(
    private val pcmSourceFactory: () -> PcmSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) : AcousticReceiver {

    private val _events = MutableSharedFlow<ReceiveEvent>(replay = 32, extraBufferCapacity = 64)
    override val events: SharedFlow<ReceiveEvent> = _events.asSharedFlow()

    private val _diagnostics = MutableStateFlow(ReceiverDiagnostics())
    override val diagnostics: StateFlow<ReceiverDiagnostics> = _diagnostics.asStateFlow()

    private var _spectrumChannel = Channel<FloatArray>(capacity = 60, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val spectrumRows: ReceiveChannel<FloatArray>
        get() = _spectrumChannel

    @Volatile
    override var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)? = null

    private val active = AtomicBoolean(false)
    override val isRunning: Boolean
        get() = active.get()

    private var currentBand: Band = Band.RETRO
    private var currentPassphrase: String? = null

    private var readerThread: Thread? = null
    private var processingJob: Job? = null
    private var activeSource: PcmSource? = null

    private val ringBuffer = SpscRingBuffer(capacity = 262144)

    @Synchronized
    override fun start(band: Band, passphrase: String?) {
        if (active.get()) {
            if (currentBand == band && currentPassphrase == passphrase) {
                return // Already running with identical config
            }
            stop()
        }

        currentBand = band
        currentPassphrase = passphrase
        ringBuffer.clear()

        val source = pcmSourceFactory()
        activeSource = source
        source.start()

        val sampleRate = source.actualSampleRate
        val audioSpec = if (sampleRate == 44100) AudioSpec.RATE_44K else AudioSpec.RATE_48K
        val info = source.sourceInfo

        _diagnostics.value = ReceiverDiagnostics(
            sourceName = info.sourceName,
            sampleRateHz = sampleRate,
            effects = info.effects
        )

        // Reset spectrum channel if closed
        if (_spectrumChannel.isClosedForSend) {
            _spectrumChannel = Channel(capacity = 60, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }

        // Initialize SpectrumAnalyzer
        val analysisBand = when (band) {
            Band.RETRO -> AnalysisBand(startHz = 1000.0, endHz = 4000.0, displayBins = 256)
            Band.STEALTH -> AnalysisBand(startHz = 16000.0, endHz = 22000.0, displayBins = 256)
        }
        val spectrumAnalyzer = SpectrumAnalyzer(
            spec = audioSpec,
            band = analysisBand,
            listener = { _, row ->
                _spectrumChannel.trySend(row.copyOf())
            }
        )

        // Initialize ReceiverPipeline
        val pipeline = ReceiverPipeline(
            band = band,
            audioSpec = audioSpec,
            listener = { event ->
                _events.tryEmit(event)
            },
            passphrase = passphrase
        )

        active.set(true)

        // 1. Launch high-priority reader thread
        val chunkSize = AudioConfig.chunkSamples(sampleRate)
        readerThread = Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            } catch (_: Exception) {}

            val readBuf = ShortArray(chunkSize)
            while (active.get() && !Thread.currentThread().isInterrupted) {
                val read = source.read(readBuf, 0, chunkSize)
                if (read > 0) {
                    ringBuffer.write(readBuf, 0, read)
                } else if (read < 0) {
                    // AudioRecord read error or uninitialized
                    break
                }
            }
        }, "ShoutSend-AudioRecord-Reader").apply {
            isDaemon = true
            start()
        }

        // 2. Launch processing coroutine
        processingJob = scope.launch(Dispatchers.Default) {
            val processBuf = ShortArray(chunkSize)
            val window500msSamples = sampleRate / 2
            var clippingWindowCount = 0
            var clippedSamplesInWindow = 0
            var continuousZeroSamples = 0L
            var silentEmitted = false

            while (isActive && active.get()) {
                val readCount = ringBuffer.read(processBuf, 0, chunkSize)
                if (readCount == 0) {
                    kotlinx.coroutines.delay(5)
                    continue
                }

                // Diagnostics calculation
                var peakMag = 0
                for (i in 0 until readCount) {
                    val s = processBuf[i].toInt()
                    val absVal = abs(s)
                    if (absVal > peakMag) peakMag = absVal

                    // Silence detection (digital zeros)
                    if (s == 0) {
                        continuousZeroSamples++
                    } else {
                        continuousZeroSamples = 0
                    }

                    // Clipping detection (magnitude >= 32440 is 99% of 32767)
                    if (absVal >= 32440) {
                        clippedSamplesInWindow++
                    }
                    clippingWindowCount++

                    if (clippingWindowCount >= window500msSamples) {
                        val clipFraction = clippedSamplesInWindow.toDouble() / clippingWindowCount
                        val isClipping = clipFraction > 0.001
                        if (isClipping) {
                            _events.tryEmit(ReceiveEvent.FrameFailed("InputTooLoud"))
                        }
                        _diagnostics.value = _diagnostics.value.copy(
                            isClipping = isClipping,
                            overrunCount = ringBuffer.overrunCount
                        )
                        clippingWindowCount = 0
                        clippedSamplesInWindow = 0
                    }
                }

                // Handle 2-second silence alert
                if (continuousZeroSamples >= 2L * sampleRate) {
                    if (!silentEmitted) {
                        silentEmitted = true
                        _diagnostics.value = _diagnostics.value.copy(
                            isSilent = true,
                            overrunCount = ringBuffer.overrunCount
                        )
                        _events.tryEmit(ReceiveEvent.Lost("MicrophoneSilent"))
                    }
                } else if (silentEmitted) {
                    silentEmitted = false
                    _diagnostics.value = _diagnostics.value.copy(
                        isSilent = false,
                        overrunCount = ringBuffer.overrunCount
                    )
                }

                val peakDbfs = if (peakMag > 0) {
                    (20.0 * log10(peakMag / 32768.0)).toFloat().coerceIn(-90f, 0f)
                } else -90f

                _diagnostics.value = _diagnostics.value.copy(
                    peakLevelDbfs = peakDbfs,
                    overrunCount = ringBuffer.overrunCount
                )

                // Feed Spectrum Analyzer, Demodulator, and optional tap
                spectrumAnalyzer.push(processBuf, 0, readCount)
                pipeline.push(processBuf, 0, readCount)
                rawAudioTap?.invoke(processBuf, 0, readCount)
            }
        }
    }

    @Synchronized
    override fun stop() {
        if (!active.getAndSet(false)) return

        readerThread?.interrupt()
        readerThread = null

        processingJob?.cancel()
        processingJob = null

        try {
            activeSource?.stop()
            activeSource?.release()
        } catch (_: Exception) {}
        activeSource = null

        ringBuffer.clear()
        _diagnostics.value = _diagnostics.value.copy(peakLevelDbfs = -90f, isClipping = false)
    }
}
