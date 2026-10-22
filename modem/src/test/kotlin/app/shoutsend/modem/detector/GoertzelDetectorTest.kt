package app.shoutsend.modem.detector

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.synth.PcmConverter
import app.shoutsend.modem.synth.SymbolEvent
import app.shoutsend.modem.synth.SymbolSchedule
import app.shoutsend.modem.synth.ToneSynth
import app.shoutsend.modem.testutil.TonePcm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

class GoertzelDetectorTest {

    private val audioSpec48k = AudioSpec(48000)
    private val audioSpec44k = AudioSpec(44100)

    @Test
    fun testPowerNormalizationWindowedAndUnwindowed() {
        val sampleRate = 48000
        val freq = 2000.0
        val blockSize = 1440 // 30ms
        val amplitude = 0.8f
        val expectedPower = amplitude * amplitude // 0.64f

        val testTone = TonePcm.generateSineSamples(
            frequencyHz = freq,
            sampleCount = blockSize,
            sampleRateHz = sampleRate,
            amplitude = amplitude
        )

        // 1. Unwindowed bank
        val unwindowedBank = GoertzelBank(
            frequencies = doubleArrayOf(freq),
            audioSpec = audioSpec48k,
            blockSize = blockSize,
            windowed = false
        )
        val outUnwindowed = FloatArray(1)
        unwindowedBank.energies(testTone, 0, outUnwindowed)
        val errUnwindowed = abs(outUnwindowed[0] - expectedPower) / expectedPower
        assertTrue(
            "Unwindowed power (${outUnwindowed[0]}) must match expected ($expectedPower) within 1%, error=$errUnwindowed",
            errUnwindowed <= 0.01f
        )

        // 2. Windowed bank (Hann)
        val windowedBank = GoertzelBank(
            frequencies = doubleArrayOf(freq),
            audioSpec = audioSpec48k,
            blockSize = blockSize,
            windowed = true
        )
        val outWindowed = FloatArray(1)
        windowedBank.energies(testTone, 0, outWindowed)
        val errWindowed = abs(outWindowed[0] - expectedPower) / expectedPower
        assertTrue(
            "Windowed power (${outWindowed[0]}) must match expected ($expectedPower) within 1%, error=$errWindowed",
            errWindowed <= 0.01f
        )

        // 3. ShortArray input
        val shorts = PcmConverter.toShortArray(testTone)
        val outShorts = FloatArray(1)
        windowedBank.energies(shorts, 0, outShorts)
        val errShorts = abs(outShorts[0] - expectedPower) / expectedPower
        assertTrue(
            "ShortArray power (${outShorts[0]}) must match expected within 1%, error=$errShorts",
            errShorts <= 0.01f
        )
    }

    @Test
    fun testCrosstalkMatrixAcrossBandsRatesAndSpeeds() {
        val bands = listOf(Band.STEALTH, Band.RETRO)
        val specs = listOf(audioSpec48k, audioSpec44k)
        val speeds = listOf(Speed.ROBUST, Speed.NORMAL, Speed.FAST)

        for (band in bands) {
            for (spec in specs) {
                val tonePlan = TonePlan(band, spec)
                val all16Tones = tonePlan.dataTones.toDoubleArray()

                for (speed in speeds) {
                    val blockSize = Timing.symbolSamples(speed, spec)
                    val rampSize = Timing.rampSamples(speed, spec)

                    val bank = GoertzelBank(
                        frequencies = all16Tones,
                        audioSpec = spec,
                        blockSize = blockSize,
                        windowed = true
                    )

                    var worstLeakageDb = -100.0
                    var worstPair = Pair(-1, -1)

                    // Synthesize each tone alone and measure leakage across all 16 filters
                    val buffer = FloatArray(blockSize + rampSize)
                    val energies = FloatArray(16)

                    for (targetTone in 0..15) {
                        val schedule = SymbolSchedule(
                            listOf(
                                SymbolEvent(
                                    startSample = 0L,
                                    lengthSamples = blockSize + rampSize,
                                    rampSamples = rampSize,
                                    toneIndexes = intArrayOf(targetTone),
                                    levelPerTone = 0.8f
                                )
                            )
                        )
                        ToneSynth.render(schedule, tonePlan, 0L, buffer, 0, blockSize)
                        bank.energies(buffer, 0, energies)

                        val targetEnergy = energies[targetTone]
                        assertTrue("Target tone $targetTone energy must be positive", targetEnergy > 0.01f)

                        for (otherTone in 0..15) {
                            if (otherTone == targetTone) continue
                            val leakEnergy = energies[otherTone]
                            val leakDb = 10.0 * log10((leakEnergy + 1e-12f) / targetEnergy)
                            if (leakDb > worstLeakageDb) {
                                worstLeakageDb = leakDb
                                worstPair = Pair(targetTone, otherTone)
                            }
                        }
                    }

                    val requiredBoundDb = if (speed == Speed.FAST) -20.0 else -25.0
                    println(
                        "Crosstalk Matrix [$band, ${spec.sampleRateHz}Hz, $speed]: " +
                                "Worst off-diagonal leakage = ${String.format("%.2f", worstLeakageDb)} dB " +
                                "(tones ${worstPair.first}->${worstPair.second}), required <= $requiredBoundDb dB"
                    )

                    assertTrue(
                        "Worst leakage ($worstLeakageDb dB) for $band, ${spec.sampleRateHz}Hz, $speed must be <= $requiredBoundDb dB",
                        worstLeakageDb <= requiredBoundDb
                    )
                }
            }
        }
    }

    @Test
    fun testAmplitudeLinearityAcross60dB() {
        val freq = 18500.0
        val blockSize = 1440
        val bank = GoertzelBank(doubleArrayOf(freq), audioSpec48k, blockSize, windowed = true)
        val energies = FloatArray(1)

        val dbfsLevels = doubleArrayOf(-6.0, -20.0, -40.0, -60.0)
        val expectedPowers = DoubleArray(dbfsLevels.size) { i ->
            val amp = 10.0.pow(dbfsLevels[i] / 20.0)
            amp * amp
        }

        for (i in dbfsLevels.indices) {
            val dbfs = dbfsLevels[i]
            val expectedPower = expectedPowers[i]
            val amp = 10.0.pow(dbfs / 20.0).toFloat()

            val tone = TonePcm.generateSineSamples(freq, blockSize, 48000, amp)
            bank.energies(tone, 0, energies)

            val measuredPower = energies[0].toDouble()
            val ratio = measuredPower / expectedPower
            assertTrue(
                "Linearity failure at $dbfs dBFS: measured=$measuredPower, expected=$expectedPower, ratio=$ratio",
                abs(ratio - 1.0) < 0.02
            )
        }
    }

    @Test
    fun testFrequencyTolerance30HzOffset() {
        val bands = listOf(Band.STEALTH, Band.RETRO)
        // 10ms analysis window (480 samples at 48 kHz) used for front-end hopping and transient detection
        val blockSize = Timing.symbolSamples(10, audioSpec48k) // 480 samples

        for (band in bands) {
            val tonePlan = TonePlan(band, audioSpec48k)
            val nominalFreq = tonePlan.dataTones[5]
            val bank = GoertzelBank(doubleArrayOf(nominalFreq), audioSpec48k, blockSize, windowed = true)
            val energies = FloatArray(1)

            // Nominal power
            val nominalTone = TonePcm.generateSineSamples(nominalFreq, blockSize, 48000, 0.8f)
            bank.energies(nominalTone, 0, energies)
            val nominalPower = energies[0]

            // +30 Hz offset
            val plus30Tone = TonePcm.generateSineSamples(nominalFreq + 30.0, blockSize, 48000, 0.8f)
            bank.energies(plus30Tone, 0, energies)
            val plus30Power = energies[0]
            val plus30Ratio = plus30Power / nominalPower

            // -30 Hz offset
            val minus30Tone = TonePcm.generateSineSamples(nominalFreq - 30.0, blockSize, 48000, 0.8f)
            bank.energies(minus30Tone, 0, energies)
            val minus30Power = energies[0]
            val minus30Ratio = minus30Power / nominalPower

            println(
                "Frequency Tolerance [$band @ $nominalFreq Hz, $blockSize samples (10ms)]: " +
                        "+30Hz power ratio = ${String.format("%.3f", plus30Ratio)} (${String.format("%.2f", 10.0 * log10(plus30Ratio.toDouble()))} dB), " +
                        "-30Hz power ratio = ${String.format("%.3f", minus30Ratio)} (${String.format("%.2f", 10.0 * log10(minus30Ratio.toDouble()))} dB)"
            )

            assertTrue(
                "Power at +30Hz ($plus30Ratio) must retain at least 80% (0.80) of nominal power",
                plus30Ratio >= 0.80f
            )
            assertTrue(
                "Power at -30Hz ($minus30Ratio) must retain at least 80% (0.80) of nominal power",
                minus30Ratio >= 0.80f
            )
        }
    }

    @Test
    fun testBlockDetectorStreamingRingBufferAndChunkIndependence() {
        val freq = 2500.0
        val blockSize = 960 // 20ms
        val hopSize = 240 // 5ms (quarter block)
        val bank = GoertzelBank(doubleArrayOf(freq), audioSpec48k, blockSize, windowed = true)

        val totalSamples = 4800 // 100ms
        val fullSignal = TonePcm.generateSineSamples(freq, totalSamples, 48000, 0.8f)

        // 1. Monolithic evaluation
        val expectedBlocks = mutableListOf<Pair<Long, Float>>()
        var cursor = 0
        while (cursor + blockSize <= totalSamples) {
            val out = FloatArray(1)
            bank.energies(fullSignal, cursor, out)
            expectedBlocks.add(Pair(cursor.toLong(), out[0]))
            cursor += hopSize
        }

        // 2. Stream through BlockDetector in irregular chunks (1, 7, 480, 1000)
        val streamedBlocks = mutableListOf<Pair<Long, Float>>()
        val detector = BlockDetector(bank, hopSize) { sampleIdx, energies ->
            streamedBlocks.add(Pair(sampleIdx, energies[0]))
        }

        val chunkSizes = intArrayOf(1, 7, 13, 240, 480, 1000)
        var offset = 0
        var chunkIdx = 0
        while (offset < totalSamples) {
            val chunkSize = chunkSizes[chunkIdx % chunkSizes.size]
            val count = minOf(chunkSize, totalSamples - offset)
            detector.process(fullSignal, offset, count)
            offset += count
            chunkIdx++
        }

        assertEquals("Block count mismatch between monolithic and streamed", expectedBlocks.size, streamedBlocks.size)
        for (i in expectedBlocks.indices) {
            assertEquals("Sample index mismatch at block $i", expectedBlocks[i].first, streamedBlocks[i].first)
            assertEquals("Energy mismatch at block $i", expectedBlocks[i].second, streamedBlocks[i].second, 1e-5f)
        }
    }

    @Test
    fun testEdgeCasesSilenceDcAndValidation() {
        val freq = 18000.0
        val blockSize = 480
        val bank = GoertzelBank(doubleArrayOf(freq), audioSpec48k, blockSize, windowed = true)
        val out = FloatArray(1)

        // 1. Silence input returns 0.0f (no NaN, no Inf)
        val silence = FloatArray(blockSize)
        bank.energies(silence, 0, out)
        assertEquals(0.0f, out[0], 0.0f)
        assertFalse(out[0].isNaN())
        assertFalse(out[0].isInfinite())

        // 2. Full-scale DC input (constant 1.0f) returns near-zero (attenuated > 40 dB)
        val dc = FloatArray(blockSize) { 1.0f }
        bank.energies(dc, 0, out)
        assertTrue("DC input must not leak significantly into tone detector: ${out[0]}", out[0] < 1e-4f)
        assertFalse(out[0].isNaN())

        // 3. Validation
        assertThrows(IllegalArgumentException::class.java) {
            GoertzelBank(doubleArrayOf(), audioSpec48k, 480)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GoertzelBank(doubleArrayOf(1000.0), audioSpec48k, 0)
        }
    }

    @Test
    fun testGoertzelBankMicroBenchmark() {
        val tonePlan = TonePlan(Band.STEALTH, audioSpec48k)
        val all18Tones = (tonePlan.dataTones + listOf(tonePlan.syncTone0, tonePlan.syncTone1)).toDoubleArray()
        val blockSize = 1440 // 30ms Normal block
        val bank = GoertzelBank(all18Tones, audioSpec48k, blockSize, windowed = true)

        val testSignal = FloatArray(blockSize) { i -> (0.8 * kotlin.math.sin(2.0 * kotlin.math.PI * 18000.0 * i / 48000.0)).toFloat() }
        val out = FloatArray(18)

        // Warm up JIT
        for (i in 0 until 500) {
            bank.energies(testSignal, 0, out)
        }

        val iterations = 5000
        val startTime = System.nanoTime()
        for (i in 0 until iterations) {
            bank.energies(testSignal, 0, out)
        }
        val durationNs = System.nanoTime() - startTime
        val totalSamplesProcessed = iterations.toLong() * blockSize
        val samplesPerSec = (totalSamplesProcessed.toDouble() / (durationNs / 1e9)).toLong()
        val throughputMbPerSec = (samplesPerSec * 4.0) / (1024.0 * 1024.0)

        println(
            "GoertzelBank Micro-Benchmark (18 Filters): " +
                    "$samplesPerSec samples/sec (${String.format("%.2f", throughputMbPerSec)} MB/s) " +
                    "(${String.format("%.2f", samplesPerSec.toDouble() / 48000.0)}x realtime audio)"
        )

        // Realtime factor must be comfortably > 10x
        assertTrue(samplesPerSec > 480000)
    }
}
