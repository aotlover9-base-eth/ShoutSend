package app.shoutsend.modem.demod

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.crypto.SecureBox
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverPipelineTest {

    private val audioSpec = AudioSpec.RATE_48K

    private fun transmitAndReceive(
        profile: ModemProfile,
        payloadBytes: ByteArray,
        type: MessageType = MessageType.TEXT,
        flags: MessageFlags = MessageFlags.NONE,
        passphrase: String? = null
    ): List<ReceiveEvent> {
        val plan = TransmissionBuilder.build(
            type = type,
            flags = flags,
            payload = payloadBytes,
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = 50,
            tailSilenceMs = 50
        )
        val pcm = Modulator(plan).modulateToFloatArray()

        val events = mutableListOf<ReceiveEvent>()
        val pipeline = ReceiverPipeline(
            band = profile.band,
            audioSpec = audioSpec,
            listener = { event -> events.add(event) },
            passphrase = passphrase
        )
        pipeline.push(pcm)
        return events
    }

    @Test
    fun testTextPayloadRetroMfskNormal() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val text = "ShoutSend Acoustic Data Transfer"
        val payload = Payload.Text(text)
        val payloadBytes = PayloadCodec.encode(payload)

        val events = transmitAndReceive(profile, payloadBytes, MessageType.TEXT)

        val completeEvent = events.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        assertNotNull("MessageComplete should be emitted", completeEvent)
        assertEquals(MessageType.TEXT, completeEvent!!.message.type)

        val decodedPayload = completeEvent.payload as? Payload.Text
        assertNotNull("Decoded payload should be Payload.Text", decodedPayload)
        assertEquals(text, decodedPayload!!.value)
    }

    @Test
    fun testContactPayloadStealthDtfskFast() {
        val profile = ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 1, rsProfileId = 1)
        val contact = Payload.Contact(displayName = "Alice", phones = listOf("+1234567890"), email = "alice@shoutsend.app")
        val payloadBytes = PayloadCodec.encode(contact)

        val events = transmitAndReceive(profile, payloadBytes, MessageType.CONTACT)

        val completeEvent = events.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        assertNotNull(completeEvent)
        val decoded = completeEvent!!.payload as? Payload.Contact
        assertNotNull(decoded)
        assertEquals("Alice", decoded!!.displayName)
        assertEquals("+1234567890", decoded.phones[0])
        assertEquals("alice@shoutsend.app", decoded.email)
    }

    @Test
    fun testUrlPayloadRetroDtfskNormal() {
        val profile = ModemProfile(Band.RETRO, Scheme.DTFSK, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val url = "https://shoutsend.app"
        val payload = Payload.Url(url)
        val payloadBytes = PayloadCodec.encode(payload)

        val events = transmitAndReceive(profile, payloadBytes, MessageType.URL)

        val completeEvent = events.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        assertNotNull(completeEvent)
        val decoded = completeEvent!!.payload as? Payload.Url
        assertNotNull(decoded)
        assertEquals(url, decoded!!.value)
    }

    @Test
    fun testSecureModeWithCorrectAndWrongPassphrase() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val secretText = "Super Secret Acoustic Token"
        val payload = Payload.Text(secretText)
        val plaintextBytes = PayloadCodec.encode(payload)

        // Encrypt with passphrase "hunter22pass" (length >= 8)
        val encryptedBytes = SecureBox.seal(
            plainBytes = plaintextBytes,
            type = MessageType.TEXT,
            passphrase = "hunter22pass".toCharArray()
        )

        // 1. Receive with correct passphrase
        val eventsOk = transmitAndReceive(
            profile = profile,
            payloadBytes = encryptedBytes,
            type = MessageType.TEXT,
            flags = MessageFlags(isEncrypted = true),
            passphrase = "hunter22pass"
        )
        val completeOk = eventsOk.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        assertNotNull("Complete message with correct passphrase", completeOk)
        val decryptedText = (completeOk!!.payload as? Payload.Text)?.value
        assertEquals(secretText, decryptedText)

        // 2. Receive with wrong passphrase
        val eventsWrong = transmitAndReceive(
            profile = profile,
            payloadBytes = encryptedBytes,
            type = MessageType.TEXT,
            flags = MessageFlags(isEncrypted = true),
            passphrase = "wrong_password"
        )
        val completeWrong = eventsWrong.filterIsInstance<ReceiveEvent.MessageComplete>().firstOrNull()
        assertNotNull("Message is delivered even if payload decryption fails", completeWrong)
        assertNull("Decoded payload must be null on wrong passphrase", completeWrong!!.payload)
    }

    @Test
    fun testTwoBackToBackMessagesBothDelivered() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)

        val msg1 = "First transmission"
        val plan1 = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = PayloadCodec.encode(Payload.Text(msg1)),
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = 50,
            tailSilenceMs = 350 // Allow 300 ms cooldown to complete
        )
        val pcm1 = Modulator(plan1).modulateToFloatArray()

        val msg2 = "Second transmission"
        val plan2 = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = PayloadCodec.encode(Payload.Text(msg2)),
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = 50,
            tailSilenceMs = 50
        )
        val pcm2 = Modulator(plan2).modulateToFloatArray()

        val combinedPcm = FloatArray(pcm1.size + pcm2.size)
        System.arraycopy(pcm1, 0, combinedPcm, 0, pcm1.size)
        System.arraycopy(pcm2, 0, combinedPcm, pcm1.size, pcm2.size)

        val events = mutableListOf<ReceiveEvent>()
        val pipeline = ReceiverPipeline(profile.band, audioSpec, { events.add(it) })
        pipeline.push(combinedPcm)

        val completes = events.filterIsInstance<ReceiveEvent.MessageComplete>()
        assertEquals("Both back-to-back transmissions should complete", 2, completes.size)
        assertEquals(msg1, (completes[0].payload as Payload.Text).value)
        assertEquals(msg2, (completes[1].payload as Payload.Text).value)
    }

    @Test
    fun testCutoffHalfwayEmitsLostAndRecoversNextTransmission() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = PayloadCodec.encode(Payload.Text("Cutoff text")),
            profile = profile,
            audioSpec = audioSpec
        )
        val fullPcm = Modulator(plan).modulateToFloatArray()

        // Cut off audio midway through first frame
        val cutPcm = fullPcm.copyOf((fullPcm.size * 0.4).toInt())
        val events = mutableListOf<ReceiveEvent>()
        val pipeline = ReceiverPipeline(profile.band, audioSpec, { events.add(it) })
        pipeline.push(cutPcm)

        // Then push 500 ms silence
        pipeline.push(FloatArray(audioSpec.sampleRateHz / 2))

        val completes = events.filterIsInstance<ReceiveEvent.MessageComplete>()
        assertTrue("Incomplete message should not complete", completes.isEmpty())

        // Now push a full message to verify pipeline recovery
        val secondEvents = mutableListOf<ReceiveEvent>()
        val recoverPipeline = ReceiverPipeline(profile.band, audioSpec, { secondEvents.add(it) })
        recoverPipeline.push(fullPcm)
        assertTrue(secondEvents.any { it is ReceiveEvent.MessageComplete })
    }
}
