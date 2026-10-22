package app.shoutsend.modem.modulator

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.packet.AirTime
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModeBlockCodec
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.SymbolMapper
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.protocol.ProtocolConstants
import app.shoutsend.modem.protocol.Whitener
import app.shoutsend.modem.testutil.WavWriter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10

class ModulatorTest {

    private val audioSpec48k = AudioSpec(48000)

    @Test
    fun testAirTimeDurationSecondsMatchesEstimateWithinOneSymbol() {
        val testProfiles = listOf(
            ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 2, rsProfileId = 1),
            ModemProfile(Band.RETRO, Scheme.DTFSK, Speed.FAST, repeats = 2, rsProfileId = 1),
            ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.ROBUST, repeats = 1, rsProfileId = 0),
            ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.FAST, repeats = 3, rsProfileId = 2)
        )
        val payloadSizes = intArrayOf(1, 42, 84, 120)

        for (profile in testProfiles) {
            for (size in payloadSizes) {
                val payload = ByteArray(size) { i -> ((i * 17 + 3) and 0xFF).toByte() }
                val plan = TransmissionBuilder.build(
                    type = MessageType.TEXT,
                    flags = MessageFlags.NONE,
                    payload = payload,
                    profile = profile,
                    audioSpec = audioSpec48k,
                    leadInSilenceMs = 250,
                    tailSilenceMs = 150
                )
                val modulator = Modulator(plan)

                val estimatedDuration = AirTime.estimate(
                    profile = profile,
                    onAirBytes = size,
                    repeats = profile.repeats,
                    leadInSilenceMs = 250,
                    tailSilenceMs = 150
                )

                val measuredDuration = modulator.durationSeconds
                val oneSymbolDuration = profile.speed.durationMs / 1000.0
                val diff = abs(measuredDuration - estimatedDuration)

                assertTrue(
                    "Duration mismatch for profile $profile, size $size: measured=$measuredDuration, estimated=$estimatedDuration, diff=$diff > $oneSymbolDuration",
                    diff <= oneSymbolDuration
                )
            }
        }
    }

    @Test
    fun testPreambleRegionContainsEnergyOnlyAtS0AndS1() {
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "Preamble Test".encodeToByteArray(),
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val modulator = Modulator(plan)
        val pcm = modulator.modulateToFloatArray()

        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val headerRampSamples = Timing.rampSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)

        val tonePlan = plan.tonePlan
        val s0Freq = tonePlan.frequency(16)
        val s1Freq = tonePlan.frequency(17)

        // Evaluate all 16 preamble symbols
        for (i in 0 until 16) {
            val symStart = i * headerDurSamples
            val steadyStart = symStart + headerRampSamples
            val steadyLength = headerDurSamples - headerRampSamples

            val powerS0 = computeGoertzelPower(pcm, steadyStart, steadyLength, s0Freq, 48000)
            val powerS1 = computeGoertzelPower(pcm, steadyStart, steadyLength, s1Freq, 48000)

            if (i % 2 == 0) {
                assertTrue("Symbol $i should be S0: powerS0=$powerS0, powerS1=$powerS1", powerS0 > powerS1 * 100.0)
                // Check suppression on data tones
                for (d in 0..15) {
                    val pData = computeGoertzelPower(pcm, steadyStart, steadyLength, tonePlan.frequency(d), 48000)
                    val diffDb = 10.0 * log10(pData / powerS0)
                    assertTrue("Data tone $d on symbol $i should be suppressed (diff $diffDb dB)", diffDb < -30.0)
                }
            } else {
                assertTrue("Symbol $i should be S1: powerS1=$powerS1, powerS0=$powerS0", powerS1 > powerS0 * 100.0)
                // Check suppression on data tones
                for (d in 0..15) {
                    val pData = computeGoertzelPower(pcm, steadyStart, steadyLength, tonePlan.frequency(d), 48000)
                    val diffDb = 10.0 * log10(pData / powerS1)
                    assertTrue("Data tone $d on symbol $i should be suppressed (diff $diffDb dB)", diffDb < -30.0)
                }
            }
        }
    }

    @Test
    fun testSyncWordMatchesConstantPatternViaGoertzel() {
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "Sync Test".encodeToByteArray(),
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val modulator = Modulator(plan)
        val pcm = modulator.modulateToFloatArray()

        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val headerRampSamples = Timing.rampSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val tonePlan = plan.tonePlan

        // Sync word begins right after 16 preamble symbols
        val syncStartSample = 16 * headerDurSamples
        val decodedSync = IntArray(8)

        for (i in 0 until 8) {
            val symStart = syncStartSample + i * headerDurSamples
            val steadyStart = symStart + headerRampSamples
            val steadyLength = headerDurSamples - headerRampSamples
            decodedSync[i] = detectMfsk16Tone(pcm, steadyStart, steadyLength, tonePlan, 48000)
        }

        assertArrayEquals(
            "Decoded sync word must match ProtocolConstants.SYNC_WORD_PATTERN",
            ProtocolConstants.SYNC_WORD_PATTERN,
            decodedSync
        )
    }

    @Test
    fun testModeBlockDecodesToActiveProfileParameters() {
        val profile = ModemProfile(Band.RETRO, Scheme.DTFSK, Speed.FAST, repeats = 1, rsProfileId = 2)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "Mode Test".encodeToByteArray(),
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val modulator = Modulator(plan)
        val pcm = modulator.modulateToFloatArray()

        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val headerRampSamples = Timing.rampSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val tonePlan = plan.tonePlan

        // Mode block begins right after preamble (16) + sync (8) = 24 symbols
        val modeStartSample = 24 * headerDurSamples
        val decodedModeSymbols = IntArray(4)

        for (i in 0 until 4) {
            val symStart = modeStartSample + i * headerDurSamples
            val steadyStart = symStart + headerRampSamples
            val steadyLength = headerDurSamples - headerRampSamples
            decodedModeSymbols[i] = detectMfsk16Tone(pcm, steadyStart, steadyLength, tonePlan, 48000)
        }

        // Mode block is sent twice (2 symbols each)
        assertEquals(decodedModeSymbols[0], decodedModeSymbols[2])
        assertEquals(decodedModeSymbols[1], decodedModeSymbols[3])

        val decodedModeResult = ModeBlockCodec.decodeMode(decodedModeSymbols)
        assertTrue("Mode decoding should succeed", decodedModeResult.isSuccess)
        val decodedMode = decodedModeResult.getOrThrow()
        assertEquals(profile.scheme, decodedMode.scheme)
        assertEquals(profile.speed, decodedMode.speed)
        assertEquals(profile.rsProfileId, decodedMode.rsProfileId)
    }

    @Test
    fun testCodewordDecodesBackToExactBytesMfsk16() {
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val testPayload = "Hello ShoutSend Acoustic Modem!".encodeToByteArray()
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = testPayload,
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val modulator = Modulator(plan)
        val pcm = modulator.modulateToFloatArray()

        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val payloadDurSamples = Timing.symbolSamples(profile.speed, audioSpec48k)
        val payloadRampSamples = Timing.rampSamples(profile.speed, audioSpec48k)
        val tonePlan = plan.tonePlan

        // Frames start after 16 + 8 + 4 = 28 header symbols
        var cursor = 28 * headerDurSamples

        for (f in plan.frames.indices) {
            // 1. Frame Marker (4 symbols)
            val decodedMarker = IntArray(4)
            for (m in 0 until 4) {
                val steadyStart = cursor + payloadRampSamples
                val steadyLength = payloadDurSamples - payloadRampSamples
                decodedMarker[m] = detectMfsk16Tone(pcm, steadyStart, steadyLength, tonePlan, 48000)
                cursor += payloadDurSamples
            }
            assertArrayEquals(ProtocolConstants.FRAME_MARKER_PATTERN, decodedMarker)

            // 2. Codeword symbols
            val expectedCodeword = plan.codewords[f]
            val expectedSymbolCount = expectedCodeword.size * 2
            val decodedSymbols = IntArray(expectedSymbolCount)

            for (s in 0 until expectedSymbolCount) {
                val steadyStart = cursor + payloadRampSamples
                val steadyLength = payloadDurSamples - payloadRampSamples
                decodedSymbols[s] = detectMfsk16Tone(pcm, steadyStart, steadyLength, tonePlan, 48000)
                cursor += payloadDurSamples
            }

            // Convert symbols back to whitened bytes and de-whiten
            val whitenedBytes = SymbolMapper.symbolsToBytes(decodedSymbols, Scheme.MFSK16, expectedCodeword.size)
            val restoredBytes = Whitener.whiten(whitenedBytes, ProtocolConstants.WHITEN_SEED_V1)

            assertArrayEquals(
                "Restored codeword $f must match original Reed-Solomon codeword byte-for-byte",
                expectedCodeword,
                restoredBytes
            )
        }
    }

    @Test
    fun testCodewordDecodesBackToExactBytesDtfsk() {
        val profile = ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 1, rsProfileId = 1)
        val testPayload = "DTFSK High Speed Acoustic Link".encodeToByteArray()
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = testPayload,
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val modulator = Modulator(plan)
        val pcm = modulator.modulateToFloatArray()

        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val payloadDurSamples = Timing.symbolSamples(profile.speed, audioSpec48k)
        val payloadRampSamples = Timing.rampSamples(profile.speed, audioSpec48k)
        val tonePlan = plan.tonePlan

        var cursor = 28 * headerDurSamples

        for (f in plan.frames.indices) {
            // Frame Marker
            for (m in 0 until 4) {
                cursor += payloadDurSamples
            }

            // Codeword DTFSK symbols
            val expectedCodeword = plan.codewords[f]
            val expectedSymbolCount = (expectedCodeword.size * 8 + 5) / 6
            val decodedSymbols = IntArray(expectedSymbolCount)

            for (s in 0 until expectedSymbolCount) {
                val steadyStart = cursor + payloadRampSamples
                val steadyLength = payloadDurSamples - payloadRampSamples
                decodedSymbols[s] = detectDtfskSymbol(pcm, steadyStart, steadyLength, tonePlan, 48000)
                cursor += payloadDurSamples
            }

            val whitenedBytes = SymbolMapper.symbolsToBytes(decodedSymbols, Scheme.DTFSK, expectedCodeword.size)
            val restoredBytes = Whitener.whiten(whitenedBytes, ProtocolConstants.WHITEN_SEED_V1)

            assertArrayEquals(
                "Restored DTFSK codeword $f must match original byte-for-byte",
                expectedCodeword,
                restoredBytes
            )
        }
    }

    @Test
    fun testRepeatedFramesAreIdenticalAndDecodeToSameCodewords() {
        val profile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 2, rsProfileId = 1)
        val testPayload = "Repeat Consistency Check".encodeToByteArray()
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = testPayload,
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0,
            repeats = 2
        )
        val modulator = Modulator(plan)
        val pcm = modulator.modulateToFloatArray()

        val headerDurSamples = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec48k)
        val payloadDurSamples = Timing.symbolSamples(profile.speed, audioSpec48k)
        val payloadRampSamples = Timing.rampSamples(profile.speed, audioSpec48k)
        val tonePlan = plan.tonePlan

        val symbolsPerFrame = 4 + (plan.codewords[0].size * 2)
        val frameDurationSamples = symbolsPerFrame * payloadDurSamples

        val rep1Start = 28 * headerDurSamples
        val rep2Start = rep1Start + frameDurationSamples

        // Decode Repeat 1
        val rep1Symbols = IntArray(symbolsPerFrame)
        for (s in 0 until symbolsPerFrame) {
            val steadyStart = rep1Start + s * payloadDurSamples + payloadRampSamples
            val steadyLength = payloadDurSamples - payloadRampSamples
            rep1Symbols[s] = detectMfsk16Tone(pcm, steadyStart, steadyLength, tonePlan, 48000)
        }

        // Decode Repeat 2
        val rep2Symbols = IntArray(symbolsPerFrame)
        for (s in 0 until symbolsPerFrame) {
            val steadyStart = rep2Start + s * payloadDurSamples + payloadRampSamples
            val steadyLength = payloadDurSamples - payloadRampSamples
            rep2Symbols[s] = detectMfsk16Tone(pcm, steadyStart, steadyLength, tonePlan, 48000)
        }

        // 1. Both repeats decode to identical symbol sequences
        assertArrayEquals("Repeats must decode to identical symbols", rep1Symbols, rep2Symbols)

        // 2. Both frame markers match FRAME_MARKER_PATTERN
        assertArrayEquals(ProtocolConstants.FRAME_MARKER_PATTERN, rep1Symbols.sliceArray(0..3))
        assertArrayEquals(ProtocolConstants.FRAME_MARKER_PATTERN, rep2Symbols.sliceArray(0..3))

        // 3. Both decode to the exact codeword bytes
        val rep1CodewordBytes = Whitener.whiten(
            SymbolMapper.symbolsToBytes(rep1Symbols.sliceArray(4 until symbolsPerFrame), Scheme.MFSK16, plan.codewords[0].size),
            ProtocolConstants.WHITEN_SEED_V1
        )
        val rep2CodewordBytes = Whitener.whiten(
            SymbolMapper.symbolsToBytes(rep2Symbols.sliceArray(4 until symbolsPerFrame), Scheme.MFSK16, plan.codewords[0].size),
            ProtocolConstants.WHITEN_SEED_V1
        )
        assertArrayEquals(plan.codewords[0], rep1CodewordBytes)
        assertArrayEquals(plan.codewords[0], rep2CodewordBytes)
    }

    @Test
    fun testTotalSampleCountAndStreamingChunkIndependence() {
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "Chunk Independence Test".encodeToByteArray(),
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 250,
            tailSilenceMs = 150
        )
        val modulator = Modulator(plan)
        val monolithic = modulator.modulateToFloatArray()

        assertEquals(plan.totalSamples.toInt(), monolithic.size)

        // Stream render in chunks of 480 samples (10 ms chunks)
        val streamed = FloatArray(monolithic.size)
        var offset = 0
        while (offset < monolithic.size) {
            val count = minOf(480, monolithic.size - offset)
            modulator.render(offset.toLong(), streamed, offset, count)
            offset += count
        }

        for (i in monolithic.indices) {
            assertEquals(
                "Streamed mismatch at sample $i",
                monolithic[i].toRawBits(),
                streamed[i].toRawBits()
            )
        }
    }

    @Test
    fun testRenderingTenSecondsOfAudioDoesNotAllocateInSteadyState() {
        val profile = ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 2, rsProfileId = 1)
        val payload = ByteArray(120) { 0x42 }
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = payload,
            profile = profile,
            audioSpec = audioSpec48k
        )
        val modulator = Modulator(plan)

        // Preallocate destination chunk buffer
        val chunkSize = 480
        val chunkBuffer = FloatArray(chunkSize)

        // Warm up JIT and pre-populate RampTable cache
        for (i in 0 until 50) {
            modulator.render((i * chunkSize).toLong() % modulator.totalSamples, chunkBuffer, 0, chunkSize)
        }

        val threadMxBean = try {
            java.lang.management.ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean
        } catch (_: Throwable) {
            null
        }

        if (threadMxBean != null && threadMxBean.isThreadAllocatedMemorySupported) {
            val allocatedBefore = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().id)

            // Render 10 seconds of streaming audio (1000 calls of 480 samples @ 48kHz)
            for (step in 0 until 1000) {
                val sampleOffset = (step.toLong() * chunkSize) % (modulator.totalSamples - chunkSize)
                modulator.render(sampleOffset, chunkBuffer, 0, chunkSize)
            }

            val allocatedAfter = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().id)
            val totalAllocated = allocatedAfter - allocatedBefore

            // In steady state, ToneSynth renders purely using primitives and preallocated arrays (0 allocations)
            assertEquals("Steady-state streaming render must perform 0 heap allocations", 0L, totalAllocated)
        }
    }

    @Test
    fun testWavGenerationProducesValidAudioFiles() {
        val retroProfile = ModemProfile(Band.RETRO, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val retroPlan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "ShoutSend Retro Acoustic Demo".encodeToByteArray(),
            profile = retroProfile,
            audioSpec = audioSpec48k
        )
        val retroPcm = Modulator.modulateToPcm16(retroPlan)

        val outputDir = File("build/outputs")
        outputDir.mkdirs()

        val retroWav = File(outputDir, "shoutsend_retro_sample.wav")
        WavWriter.writeWav(retroWav, retroPcm, 48000)
        assertTrue(retroWav.exists())
        assertEquals(44L + retroPcm.size * 2, retroWav.length())

        val stealthProfile = ModemProfile(Band.STEALTH, Scheme.DTFSK, Speed.FAST, repeats = 1, rsProfileId = 1)
        val stealthPlan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "ShoutSend Stealth Acoustic Demo".encodeToByteArray(),
            profile = stealthProfile,
            audioSpec = audioSpec48k
        )
        val stealthPcm = Modulator.modulateToPcm16(stealthPlan)

        val stealthWav = File(outputDir, "shoutsend_stealth_sample.wav")
        WavWriter.writeWav(stealthWav, stealthPcm, 48000)
        assertTrue(stealthWav.exists())
        assertEquals(44L + stealthPcm.size * 2, stealthWav.length())
    }

    private fun detectMfsk16Tone(
        samples: FloatArray,
        startSample: Int,
        symbolLength: Int,
        tonePlan: TonePlan,
        sampleRateHz: Int
    ): Int {
        var bestTone = -1
        var bestPower = -1.0
        for (toneIdx in 0..15) {
            val freq = tonePlan.frequency(toneIdx)
            val power = computeGoertzelPower(samples, startSample, symbolLength, freq, sampleRateHz)
            if (power > bestPower) {
                bestPower = power
                bestTone = toneIdx
            }
        }
        return bestTone
    }

    private fun detectDtfskSymbol(
        samples: FloatArray,
        startSample: Int,
        symbolLength: Int,
        tonePlan: TonePlan,
        sampleRateHz: Int
    ): Int {
        var bestLow = -1
        var bestLowPower = -1.0
        for (toneIdx in 0..7) {
            val freq = tonePlan.frequency(toneIdx)
            val power = computeGoertzelPower(samples, startSample, symbolLength, freq, sampleRateHz)
            if (power > bestLowPower) {
                bestLowPower = power
                bestLow = toneIdx
            }
        }

        var bestHigh = -1
        var bestHighPower = -1.0
        for (toneIdx in 8..15) {
            val freq = tonePlan.frequency(toneIdx)
            val power = computeGoertzelPower(samples, startSample, symbolLength, freq, sampleRateHz)
            if (power > bestHighPower) {
                bestHighPower = power
                bestHigh = toneIdx
            }
        }

        return SymbolMapper.combineDtfskTones(bestLow, bestHigh)
    }

    private fun computeGoertzelPower(
        samples: FloatArray,
        offset: Int,
        length: Int,
        freqHz: Double,
        sampleRateHz: Int
    ): Double {
        val omega = (2.0 * PI * freqHz) / sampleRateHz
        val coeff = 2.0 * cos(omega)
        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until length) {
            val s = samples[offset + i].toDouble()
            q0 = coeff * q1 - q2 + s
            q2 = q1
            q1 = q0
        }
        return q1 * q1 + q2 * q2 - coeff * q1 * q2
    }
}
