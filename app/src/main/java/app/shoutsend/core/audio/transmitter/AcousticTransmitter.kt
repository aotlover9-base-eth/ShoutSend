package app.shoutsend.core.audio.transmitter

import app.shoutsend.core.audio.AudioConfig
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.modulator.Modulator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time transmission progress information.
 */
data class TransmitProgress(
    val samplesWritten: Long,
    val totalSamples: Long,
    val fraction: Float,
    val elapsedSeconds: Double,
    val remainingSeconds: Double
)

/**
 * Outcome of an acoustic transmission attempt.
 */
sealed class TransmitResult {
    data class Success(
        val totalFrames: Long,
        val durationMs: Long,
        val underrunCount: Int
    ) : TransmitResult()

    data class Cancelled(
        val framesWritten: Long,
        val durationMs: Long
    ) : TransmitResult()

    data class Failure(
        val error: Throwable,
        val framesWritten: Long
    ) : TransmitResult()
}

/**
 * Interface contract for playing modulated acoustic signals through the device speaker.
 */
interface AcousticTransmitter {
    suspend fun transmit(
        source: AudioStreamSource,
        onProgress: (progress: TransmitProgress) -> Unit = {}
    ): TransmitResult
}

/**
 * Default production implementation of [AcousticTransmitter] streaming audio through [PcmSink].
 *
 * Implements:
 * 1. Dedicated single-thread audio dispatcher with real-time audio thread priority.
 * 2. Reused buffers of [AudioConfig.chunkSamples] with zero heap allocations per chunk (Rule 22).
 * 3. Smooth 10 ms cosine fade-out on cancellation to prevent speaker pop/click.
 * 4. Automatic release of hardware resources in finally blocks.
 */
class DefaultAcousticTransmitter(
    private val sinkFactory: (sampleRateHz: Int) -> PcmSink = { rate -> AudioTrackSink(rate) },
    private val dispatcher: CoroutineDispatcher = defaultAudioDispatcher()
) : AcousticTransmitter {

    override suspend fun transmit(
        source: AudioStreamSource,
        onProgress: (progress: TransmitProgress) -> Unit
    ): TransmitResult = withContext(dispatcher) {
        val sampleRateHz = source.audioSpec.sampleRateHz
        val totalSamples = source.totalSamples
        val totalSeconds = source.durationSeconds
        val sink = sinkFactory(sampleRateHz)

        val chunkSize = AudioConfig.chunkSamples(sampleRateHz)
        val floatChunk = FloatArray(chunkSize)
        val pcmChunk = ShortArray(chunkSize)

        val startTime = System.currentTimeMillis()
        var samplesWritten: Long = 0L
        var lastProgressTime: Long = 0L
        var initialUnderrun = 0

        try {
            sink.play()
            initialUnderrun = sink.getUnderrunCount()

            while (samplesWritten < totalSamples) {
                // Check coroutine cancellation
                currentCoroutineContext().ensureActive()

                val toRender = min(chunkSize.toLong(), totalSamples - samplesWritten).toInt()
                source.render(
                    startSample = samplesWritten,
                    out = floatChunk,
                    outOffset = 0,
                    count = toRender
                )

                // Convert float [-1.0f, 1.0f] to 16-bit signed PCM
                for (i in 0 until toRender) {
                    val sample = (floatChunk[i] * 32767.0f).toInt().coerceIn(-32768, 32767)
                    pcmChunk[i] = sample.toShort()
                }

                val written = sink.write(pcmChunk, 0, toRender)
                if (written < 0) {
                    throw IllegalStateException("PcmSink write failed with error code: $written")
                }
                samplesWritten += written

                // Throttle progress updates to at most 20 Hz (50 ms)
                val now = System.currentTimeMillis()
                if (now - lastProgressTime >= 50L || samplesWritten >= totalSamples) {
                    lastProgressTime = now
                    val fraction = (samplesWritten.toFloat() / totalSamples.toFloat()).coerceIn(0.0f, 1.0f)
                    val elapsedSec = samplesWritten.toDouble() / sampleRateHz.toDouble()
                    val remainingSec = max(0.0, totalSeconds - elapsedSec)
                    onProgress(
                        TransmitProgress(
                            samplesWritten = samplesWritten,
                            totalSamples = totalSamples,
                            fraction = fraction,
                            elapsedSeconds = elapsedSec,
                            remainingSeconds = remainingSec
                        )
                    )
                }
            }

            // Await playback head catching up to total frames written
            val remainingPlayMs = ((totalSamples - sink.getPlaybackHeadPosition()) * 1000L / sampleRateHz).coerceAtLeast(0L)
            val deadline = System.currentTimeMillis() + remainingPlayMs + 1000L

            while (sink.getPlaybackHeadPosition() < totalSamples && System.currentTimeMillis() < deadline) {
                delay(10)
            }

            val finalUnderruns = max(0, sink.getUnderrunCount() - initialUnderrun)
            val durationMs = System.currentTimeMillis() - startTime
            TransmitResult.Success(
                totalFrames = samplesWritten,
                durationMs = durationMs,
                underrunCount = finalUnderruns
            )

        } catch (cancellation: CancellationException) {
            // Apply 10ms smooth fade-out chunk to eliminate click
            applyClickFreeFadeOut(sink, pcmChunk, sampleRateHz)
            val durationMs = System.currentTimeMillis() - startTime
            TransmitResult.Cancelled(
                framesWritten = samplesWritten,
                durationMs = durationMs
            )
        } catch (error: Throwable) {
            val durationMs = System.currentTimeMillis() - startTime
            TransmitResult.Failure(
                error = error,
                framesWritten = samplesWritten
            )
        } finally {
            sink.pause()
            sink.flush()
            sink.stop()
            sink.release()
        }
    }

    /**
     * Synthesizes and writes a 10 ms linear fade-out to prevent speaker pop/click on cancellation.
     */
    private fun applyClickFreeFadeOut(sink: PcmSink, scratchBuffer: ShortArray, sampleRateHz: Int) {
        try {
            val fadeSamples = min(scratchBuffer.size, (sampleRateHz * 10) / 1000)
            if (fadeSamples > 0) {
                // Smooth cosine / linear ramp down
                val lastSample = if (scratchBuffer.isNotEmpty()) scratchBuffer[0].toFloat() else 0.0f
                for (i in 0 until fadeSamples) {
                    val factor = 0.5f * (1.0f + cos(Math.PI * i / fadeSamples)).toFloat()
                    scratchBuffer[i] = (lastSample * factor).toInt().toShort()
                }
                sink.write(scratchBuffer, 0, fadeSamples)
            }
        } catch (_: Exception) {
            // Non-fatal fallback
        }
    }

    companion object {
        fun defaultAudioDispatcher(): CoroutineDispatcher {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "ShoutSend-AudioTx").apply {
                    priority = Thread.MAX_PRIORITY
                }
            }
            return executor.asCoroutineDispatcher()
        }
    }
}
