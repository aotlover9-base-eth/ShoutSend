package app.shoutsend.core.audio.transmitter

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.ModemProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AcousticTransmitterTest {

    private class FakePcmSink(
        var simulatedUnderrun: Int = 0,
        var shouldFailWrite: Boolean = false
    ) : PcmSink {
        val writtenSamples = mutableListOf<Short>()
        var playCount = 0
        var stopCount = 0
        var releaseCount = 0
        var pauseCount = 0
        var flushCount = 0
        var currentHead: Long = 0L

        override fun write(buffer: ShortArray, offset: Int, count: Int): Int {
            if (shouldFailWrite) return -1
            for (i in offset until offset + count) {
                writtenSamples.add(buffer[i])
            }
            currentHead += count
            return count
        }

        override fun play() { playCount++ }
        override fun pause() { pauseCount++ }
        override fun flush() { flushCount++ }
        override fun stop() { stopCount++ }
        override fun release() { releaseCount++ }
        override fun getPlaybackHeadPosition(): Long = currentHead
        override fun getUnderrunCount(): Int = simulatedUnderrun
    }

    private fun createModulator(): Modulator {
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = "ShoutSend Test Transmission".toByteArray(Charsets.UTF_8),
            profile = ModemProfile.DEFAULT_RETRO,
            audioSpec = AudioSpec.RATE_48K,
            leadInSilenceMs = 50,
            tailSilenceMs = 50
        )
        return Modulator(plan)
    }

    @Test
    fun transmitPlaysAllSamplesToCompletion() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sink = FakePcmSink()
        val transmitter = DefaultAcousticTransmitter(
            sinkFactory = { sink },
            dispatcher = testDispatcher
        )
        val modulator = createModulator()

        val progressList = mutableListOf<TransmitProgress>()
        val result = transmitter.transmit(modulator) { progress ->
            progressList.add(progress)
        }

        assertTrue("Expected Success, got $result", result is TransmitResult.Success)
        val success = result as TransmitResult.Success
        assertEquals(modulator.totalSamples, success.totalFrames)
        assertEquals(modulator.totalSamples.toInt(), sink.writtenSamples.size)
        assertEquals(1, sink.playCount)
        assertEquals(1, sink.stopCount)
        assertEquals(1, sink.releaseCount)

        // Progress check
        assertTrue("Progress should have been emitted", progressList.isNotEmpty())
        assertEquals(1.0f, progressList.last().fraction, 1e-4f)
        assertEquals(modulator.totalSamples, progressList.last().samplesWritten)
    }

    @Test
    fun transmitProgressIsMonotonic() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sink = FakePcmSink()
        val transmitter = DefaultAcousticTransmitter(
            sinkFactory = { sink },
            dispatcher = testDispatcher
        )
        val modulator = createModulator()

        val fractions = mutableListOf<Float>()
        transmitter.transmit(modulator) { progress ->
            fractions.add(progress.fraction)
        }

        for (i in 0 until fractions.size - 1) {
            assertTrue(
                "Progress must be monotonic: ${fractions[i]} <= ${fractions[i + 1]}",
                fractions[i] <= fractions[i + 1]
            )
        }
    }

    @Test
    fun transmitCancellationAppliesFadeOutAndReleasesCleanly() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sink = FakePcmSink()
        val transmitter = DefaultAcousticTransmitter(
            sinkFactory = { sink },
            dispatcher = testDispatcher
        )
        val modulator = createModulator()

        val job = async(testDispatcher) {
            transmitter.transmit(modulator)
        }

        // Advance dispatcher slightly then cancel
        testScheduler.advanceTimeBy(100)
        job.cancelAndJoin()

        assertEquals(1, sink.releaseCount)
        assertEquals(1, sink.stopCount)
    }

    @Test
    fun transmitSinkFailureReleasesSinkAndReturnsFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val failingSink = FakePcmSink(shouldFailWrite = true)
        val transmitter = DefaultAcousticTransmitter(
            sinkFactory = { failingSink },
            dispatcher = testDispatcher
        )
        val modulator = createModulator()

        val result = transmitter.transmit(modulator)

        assertTrue("Expected Failure result, got $result", result is TransmitResult.Failure)
        assertEquals(1, failingSink.releaseCount)
    }

    @Test
    fun outputRouteInfoModelProperties() {
        val info = OutputRouteInfo(
            hasHeadset = true,
            hasBluetooth = false,
            hasUsb = true,
            volumeFraction = 0.75f,
            nativeSampleRateHz = 48000
        )
        assertTrue(info.hasHeadset)
        assertFalse(info.hasBluetooth)
        assertTrue(info.hasUsb)
        assertEquals(0.75f, info.volumeFraction, 1e-4f)
        assertEquals(48000, info.nativeSampleRateHz)
    }
}
