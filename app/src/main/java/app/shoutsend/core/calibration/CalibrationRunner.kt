package app.shoutsend.core.calibration

import android.content.Context
import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.getOutputRouteInfo
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.calibration.CalibrationAnalyzer
import app.shoutsend.modem.calibration.TestSequence
import app.shoutsend.modem.profile.Band
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Orchestrator for full-duplex acoustic loopback self-calibration.
 *
 * EXPLICIT HALF-DUPLEX EXCEPTION (Step 29):
 * ShoutSend strictly enforces half-duplex operation for all messaging sessions to prevent
 * acoustic feedback and self-demodulation.
 * Calibration is the SOLE ARCHITECTURAL EXCEPTION where [AcousticReceiver] and
 * [AcousticTransmitter] run simultaneously to profile transducer frequency response.
 */
class CalibrationRunner(
    private val receiver: AcousticReceiver,
    private val transmitter: AcousticTransmitter,
    private val audioSpec: AudioSpec = AudioSpec.RATE_48K,
    private val routeInfoProvider: () -> app.shoutsend.core.audio.transmitter.OutputRouteInfo
) {
    constructor(
        context: Context,
        receiver: AcousticReceiver,
        transmitter: AcousticTransmitter,
        audioSpec: AudioSpec = AudioSpec.RATE_48K
    ) : this(
        receiver = receiver,
        transmitter = transmitter,
        audioSpec = audioSpec,
        routeInfoProvider = { getOutputRouteInfo(context) }
    )

    /**
     * Executes the calibration procedure:
     * 1. Inspects audio output routing and media volume to assemble hardware warnings.
     * 2. Starts [AcousticReceiver] with raw audio tap to record ambient room noise and loopback tones.
     * 3. Pauses 1000 ms to establish ambient noise baseline.
     * 4. Transmits [TestSequence] through [AcousticTransmitter].
     * 5. Stops [AcousticReceiver] and passes recorded audio to [CalibrationAnalyzer].
     * 6. Returns structured [CalibrationResult].
     */
    suspend fun runCalibration(
        onProgress: (progress: Float) -> Unit = {}
    ): CalibrationResult = withContext(Dispatchers.Default) {
        val warnings = ArrayList<String>()
        val route = routeInfoProvider()

        if (route.hasHeadset || route.hasBluetooth || route.hasUsb) {
            warnings.add("HEADPHONES_CONNECTED")
        }
        if (route.volumeFraction < 0.5f) {
            warnings.add("VOLUME_LOW")
        }

        val testSequence = TestSequence(audioSpec)
        val recordedChunks = ConcurrentLinkedQueue<ShortArray>()
        val totalRecordedSamples = AtomicInteger(0)

        // Attach audio tap to capture raw microphone samples
        receiver.rawAudioTap = { buffer, offset, count ->
            val slice = ShortArray(count)
            System.arraycopy(buffer, offset, slice, 0, count)
            recordedChunks.add(slice)
            totalRecordedSamples.addAndGet(count)
        }

        try {
            onProgress(0.05f)

            // 1. Start acoustic receiver
            receiver.start(band = Band.RETRO)

            // 2. Wait 1 second for ambient noise profiling
            delay(1000)
            currentCoroutineContext().ensureActive()
            onProgress(0.15f)

            // 3. Play test sequence through transmitter
            transmitter.transmit(testSequence) { prog ->
                // Progress mapped from 0.15f to 0.85f
                val p = 0.15f + (prog.fraction * 0.70f)
                onProgress(p.coerceIn(0.15f, 0.85f))
            }

            // Short settling delay to capture acoustic tail
            delay(500)
            currentCoroutineContext().ensureActive()
            onProgress(0.90f)

        } catch (e: CancellationException) {
            throw e
        } finally {
            // Cleanly stop receiver and disconnect tap
            receiver.rawAudioTap = null
            receiver.stop()
        }

        onProgress(0.95f)

        // Assemble flattened ShortArray
        val totalCount = totalRecordedSamples.get()
        val pcmShorts = ShortArray(totalCount)
        var cursor = 0
        for (chunk in recordedChunks) {
            val toCopy = minOf(chunk.size, totalCount - cursor)
            if (toCopy > 0) {
                System.arraycopy(chunk, 0, pcmShorts, cursor, toCopy)
                cursor += toCopy
            }
        }

        // Run pure analyzer
        val analysis = CalibrationAnalyzer.analyze(pcmShorts, audioSpec)
        onProgress(1.0f)

        CalibrationResult(
            timestampMs = System.currentTimeMillis(),
            retroReport = BandReportSummary.fromReport(analysis.retroReport),
            stealthReport = BandReportSummary.fromReport(analysis.stealthReport),
            recommendedBand = analysis.recommendation.recommendedBand,
            recommendedSpeed = analysis.recommendation.recommendedSpeed,
            reasonCode = analysis.recommendation.reason.name,
            warnings = warnings
        )
    }
}
