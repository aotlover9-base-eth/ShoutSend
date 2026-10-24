package app.shoutsend.core.audio.receiver

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AcousticReceiverTest {

    private val audioSpec = AudioSpec.RATE_48K

    @Test
    fun testMessageCompleteDeliversExpectedPayload() = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.Default) {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val text = "ShoutSend Acoustic AudioRecord Pipeline"
        val payloadBytes = PayloadCodec.encode(Payload.Text(text))

        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = payloadBytes,
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = 50,
            tailSilenceMs = 300
        )
        val pcmShorts = Modulator(plan).modulateToPcm16()

        val fakeSource = FakePcmSource(pcmShorts, actualSampleRate = 48000)
        val receiver = DefaultAcousticReceiver(
            pcmSourceFactory = { fakeSource }
        )

        val eventsList = java.util.concurrent.CopyOnWriteArrayList<ReceiveEvent>()
        val collectJob = launch {
            receiver.events.collect {
                eventsList.add(it)
            }
        }

        receiver.start(Band.RETRO)

        // Wait up to 5 seconds of real time for background pipeline to decode
        var waited = 0
        while (waited < 50 && eventsList.none { it is ReceiveEvent.MessageComplete }) {
            kotlinx.coroutines.delay(100)
            waited++
        }

        val completeEvent = eventsList.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        assertNotNull("MessageComplete must be emitted within 5s, got: $eventsList", completeEvent)
        assertEquals(MessageType.TEXT, completeEvent!!.message.type)
        val decodedText = (completeEvent.payload as? Payload.Text)?.value
        assertEquals(text, decodedText)

        receiver.stop()
        collectJob.cancel()
        assertTrue("Source released on stop", fakeSource.isReleased)
    }

    @Test
    fun testMicrophoneSilenceDetectionTriggersMicrophoneSilent() = runTest {
        // 2.5 seconds of absolute digital zero silence at 48 kHz
        val silenceShorts = ShortArray(48000 * 5 / 2)
        val fakeSource = FakePcmSource(silenceShorts, actualSampleRate = 48000)

        val receiver = DefaultAcousticReceiver(
            pcmSourceFactory = { fakeSource },
            scope = backgroundScope
        )

        receiver.start(Band.RETRO)

        // Wait for silence diagnostics
        val diag = receiver.diagnostics.first { it.isSilent }
        assertTrue("Diagnostics flags silence", diag.isSilent)

        val lostEvent = receiver.events.filterIsInstance<ReceiveEvent.Lost>().first()
        assertEquals("MicrophoneSilent", lostEvent.reason)

        receiver.stop()
    }

    @Test
    fun testClippingSignalTriggersInputTooLoud() = runTest {
        // 1 second of maximum amplitude clipping signal
        val clippingShorts = ShortArray(48000) { 32767.toShort() }
        val fakeSource = FakePcmSource(clippingShorts, actualSampleRate = 48000)

        val receiver = DefaultAcousticReceiver(
            pcmSourceFactory = { fakeSource },
            scope = backgroundScope
        )

        receiver.start(Band.RETRO)

        val diag = receiver.diagnostics.first { it.isClipping }
        assertTrue("Diagnostics flags clipping", diag.isClipping)

        val failEvent = receiver.events.filterIsInstance<ReceiveEvent.FrameFailed>().first()
        assertEquals("InputTooLoud", failEvent.reason)

        receiver.stop()
    }

    @Test
    fun testStopReleasesSourceAndIsIdempotent() = runTest {
        val fakeSource = FakePcmSource(ShortArray(1000), actualSampleRate = 48000)
        val receiver = DefaultAcousticReceiver(
            pcmSourceFactory = { fakeSource },
            scope = backgroundScope
        )

        receiver.start(Band.RETRO)
        assertTrue("Receiver is running", receiver.isRunning)
        assertTrue("Source started", fakeSource.isStarted)

        receiver.stop()
        assertTrue("Receiver stopped", !receiver.isRunning)
        assertTrue("Source released", fakeSource.isReleased)

        // Second stop is safe and idempotent
        receiver.stop()
        assertTrue("Still stopped", !receiver.isRunning)
    }
}
