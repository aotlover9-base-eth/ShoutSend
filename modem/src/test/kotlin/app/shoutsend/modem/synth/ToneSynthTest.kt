package app.shoutsend.modem.synth

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.testutil.PcmMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

class ToneSynthTest {

    private val audioSpec48k = AudioSpec(48000)
    private val stealthPlan = TonePlan(Band.STEALTH, audioSpec48k)
    private val retroPlan = TonePlan(Band.RETRO, audioSpec48k)

    @Test
    fun testChunkIndependenceBitIdenticalAcrossChunkSizes() {
        val symLength = 480
        val rampLength = 96
        val events = mutableListOf<SymbolEvent>()

        var currentSample = 0L
        val toneSequence = intArrayOf(0, 5, 5, 12, 15, 16, 17, 3)
        for (tone in toneSequence) {
            events.add(
                SymbolEvent(
                    startSample = currentSample,
                    lengthSamples = symLength,
                    rampSamples = rampLength,
                    toneIndexes = intArrayOf(tone),
                    levelPerTone = 0.8f
                )
            )
            currentSample += (symLength - rampLength)
        }
        val schedule = SymbolSchedule(events)
        val totalLength = schedule.totalSamples.toInt()

        // 1. Render all at once
        val fullRender = FloatArray(totalLength)
        ToneSynth.render(schedule, stealthPlan, 0L, fullRender)

        // 2. Render in chunks of 1 sample
        val chunk1Render = FloatArray(totalLength)
        for (s in 0 until totalLength) {
            ToneSynth.render(schedule, stealthPlan, s.toLong(), chunk1Render, s, 1)
        }
        for (i in 0 until totalLength) {
            assertEquals(
                "Mismatch at sample $i for chunk size 1",
                fullRender[i].toRawBits(),
                chunk1Render[i].toRawBits()
            )
        }

        // 3. Render in chunks of 7 samples
        val chunk7Render = FloatArray(totalLength)
        var offset = 0
        while (offset < totalLength) {
            val count = minOf(7, totalLength - offset)
            ToneSynth.render(schedule, stealthPlan, offset.toLong(), chunk7Render, offset, count)
            offset += count
        }
        for (i in 0 until totalLength) {
            assertEquals(
                "Mismatch at sample $i for chunk size 7",
                fullRender[i].toRawBits(),
                chunk7Render[i].toRawBits()
            )
        }

        // 4. Render in chunks of 480 samples
        val chunk480Render = FloatArray(totalLength)
        offset = 0
        while (offset < totalLength) {
            val count = minOf(480, totalLength - offset)
            ToneSynth.render(schedule, stealthPlan, offset.toLong(), chunk480Render, offset, count)
            offset += count
        }
        for (i in 0 until totalLength) {
            assertEquals(
                "Mismatch at sample $i for chunk size 480",
                fullRender[i].toRawBits(),
                chunk480Render[i].toRawBits()
            )
        }

        // 5. Render in chunk size of 10,000 samples (larger than total schedule length)
        val chunk10kRender = FloatArray(10000)
        ToneSynth.render(schedule, stealthPlan, 0L, chunk10kRender, 0, 10000)
        for (i in 0 until totalLength) {
            assertEquals(
                "Mismatch at sample $i for chunk size 10000",
                fullRender[i].toRawBits(),
                chunk10kRender[i].toRawBits()
            )
        }
        // Verify past totalLength is 0.0f
        for (i in totalLength until 10000) {
            assertEquals(0.0f, chunk10kRender[i], 0.0f)
        }
    }

    @Test
    fun testSpectralPuritySingleToneConcentratesEnergy() {
        // Schedule a single tone of 3200 samples (multiple of 320 samples for exact stealth bin orthogonality)
        val length = 3200
        val targetTone = 5
        val targetFreq = stealthPlan.frequency(targetTone)
        val schedule = SymbolSchedule(
            listOf(
                SymbolEvent(
                    startSample = 0L,
                    lengthSamples = length,
                    rampSamples = 0, // Unwindowed steady region for exact DFT bin orthogonal check
                    toneIndexes = intArrayOf(targetTone),
                    levelPerTone = 0.8f
                )
            )
        )
        val buffer = FloatArray(length)
        ToneSynth.render(schedule, stealthPlan, 0L, buffer)

        val targetPower = computeDftPower(buffer, targetFreq, 48000)
        assertTrue("Target tone power must be positive", targetPower > 0.01)

        // Verify all other 15 data tones and 2 sync tones are at least 40 dB lower
        for (toneIdx in 0..17) {
            if (toneIdx == targetTone) continue
            val otherFreq = stealthPlan.frequency(toneIdx)
            val otherPower = computeDftPower(buffer, otherFreq, 48000)
            val diffDb = 10.0 * log10(otherPower / targetPower)
            assertTrue(
                "Tone $toneIdx ($otherFreq Hz) suppression should be >= 40 dB, got $diffDb dB",
                diffDb <= -40.0
            )
        }
    }

    @Test
    fun testRepeatedToneShowsNoAmplitudeDipAtBoundary() {
        val symLength = 480
        val rampLength = 96
        val tone = 4
        // Two consecutive symbols with the exact same tone overlapping by rampLength
        val schedule = SymbolSchedule(
            listOf(
                SymbolEvent(0L, symLength, rampLength, intArrayOf(tone), 0.8f),
                SymbolEvent(
                    (symLength - rampLength).toLong(),
                    symLength,
                    rampLength,
                    intArrayOf(tone),
                    0.8f
                )
            )
        )
        val totalSamples = schedule.totalSamples.toInt()
        val buffer = FloatArray(totalSamples)
        ToneSynth.render(schedule, stealthPlan, 0L, buffer)

        // Boundary overlap region: [symLength - rampLength, symLength) = [384, 480)
        val boundarySamples = buffer.sliceArray((symLength - rampLength) until symLength)
        val boundaryRms = PcmMath.calculateRms(boundarySamples)

        // Steady region: [rampLength, symLength - rampLength) = [96, 384)
        val steadySamples = buffer.sliceArray(rampLength until (symLength - rampLength))
        val steadyRms = PcmMath.calculateRms(steadySamples)

        val diffDb = abs(20.0 * log10(boundaryRms.toDouble() / steadyRms.toDouble()))
        assertTrue(
            "Boundary RMS ($boundaryRms) and Steady RMS ($steadyRms) must be within 1 dB, diff was $diffDb dB",
            diffDb < 1.0
        )
        // Due to raised cosine exact complement, diff is practically 0 (< 0.05 dB)
        assertTrue("Ramp complement is mathematically exact", diffDb < 0.05)
    }

    @Test
    fun testClickTestBoundedSlopeAcrossSymbolBoundary() {
        val symLength = 480
        val rampLength = 96
        // Transition between two widely separated tones
        val toneA = 0 // 17600 Hz
        val toneB = 15 // 19850 Hz
        val schedule = SymbolSchedule(
            listOf(
                SymbolEvent(0L, symLength, rampLength, intArrayOf(toneA), 0.8f),
                SymbolEvent(
                    (symLength - rampLength).toLong(),
                    symLength,
                    rampLength,
                    intArrayOf(toneB),
                    0.8f
                )
            )
        )
        val totalSamples = schedule.totalSamples.toInt()
        val buffer = FloatArray(totalSamples)
        ToneSynth.render(schedule, stealthPlan, 0L, buffer)

        // Find maximum sample-to-sample difference across the entire transition region
        var maxSlope = 0.0f
        for (i in 1 until totalSamples) {
            val slope = abs(buffer[i] - buffer[i - 1])
            if (slope > maxSlope) {
                maxSlope = slope
            }
        }

        // Maximum continuous derivative of 0.8 * sin(2*pi*20000*t) at Fs=48000 is ~2.09
        // Smooth raised-cosine ramp ensures no click / spike above smooth tone slope
        assertTrue(
            "Max sample-to-sample slope ($maxSlope) must be bounded (< 2.2f)",
            maxSlope < 2.2f
        )
    }

    @Test
    fun testDtfskAmplitudeSumAndEqualEnergy() {
        val symLength = 480
        val rampLength = 96
        val toneA = 2 // Lower group
        val toneB = 10 // Upper group
        val outputLevel = 0.8f
        val levelPerTone = outputLevel / 2.0f // 0.4f

        val schedule = SymbolSchedule(
            listOf(
                SymbolEvent(
                    startSample = 0L,
                    lengthSamples = symLength,
                    rampSamples = rampLength,
                    toneIndexes = intArrayOf(toneA, toneB),
                    levelPerTone = levelPerTone
                )
            )
        )
        val buffer = FloatArray(symLength)
        ToneSynth.render(schedule, stealthPlan, 0L, buffer)

        // 1. Peak never exceeds outputLevel + tolerance (0.801f)
        var peak = 0.0f
        for (s in buffer) {
            val mag = abs(s)
            if (mag > peak) peak = mag
        }
        assertTrue("DTFSK peak ($peak) must not exceed 0.801f", peak <= 0.801f)

        // 2. Both tones present with equal energy
        val powerA = computeDftPower(buffer, stealthPlan.frequency(toneA), 48000)
        val powerB = computeDftPower(buffer, stealthPlan.frequency(toneB), 48000)
        val ratioDb = abs(10.0 * log10(powerA / powerB))
        assertTrue(
            "DTFSK tone energies must be balanced within 0.5 dB, got $ratioDb dB",
            ratioDb < 0.5
        )
    }

    @Test
    fun testEdgeCasesAndValidation() {
        // 1. Ramp longer than half symbol length must fail
        assertThrows(IllegalArgumentException::class.java) {
            SymbolEvent(
                startSample = 0L,
                lengthSamples = 100,
                rampSamples = 51,
                toneIndexes = intArrayOf(1),
                levelPerTone = 0.8f
            )
        }

        // 2. Single symbol schedule renders cleanly
        val single = SymbolSchedule(
            listOf(SymbolEvent(0L, 200, 20, intArrayOf(2), 0.8f))
        )
        val outSingle = FloatArray(200)
        ToneSynth.render(single, stealthPlan, 0L, outSingle)
        assertTrue(PcmMath.calculateRms(outSingle) > 0.1f)

        // 3. Start index in the middle of a symbol
        val midBuffer = FloatArray(50)
        ToneSynth.render(single, stealthPlan, 50L, midBuffer)
        for (i in 0 until 50) {
            assertEquals(
                "Middle sample $i mismatch",
                outSingle[50 + i].toRawBits(),
                midBuffer[i].toRawBits()
            )
        }

        // 4. Rendering past end fills zeros
        val pastEndBuffer = FloatArray(100) { 1.0f } // Pre-fill with non-zero
        ToneSynth.render(single, stealthPlan, 500L, pastEndBuffer)
        for (i in pastEndBuffer.indices) {
            assertEquals(0.0f, pastEndBuffer[i], 0.0f)
        }
    }

    @Test
    fun testPcmConverterAccuracyAndClipping() {
        // Zero, positive full scale, negative full scale
        assertEquals(0.toShort(), PcmConverter.floatToShort(0.0f))
        assertEquals(32767.toShort(), PcmConverter.floatToShort(1.0f))
        assertEquals((-32768).toShort(), PcmConverter.floatToShort(-1.0f))

        // Clipping beyond [-1.0, 1.0]
        assertEquals(32767.toShort(), PcmConverter.floatToShort(2.5f))
        assertEquals((-32768).toShort(), PcmConverter.floatToShort(-3.0f))

        // Short to float conversion
        assertEquals(0.0f, PcmConverter.shortToFloat(0), 1e-6f)
        assertEquals(1.0f, PcmConverter.shortToFloat(32767), 1e-6f)
        assertEquals(-1.0f, PcmConverter.shortToFloat((-32768).toShort()), 1e-6f)

        // Array and Byte conversions
        val floats = floatArrayOf(-1.0f, -0.5f, 0.0f, 0.5f, 1.0f)
        val shorts = PcmConverter.toShortArray(floats)
        val bytes = PcmConverter.toByteArray(shorts)
        val reconstructedShorts = PcmConverter.fromByteArray(bytes)
        val reconstructedFloats = PcmConverter.toFloatArray(reconstructedShorts)

        for (i in floats.indices) {
            assertEquals(shorts[i], reconstructedShorts[i])
            PcmMath.assertApproxEquals(floats[i], reconstructedFloats[i], 1e-4f)
        }
    }

    private fun computeDftPower(samples: FloatArray, targetFreqHz: Double, sampleRateHz: Int): Double {
        val omega = (2.0 * PI * targetFreqHz) / sampleRateHz
        var sumReal = 0.0
        var sumImag = 0.0
        for (n in samples.indices) {
            val phase = omega * n
            val s = samples[n].toDouble()
            sumReal += s * cos(phase)
            sumImag -= s * sin(phase)
        }
        val n = samples.size.toDouble()
        return (sumReal * sumReal + sumImag * sumImag) / (n * n)
    }
}
