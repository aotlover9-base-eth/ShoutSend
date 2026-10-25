package app.shoutsend.modem.modulator

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransmissionEnvelopeTest {

    private val audioSpec = AudioSpec(48000)

    @Test
    fun computeEnvelopeProducesValidWindowsAndProfiles() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val payload = "Hello acoustic envelope!".toByteArray(Charsets.UTF_8)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            payload = payload,
            profile = profile,
            audioSpec = audioSpec,
            leadInSilenceMs = 200,
            tailSilenceMs = 200
        )

        val envelope = TransmissionEnvelope.compute(plan, windowMs = 20)
        assertNotNull(envelope)
        assertTrue(envelope.totalWindows > 0)
        assertEquals(envelope.totalWindows, envelope.rms.size)
        assertEquals(envelope.totalWindows, envelope.toneIndex.size)

        // 1. Initial 100ms should be lead-in silence (window 0..4)
        val earlyPoint = envelope.getAtTime(0.05)
        assertEquals(0.0f, earlyPoint.rms, 0.01f)
        assertEquals(-1, earlyPoint.toneIndex)

        // 2. Mid-transmission should have active tones and RMS > 0
        val midTime = plan.durationSeconds / 2.0
        val midPoint = envelope.getAtTime(midTime)
        assertTrue("Mid transmission RMS must be positive: ${midPoint.rms}", midPoint.rms > 0.1f)
        assertTrue("Tone index must be valid (0..17): ${midPoint.toneIndex}", midPoint.toneIndex in 0..17)

        // 3. Final windows (tail silence) should return to silence
        val tailPoint = envelope.getAtTime(plan.durationSeconds - 0.05)
        assertEquals(0.0f, tailPoint.rms, 0.05f)
        assertEquals(-1, tailPoint.toneIndex)

        // 4. Sample-based lookup matches time-based lookup
        val sampleIndex = (midTime * audioSpec.sampleRateHz).toLong()
        val samplePoint = envelope.getAtSample(sampleIndex, audioSpec.sampleRateHz)
        assertEquals(midPoint.rms, samplePoint.rms, 0.001f)
        assertEquals(midPoint.toneIndex, samplePoint.toneIndex)

        // 5. Boundary clamping
        val beforeStart = envelope.getAtTime(-1.0)
        assertEquals(0.0f, beforeStart.rms, 0.001f)
        val afterEnd = envelope.getAtTime(plan.durationSeconds + 10.0)
        assertEquals(envelope.rms.last(), afterEnd.rms, 0.001f)
    }

    @Test
    fun computeHandlesEmptyPlanGracefully() {
        val emptyPlan = TransmissionPlan(
            message = OutgoingMessage(MessageType.TEXT, MessageFlags.NONE, byteArrayOf(0)),
            frames = emptyList(),
            codewords = emptyList(),
            schedule = app.shoutsend.modem.synth.SymbolSchedule.EMPTY,
            profile = ModemProfile.DEFAULT_RETRO,
            tonePlan = app.shoutsend.modem.profile.TonePlan(Band.RETRO, audioSpec),
            audioSpec = audioSpec,
            leadInSilenceMs = 0,
            tailSilenceMs = 0,
            totalSamples = 0L
        )

        val envelope = TransmissionEnvelope.compute(emptyPlan, windowMs = 20)
        assertEquals(0, envelope.totalWindows)
        val point = envelope.getAtTime(1.0)
        assertEquals(0.0f, point.rms, 0.0f)
        assertEquals(-1, point.toneIndex)
    }
}
