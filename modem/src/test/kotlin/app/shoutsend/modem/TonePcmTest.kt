package app.shoutsend.modem

import app.shoutsend.modem.testutil.PcmBufferBuilder
import app.shoutsend.modem.testutil.PcmMath
import app.shoutsend.modem.testutil.SeededRandom
import app.shoutsend.modem.testutil.TonePcm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class TonePcmTest {

    private val spec = AudioSpec.RATE_48K

    @Test
    fun generatesCorrectLengthAndPeakAmplitude() {
        val duration = 0.05 // 50 ms
        val expectedSamples = (duration * spec.sampleRateHz).toInt()
        val amplitude = 0.8f
        val tone = TonePcm.generateSine(
            frequencyHz = 18500.0,
            durationSeconds = duration,
            sampleRateHz = spec.sampleRateHz,
            amplitude = amplitude
        )

        assertEquals(expectedSamples, tone.size)

        var maxPeak = 0f
        for (s in tone) {
            maxPeak = maxOf(maxPeak, abs(s))
            assertTrue("Sample out of bounds: $s", abs(s) <= amplitude + 1e-4f)
        }
        PcmMath.assertApproxEquals(amplitude, maxPeak, tolerance = 0.05f, message = "Tone peak amplitude")
    }

    @Test
    fun sineWaveRmsMatchesTheoreticalValue() {
        // For pure continuous sine wave, RMS = amplitude / sqrt(2)
        val amplitude = 1.0f
        val expectedRms = (amplitude / sqrt(2.0)).toFloat() // ~0.7071

        val tone = TonePcm.generateSine(
            frequencyHz = 2000.0,
            durationSeconds = 1.0, // 1 sec gives many whole cycles
            sampleRateHz = spec.sampleRateHz,
            amplitude = amplitude
        )

        val measuredRms = PcmMath.calculateRms(tone)
        PcmMath.assertApproxEquals(expectedRms, measuredRms, tolerance = 1e-3f, message = "Sine RMS")

        // In dBFS, 0.7071 corresponds to ~ -3.01 dBFS
        val dbfs = PcmMath.toDbfs(measuredRms)
        PcmMath.assertApproxEquals(-3.01f, dbfs, tolerance = 0.05f, message = "Sine dBFS")
    }

    @Test
    fun silenceHasZeroRms() {
        val silence = TonePcm.generateSilence(0.1, spec.sampleRateHz)
        assertEquals(4800, silence.size)
        val rms = PcmMath.calculateRms(silence)
        assertEquals(0f, rms, 0.0f)
    }

    @Test
    fun pcmBufferBuilderConcatenatesAccurately() {
        val builder = PcmBufferBuilder(spec)
            .addSilence(0.01) // 480 samples
            .addTone(frequencyHz = 1000.0, durationSeconds = 0.02, amplitude = 0.5f) // 960 samples
            .addSilenceSamples(200)

        val buffer = builder.build()
        val expectedTotal = 480 + 960 + 200
        assertEquals(expectedTotal, buffer.size)
        assertEquals(expectedTotal, builder.sampleCount())

        // First 480 should be zero
        for (i in 0 until 480) {
            assertEquals(0f, buffer[i], 0.0f)
        }
        // Middle should have non-zero energy
        val toneSegment = buffer.sliceArray(480 until 480 + 960)
        val middleRms = PcmMath.calculateRms(toneSegment)
        val expectedToneRms = (0.5f / sqrt(2.0)).toFloat()
        PcmMath.assertApproxEquals(expectedToneRms, middleRms, tolerance = 5e-3f)

        // Last 200 should be zero
        for (i in (480 + 960) until expectedTotal) {
            assertEquals(0f, buffer[i], 0.0f)
        }
    }

    @Test
    fun seededRandomIsReproducible() {
        val rng1 = SeededRandom(12345L)
        val rng2 = SeededRandom(12345L)
        val rng3 = SeededRandom(99999L)

        val bytes1 = rng1.nextBytes(32)
        val bytes2 = rng2.nextBytes(32)
        val bytes3 = rng3.nextBytes(32)

        assertArrayEquals("Same seed must produce identical bytes", bytes1, bytes2)
        assertFalse("Different seeds should produce different bytes", bytes1.contentEquals(bytes3))
    }
}
