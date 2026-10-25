package app.shoutsend.modem.tuning

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class ModemTuningTest {

    private val audioSpec = AudioSpec.RATE_48K

    private fun sha256(data: FloatArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val byteBuf = java.nio.ByteBuffer.allocate(data.size * 4)
        for (f in data) {
            byteBuf.putFloat(f)
        }
        val hash = digest.digest(byteBuf.array())
        return hash.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testDefaultTuningIsValid() {
        val tuning = ModemTuning.DEFAULT
        val result = tuning.validate()
        assertTrue("Default tuning must be Valid", result is ModemTuningValidationResult.Valid)
    }

    @Test
    fun testValidationRejectsInvalidValues() {
        val badTuning = ModemTuning(
            outputLevel = 1.5f,
            leadInSilenceMs = 10,
            normalDurationMs = 5,
            detectionMarginDb = 0.5,
            fftSize = 1000 // Not a power of 2
        )
        val result = badTuning.validate()
        assertTrue("Bad tuning must fail validation", result is ModemTuningValidationResult.Invalid)
        val errors = (result as ModemTuningValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("outputLevel") })
        assertTrue(errors.any { it.contains("leadInSilenceMs") })
        assertTrue(errors.any { it.contains("normalDurationMs") })
        assertTrue(errors.any { it.contains("detectionMarginDb") })
        assertTrue(errors.any { it.contains("fftSize") })
    }

    @Test
    fun testDefaultTuningProducesBitIdenticalRenderedTransmission() {
        val payload = PayloadCodec.encode(Payload.Text("Deterministic Audio Waveform Hash Check"))
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 2, rsProfileId = 1)
        val message = OutgoingMessage(MessageType.TEXT, MessageFlags.NONE, payload, messageId = 101)

        // Render 1: Using default parameters
        val plan1 = TransmissionBuilder.build(
            message = message,
            profile = profile,
            audioSpec = audioSpec
        )
        val pcm1 = Modulator(plan1).modulateToFloatArray()
        val hash1 = sha256(pcm1)

        // Render 2: Explicitly passing ModemTuning.DEFAULT
        val plan2 = TransmissionBuilder.build(
            message = message,
            profile = profile,
            audioSpec = audioSpec,
            tuning = ModemTuning.DEFAULT
        )
        val pcm2 = Modulator(plan2).modulateToFloatArray()
        val hash2 = sha256(pcm2)

        println("[HASH] Default render SHA-256: $hash1")
        assertEquals("Hash of transmission with ModemTuning.DEFAULT must match exactly", hash1, hash2)
        assertArrayEquals("Rendered PCM buffers must be bit-identical", pcm1, pcm2, 0.0f)
    }

    @Test
    fun testTuningOverridesSymbolDurationAndLeadInSilence() {
        val payload = PayloadCodec.encode(Payload.Text("Tuning Override Test"))
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.FAST, repeats = 1, rsProfileId = 0)
        val message = OutgoingMessage(MessageType.TEXT, MessageFlags.NONE, payload, messageId = 202)

        val customTuning = ModemTuning(
            leadInSilenceMs = 500,
            fastDurationMs = 25,
            fastRampMs = 4,
            outputLevel = 0.50f
        )

        val planCustom = TransmissionBuilder.build(
            message = message,
            profile = profile,
            audioSpec = audioSpec,
            tuning = customTuning
        )

        val planDefault = TransmissionBuilder.build(
            message = message,
            profile = profile,
            audioSpec = audioSpec,
            tuning = ModemTuning.DEFAULT
        )

        // Custom has longer lead-in (500ms vs 250ms) and longer fast symbols (25ms vs 20ms)
        assertTrue(planCustom.durationSeconds > planDefault.durationSeconds)
        assertEquals(0.50f, planCustom.schedule.events.last().levelPerTone, 1e-4f)
    }
}
