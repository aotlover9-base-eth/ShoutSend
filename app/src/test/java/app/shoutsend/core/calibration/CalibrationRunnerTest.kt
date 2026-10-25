package app.shoutsend.core.calibration

import android.content.Context
import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.OutputRouteInfo
import app.shoutsend.core.audio.transmitter.TransmitProgress
import app.shoutsend.core.audio.transmitter.TransmitResult
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.calibration.BandVerdict
import app.shoutsend.modem.calibration.TestSequence
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.profile.Band
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalibrationRunnerTest {

    private class FakeAcousticReceiver : AcousticReceiver {
        override val events: SharedFlow<ReceiveEvent> = MutableSharedFlow()
        override val diagnostics: StateFlow<ReceiverDiagnostics> = MutableStateFlow(ReceiverDiagnostics())
        override val spectrumRows: ReceiveChannel<FloatArray> = Channel()
        override var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)? = null
        override var isRunning: Boolean = false
            private set

        var startCount = 0
        var stopCount = 0

        override fun start(band: Band, passphrase: String?) {
            isRunning = true
            startCount++
        }

        override fun stop() {
            isRunning = false
            stopCount++
        }
    }

    private class FakeAcousticTransmitter(
        private val receiverToFeed: FakeAcousticReceiver? = null
    ) : AcousticTransmitter {
        var transmitCount = 0

        override suspend fun transmit(
            source: AudioStreamSource,
            onProgress: (progress: TransmitProgress) -> Unit
        ): TransmitResult {
            transmitCount++

            // Feed test sequence into receiver's raw audio tap if present
            if (receiverToFeed != null && receiverToFeed.rawAudioTap != null) {
                val seq = TestSequence(source.audioSpec)
                val total = seq.totalSamples.toInt()
                val chunk = 960
                val floatBuf = FloatArray(chunk)
                val shortBuf = ShortArray(chunk)

                var cursor = 0
                while (cursor < total) {
                    val toRender = minOf(chunk, total - cursor)
                    seq.render(cursor.toLong(), floatBuf, 0, toRender)
                    for (i in 0 until toRender) {
                        shortBuf[i] = (floatBuf[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    receiverToFeed.rawAudioTap?.invoke(shortBuf, 0, toRender)
                    cursor += toRender

                    val frac = cursor.toFloat() / total.toFloat()
                    onProgress(
                        TransmitProgress(
                            samplesWritten = cursor.toLong(),
                            totalSamples = total.toLong(),
                            fraction = frac,
                            elapsedSeconds = cursor.toDouble() / 48000.0,
                            remainingSeconds = (total - cursor).toDouble() / 48000.0
                        )
                    )
                }
            } else {
                onProgress(
                    TransmitProgress(
                        samplesWritten = source.totalSamples,
                        totalSamples = source.totalSamples,
                        fraction = 1.0f,
                        elapsedSeconds = source.durationSeconds,
                        remainingSeconds = 0.0
                    )
                )
            }

            return TransmitResult.Success(
                totalFrames = source.totalSamples,
                durationMs = (source.durationSeconds * 1000).toLong(),
                underrunCount = 0
            )
        }
    }

    @Test
    fun testSuccessfulCalibrationLoopbackAndProgress() = runBlocking {
        val receiver = FakeAcousticReceiver()
        val transmitter = FakeAcousticTransmitter(receiverToFeed = receiver)

        val progressList = mutableListOf<Float>()
        val runner = CalibrationRunner(
            receiver = receiver,
            transmitter = transmitter,
            audioSpec = AudioSpec.RATE_48K,
            routeInfoProvider = {
                OutputRouteInfo(
                    hasHeadset = false,
                    hasBluetooth = false,
                    hasUsb = false,
                    volumeFraction = 0.8f,
                    nativeSampleRateHz = 48000
                )
            }
        )

        val result = runner.runCalibration { p ->
            progressList.add(p)
        }

        assertNotNull(result)
        assertEquals(1, receiver.startCount)
        assertEquals(1, receiver.stopCount)
        assertEquals(1, transmitter.transmitCount)
        assertNull("Raw audio tap must be detached on completion", receiver.rawAudioTap)

        // Progress monotonicity
        assertTrue("Progress updates recorded", progressList.isNotEmpty())
        assertEquals(1.0f, progressList.last(), 0.01f)
        for (i in 1 until progressList.size) {
            assertTrue("Progress must be non-decreasing", progressList[i] >= progressList[i - 1])
        }

        // Both bands should be GOOD in synthetic clean loopback
        assertEquals(BandVerdict.GOOD, result.retroReport.verdict)
        assertEquals(BandVerdict.GOOD, result.stealthReport.verdict)
        assertEquals(Band.STEALTH, result.recommendedBand)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun testHardwareWarningsReported() = runBlocking {
        val receiver = FakeAcousticReceiver()
        val transmitter = FakeAcousticTransmitter(receiverToFeed = receiver)

        val runner = CalibrationRunner(
            receiver = receiver,
            transmitter = transmitter,
            audioSpec = AudioSpec.RATE_48K,
            routeInfoProvider = {
                OutputRouteInfo(
                    hasHeadset = true,
                    hasBluetooth = false,
                    hasUsb = false,
                    volumeFraction = 0.25f,
                    nativeSampleRateHz = 48000
                )
            }
        )

        val result = runner.runCalibration()
        assertTrue("Headphones warning present", result.warnings.contains("HEADPHONES_CONNECTED"))
        assertTrue("Volume low warning present", result.warnings.contains("VOLUME_LOW"))
    }

    @Test
    fun testCancellationStopsReceiverAndClearsTap() = runBlocking {
        val receiver = FakeAcousticReceiver()
        val transmitter = FakeAcousticTransmitter(receiverToFeed = null)

        val runner = CalibrationRunner(
            receiver = receiver,
            transmitter = transmitter,
            audioSpec = AudioSpec.RATE_48K,
            routeInfoProvider = {
                OutputRouteInfo(false, false, false, 0.8f, 48000)
            }
        )

        val job = async {
            runner.runCalibration()
        }

        // Wait until receiver has started
        while (!receiver.isRunning) {
            kotlinx.coroutines.delay(10)
        }

        // Cancel while running
        job.cancelAndJoin()

        assertEquals(1, receiver.stopCount)
        assertNull("Tap must be cleared on cancellation", receiver.rawAudioTap)
    }
}
