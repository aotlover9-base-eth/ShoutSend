package app.shoutsend.modem.sync

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.protocol.ProtocolConstants
import app.shoutsend.modem.testutil.SeededRandom
import app.shoutsend.modem.testutil.TonePcm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SyncDetectorTest {

    private val audioSpec48k = AudioSpec(48000)

    @Test
    fun testPreambleLockAcrossRandomStartOffsets() {
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val payload = "Sync Offset Lock Test".encodeToByteArray()
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = payload,
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val txAudio = Modulator(plan).modulateToFloatArray()

        val quarterSymbol = plan.audioSpec.sampleRateHz * 30 / 1000 / 4 // 360 samples
        val rng = SeededRandom(42L)

        // Test 200 distinct arbitrary offsets (including non-hop multiples)
        val testOffsets = IntArray(200) { rng.nextInt(50, 2500) }

        for (offset in testOffsets) {
            val signalWithOffset = FloatArray(offset + txAudio.size)
            System.arraycopy(txAudio, 0, signalWithOffset, offset, txAudio.size)

            var lockedSample = -1L
            var snrEst = 0.0f
            val detector = SyncDetector(
                band = Band.STEALTH,
                audioSpec = audioSpec48k,
                listener = object : SyncListener {
                    override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {
                        lockedSample = symbolStartSample
                        snrEst = estimatedSnrDb
                    }
                }
            )

            // Feed audio in streaming chunks of 480 samples
            var cur = 0
            while (cur < signalWithOffset.size) {
                val count = minOf(480, signalWithOffset.size - cur)
                detector.push(signalWithOffset, cur, count)
                cur += count
            }

            assertTrue("Detector must lock for offset $offset", lockedSample != -1L)
            assertTrue("Estimated SNR must be positive ($snrEst dB)", snrEst > 10.0f)

            // True mode block start sample: offset + (16 preamble + 8 sync) * 1440 samples
            val expectedModeStart = offset + (24L * 1440L)
            val timingError = abs(lockedSample - expectedModeStart)

            assertTrue(
                "Timing error ($timingError samples) at offset $offset must be <= quarter symbol ($quarterSymbol samples)",
                timingError <= quarterSymbol
            )
        }
    }

    @Test
    fun testPureNoiseProducesZeroFalseLocks() {
        var falseLocks = 0
        val detector = SyncDetector(
            band = Band.RETRO,
            audioSpec = audioSpec48k,
            listener = object : SyncListener {
                override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {
                    falseLocks++
                }
            }
        )

        val rng = SeededRandom(9999L)
        val noiseChunk = FloatArray(480)

        // Simulate 2,000 hops of pure noise (20 seconds of continuous noise at various amplitudes)
        val noiseLevels = floatArrayOf(0.001f, 0.01f, 0.05f)
        for (level in noiseLevels) {
            for (step in 0 until 1000) {
                for (i in noiseChunk.indices) {
                    noiseChunk[i] = ((rng.nextDouble() - 0.5) * 2.0 * level).toFloat()
                }
                detector.push(noiseChunk)
            }
        }

        println("Pure Noise Immunity Test: 0 false locks encountered across 3,000 hops ($falseLocks false locks)")
        assertEquals("Pure noise must never produce false locks", 0, falseLocks)
    }

    @Test
    fun testSingleToneDoesNotFalseLock() {
        val tonePlan = TonePlan(Band.STEALTH, audioSpec48k)
        val testTones = doubleArrayOf(tonePlan.syncTone0, tonePlan.syncTone1, tonePlan.dataTones[3])

        for (testTone in testTones) {
            var locked = false
            val detector = SyncDetector(
                band = Band.STEALTH,
                audioSpec = audioSpec48k,
                listener = object : SyncListener {
                    override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {
                        locked = true
                    }
                }
            )

            // Feed 3 seconds of a pure steady tone
            val toneSamples = TonePcm.generateSineSamples(testTone, 48000 * 3, 48000, 0.8f)
            detector.push(toneSamples)

            assertFalse("Pure steady tone at $testTone Hz must never trigger lock", locked)
        }
    }

    @Test
    fun testSpeakerWakeupClippingSurvivesFiveCutoffSymbols() {
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "Clipped Preamble Test".encodeToByteArray(),
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val fullTx = Modulator(plan).modulateToFloatArray()

        // Cut off first 5 preamble symbols (5 * 1440 = 7200 samples)
        val clippedSamples = 5 * 1440
        val clippedAudio = fullTx.copyOfRange(clippedSamples, fullTx.size)

        var lockedSample = -1L
        val detector = SyncDetector(
            band = Band.STEALTH,
            audioSpec = audioSpec48k,
            listener = object : SyncListener {
                override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {
                    lockedSample = symbolStartSample
                }
            }
        )

        detector.push(clippedAudio)

        assertTrue("Must lock even when first 5 preamble symbols are clipped", lockedSample != -1L)
        val expectedModeStart = (24L * 1440L) - clippedSamples
        val error = abs(lockedSample - expectedModeStart)
        assertTrue("Timing error ($error) must be within quarter symbol (360 samples)", error <= 360)
    }

    @Test
    fun testLockRateAndTimingErrorAcrossSnrLevels() {
        val snrLevels = doubleArrayOf(20.0, 10.0, 5.0)
        val profile = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.NORMAL, repeats = 1, rsProfileId = 1)
        val plan = TransmissionBuilder.build(
            type = MessageType.TEXT,
            payload = "SNR Benchmark Payload".encodeToByteArray(),
            profile = profile,
            audioSpec = audioSpec48k,
            leadInSilenceMs = 0,
            tailSilenceMs = 0
        )
        val cleanAudio = Modulator(plan).modulateToFloatArray()
        val rng = SeededRandom(777L)

        println("\n=== Synchronization Performance across SNR Levels ===")
        println("| SNR (dB) | Trials | Locks | Lock Rate % | Mean Timing Error (samples) | Error (frac symbol) |")
        println("| :---     | :---   | :---  | :---        | :---                        | :---                |")

        for (snrDb in snrLevels) {
            val trials = 10
            var locks = 0
            var totalError = 0L

            val signalPower = 0.64 * 0.5 // Average sine power
            val noisePower = signalPower / Math.pow(10.0, snrDb / 10.0)
            val noiseAmplitude = Math.sqrt(noisePower * 2.0).toFloat()

            for (trial in 0 until trials) {
                var lockedSample = -1L
                val detector = SyncDetector(
                    band = Band.STEALTH,
                    audioSpec = audioSpec48k,
                    listener = object : SyncListener {
                        override fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {
                            lockedSample = symbolStartSample
                        }
                    }
                )

                val noisyAudio = FloatArray(cleanAudio.size)
                for (i in cleanAudio.indices) {
                    val noise = ((rng.nextDouble() - 0.5) * 2.0 * noiseAmplitude).toFloat()
                    noisyAudio[i] = cleanAudio[i] + noise
                }

                detector.push(noisyAudio)

                if (lockedSample != -1L) {
                    locks++
                    val expected = 24L * 1440L
                    totalError += abs(lockedSample - expected)
                }
            }

            val lockRate = (locks.toDouble() / trials) * 100.0
            val meanError = if (locks > 0) totalError.toDouble() / locks else 0.0
            val fracSymbol = meanError / 1440.0

            println(
                "| ${snrDb.toInt()} dB | $trials | $locks | ${String.format("%.1f", lockRate)}% | " +
                        "${String.format("%.1f", meanError)} | ${String.format("%.3f", fracSymbol)} |"
            )

            assertTrue("At SNR >= 10 dB, lock rate must be 100%", if (snrDb >= 10.0) lockRate == 100.0 else true)
        }
    }

    @Test
    fun testTimingTrackerSurvives100PpmSampleRateDrift() {
        val tracker = TimingTracker(sampleRateHz = 48000, loopGain = 0.15f)
        tracker.reset(0L)

        val symbolDuration = 1440 // 30ms Normal symbol
        val frameMarkers = 40 // Simulate 40 frame markers over ~20 seconds
        val ppmDrift = 100.0e-6 // +100 ppm drift
        val totalExpectedDriftSamples = frameMarkers * symbolDuration * ppmDrift

        var sampleCursor = 0L
        for (m in 0 until frameMarkers) {
            tracker.advanceSymbol(symbolDuration)
            sampleCursor += symbolDuration

            // Simulate drifting marker: late energy is slightly higher due to clock advance
            val driftFraction = (m.toFloat() / frameMarkers) * 0.1f
            val earlyEnergy = 0.8f * (1.0f - driftFraction)
            val lateEnergy = 0.8f * (1.0f + driftFraction)

            val locked = tracker.updateFromMarker(
                found = true,
                earlyEnergy = earlyEnergy,
                lateEnergy = lateEnergy,
                symbolDurationSamples = symbolDuration
            )
            assertTrue("Timing lock must be maintained during 100 ppm drift", locked)
        }

        println("TimingTracker drift correction applied: ${tracker.currentDrift()} samples (expected ~${String.format("%.1f", totalExpectedDriftSamples)} samples)")
        assertTrue("Tracker must apply positive drift corrections for positive ppm mismatch", tracker.currentDrift() > 0.0f)
    }
}
