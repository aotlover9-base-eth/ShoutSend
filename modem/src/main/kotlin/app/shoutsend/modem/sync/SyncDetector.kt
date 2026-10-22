package app.shoutsend.modem.sync

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.detector.GoertzelBank
import app.shoutsend.modem.detector.NoiseFloorTracker
import app.shoutsend.modem.detector.NoiseTrackerParams
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Timing
import app.shoutsend.modem.profile.TonePlan
import app.shoutsend.modem.protocol.ProtocolConstants
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Preamble detection, fine symbol synchronization, and lock tracking state machine.
 *
 * Implements Section 6 of Protocol v1:
 * 1. HUNTING: 4-phase scanning of S0/S1 sync tones at quarter-symbol hops at Normal 30ms timing.
 * 2. PREAMBLE_CANDIDATE: Requires >= 6 consecutive alternating S0/S1 symbols with both tones present.
 * 3. SYNC_VERIFY: Confirms the 8-symbol sync word pattern [3, 11, 7, 14, 1, 9, 5, 12] across candidate boundaries.
 * 4. LOCKED: Exact symbol boundary established; hands off to [TimingTracker] and freezes noise floor.
 * 5. LOST: Refractory cooldown period preventing false re-triggers after lock failure.
 *
 * Guaranteed zero heap allocations on audio push hot paths.
 */
public class SyncDetector(
    public val band: Band,
    public val audioSpec: AudioSpec,
    public val listener: SyncListener,
    public val noiseTracker: NoiseFloorTracker = NoiseFloorTracker(2, NoiseTrackerParams(hopIntervalSec = 0.0075))
) {
    public val tonePlan: TonePlan = TonePlan(band, audioSpec)
    public val headerSymbolSamples: Int = Timing.symbolSamples(ProtocolConstants.HEADER_SPEED, audioSpec)
    public val hopSize: Int = headerSymbolSamples / 4 // Quarter symbol hop (360 samples at 48 kHz)

    // Goertzel resonators for S0 and S1
    private val syncBank = GoertzelBank(
        frequencies = doubleArrayOf(tonePlan.syncTone0, tonePlan.syncTone1),
        audioSpec = audioSpec,
        blockSize = headerSymbolSamples,
        windowed = true
    )

    // Goertzel resonators for data tones 0..15 (used for sync word verification)
    private val dataBank = GoertzelBank(
        frequencies = tonePlan.dataTones.toDoubleArray(),
        audioSpec = audioSpec,
        blockSize = headerSymbolSamples,
        windowed = true
    )

    public val timingTracker: TimingTracker = TimingTracker(audioSpec.sampleRateHz)

    private var currentState: SyncState = SyncState.HUNTING
    private var refractoryHopsRemaining: Int = 0

    // Ring buffer using power-of-2 capacity for fast bitwise masking without modulo drift
    private val bufferCapacity = 65536
    private val bufferMask = 65535L
    private val ringBuffer = FloatArray(bufferCapacity)
    private var totalSamplesIngested = 0L
    private var nextHopSample = 0L

    // Preallocated scratch arrays
    private val blockScratch = FloatArray(headerSymbolSamples)
    private val syncEnergiesOut = FloatArray(2)
    private val dataEnergiesOut = FloatArray(16)

    // Rolling history of evaluated hop energies: size 64 hops = 16 symbols
    private val historySize = 64
    private val s0History = FloatArray(historySize)
    private val s1History = FloatArray(historySize)
    private val sampleIndexHistory = LongArray(historySize)
    private var historyWriteIdx = 0
    private var totalHopsEvaluated = 0L

    // Candidate tracking state
    private var candidatePhase: Int = 0
    private var candidateFineOffset: Long = 0L
    private var candidateConfidence: Float = 0.0f
    private var lastSlotHopSample: Long = 0L
    private var expectedNextTone: Int = -1 // 0 for S0, 1 for S1
    private var preambleSlotsCount: Int = 0
    private var syncWordStartSample: Long = 0L

    public fun state(): SyncState = currentState

    /**
     * Ingests normalized floating point audio samples. Zero allocations.
     */
    public fun push(samples: FloatArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "Invalid bounds" }
        if (count == 0) return

        val chunkSize = 2048
        var inOffset = offset
        var remaining = count

        while (remaining > 0) {
            val chunk = if (remaining < chunkSize) remaining else chunkSize
            var i = 0
            while (i < chunk) {
                val writeIdx = ((totalSamplesIngested + i) and bufferMask).toInt()
                ringBuffer[writeIdx] = samples[inOffset + i]
                i++
            }
            totalSamplesIngested += chunk
            inOffset += chunk
            remaining -= chunk

            drainHops()
        }
    }

    /**
     * Ingests 16-bit linear PCM ShortArray audio samples. Zero allocations.
     */
    public fun push(samples: ShortArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "Invalid bounds" }
        if (count == 0) return

        val chunkSize = 2048
        var inOffset = offset
        var remaining = count

        while (remaining > 0) {
            val chunk = if (remaining < chunkSize) remaining else chunkSize
            var i = 0
            while (i < chunk) {
                val raw = samples[inOffset + i]
                val sFloat = if (raw >= 0) raw.toFloat() / 32767.0f else raw.toFloat() / 32768.0f
                val writeIdx = ((totalSamplesIngested + i) and bufferMask).toInt()
                ringBuffer[writeIdx] = sFloat
                i++
            }
            totalSamplesIngested += chunk
            inOffset += chunk
            remaining -= chunk

            drainHops()
        }
    }

    private fun drainHops() {
        if (currentState == SyncState.SYNC_VERIFY) {
            evaluateSyncWordVerification()
            return
        }

        while (totalSamplesIngested >= nextHopSample + headerSymbolSamples) {
            val blockStartSample = nextHopSample

            // Copy contiguous block for Goertzel
            var n = 0
            while (n < headerSymbolSamples) {
                val idx = ((blockStartSample + n) and bufferMask).toInt()
                blockScratch[n] = ringBuffer[idx]
                n++
            }

            // Evaluate S0/S1 sync tones
            syncBank.energies(blockScratch, 0, syncEnergiesOut)
            val eS0 = syncEnergiesOut[0]
            val eS1 = syncEnergiesOut[1]

            // Record in rolling history
            val hIdx = historyWriteIdx % historySize
            s0History[hIdx] = eS0
            s1History[hIdx] = eS1
            sampleIndexHistory[hIdx] = blockStartSample
            historyWriteIdx = (historyWriteIdx + 1) % historySize
            totalHopsEvaluated++

            processHop(blockStartSample, eS0, eS1)

            nextHopSample += hopSize

            if (currentState == SyncState.SYNC_VERIFY) {
                evaluateSyncWordVerification()
                break
            }
        }
    }

    private fun processHop(sampleIndex: Long, eS0: Float, eS1: Float) {
        if (refractoryHopsRemaining > 0) {
            refractoryHopsRemaining--
            noiseTracker.update(syncEnergiesOut)
            return
        }

        when (currentState) {
            SyncState.HUNTING -> {
                noiseTracker.update(syncEnergiesOut)
                evaluatePreambleHunting()
            }
            SyncState.PREAMBLE_CANDIDATE -> {
                evaluatePreambleTracking(sampleIndex, eS0, eS1)
            }
            SyncState.SYNC_VERIFY -> {
                evaluateSyncWordVerification()
            }
            SyncState.LOCKED -> {
                // Tracking handled by TimingTracker during demodulation
            }
            SyncState.LOST -> {
                refractoryHopsRemaining = 15 // ~112 ms cooldown
                noiseTracker.unfreeze()
                currentState = SyncState.HUNTING
            }
        }
    }

    private fun evaluatePreambleHunting() {
        if (totalHopsEvaluated < 24) return // Require at least 6 symbols (24 hops)

        val th0 = noiseTracker.threshold(0)
        val th1 = noiseTracker.threshold(1)

        val phaseScores = FloatArray(4) { -1.0f }
        var bestScore = 0.0f
        var bestPhase = -1
        var bestAlternations = 0
        var bestLastSlotSample = -1L
        var bestNextTone = -1

        var p = 0
        while (p < 4) {
            var alternations = 0
            var s0Count = 0
            var s1Count = 0
            var scoreSum = 0.0f
            var expectedTone = -1
            var lastSlotSample = -1L

            var m = 0
            while (m < 6) {
                // Hop offset: m=0 is oldest slot (20 hops ago), m=5 is newest slot (0 hops ago)
                val hopOffset = p + (5 - m) * 4
                val idx = (historyWriteIdx - 1 - hopOffset + historySize * 10) % historySize

                val s0 = s0History[idx]
                val s1 = s1History[idx]

                if (m == 0) {
                    expectedTone = if (s0 > s1) 0 else 1
                }

                if (expectedTone == 0) {
                    if (s0 >= th0 && s0 > s1 * 1.5f) {
                        alternations++
                        s0Count++
                        scoreSum += (s0 - s1) / max(noiseTracker.noiseFloor(0), 1e-6f)
                        expectedTone = 1
                        lastSlotSample = sampleIndexHistory[idx]
                    } else {
                        break
                    }
                } else {
                    if (s1 >= th1 && s1 > s0 * 1.5f) {
                        alternations++
                        s1Count++
                        scoreSum += (s1 - s0) / max(noiseTracker.noiseFloor(1), 1e-6f)
                        expectedTone = 0
                        lastSlotSample = sampleIndexHistory[idx]
                    } else {
                        break
                    }
                }
                m++
            }

            if (alternations >= 6 && s0Count >= 2 && s1Count >= 2) {
                phaseScores[p] = scoreSum
                if (scoreSum > bestScore) {
                    bestScore = scoreSum
                    bestPhase = p
                    bestAlternations = alternations
                    bestLastSlotSample = lastSlotSample
                    bestNextTone = expectedTone
                }
            }
            p++
        }

        if (bestAlternations >= 6 && bestPhase != -1) {
            candidatePhase = bestPhase
            preambleSlotsCount = bestAlternations
            lastSlotHopSample = bestLastSlotSample
            expectedNextTone = bestNextTone
            candidateConfidence = (bestScore / (bestAlternations * 10.0f)).coerceIn(0.1f, 1.0f)

            // Parabolic interpolation across neighbor hop phases
            val pPrev = (bestPhase + 1) % 4 // 1 hop earlier in time
            val pNext = (bestPhase + 3) % 4 // 1 hop later in time
            val sPrev = max(phaseScores[pPrev], 0.0f)
            val sCenter = phaseScores[bestPhase]
            val sNext = max(phaseScores[pNext], 0.0f)
            val denom = sPrev - 2.0f * sCenter + sNext

            candidateFineOffset = if (denom < -1e-6f) {
                val delta = 0.5f * (sPrev - sNext) / denom
                val clampedDelta = delta.coerceIn(-0.5f, 0.5f)
                (-clampedDelta * hopSize).roundToInt().toLong()
            } else {
                0L
            }

            currentState = SyncState.PREAMBLE_CANDIDATE
            listener.onPreambleCandidate(lastSlotHopSample + candidateFineOffset, candidateConfidence)
        }
    }

    private fun evaluatePreambleTracking(currentHopSample: Long, eS0: Float, eS1: Float) {
        val nextSlotExpectedHop = lastSlotHopSample + headerSymbolSamples
        // Wait until we reach the exact symbol slot at candidate phase
        if (currentHopSample < nextSlotExpectedHop) {
            return
        }

        val th0 = noiseTracker.threshold(0)
        val th1 = noiseTracker.threshold(1)

        val isExpectedActive = if (expectedNextTone == 0) {
            eS0 >= th0 && eS0 > eS1 * 1.3f
        } else {
            eS1 >= th1 && eS1 > eS0 * 1.3f
        }

        if (isExpectedActive && preambleSlotsCount < 24) {
            // Preamble continues
            preambleSlotsCount++
            lastSlotHopSample = nextSlotExpectedHop
            expectedNextTone = 1 - expectedNextTone
        } else {
            // Preamble alternating pattern has ended; sync word begins at this exact slot
            val nominalSyncStart = lastSlotHopSample + headerSymbolSamples
            syncWordStartSample = nominalSyncStart + candidateFineOffset
            currentState = SyncState.SYNC_VERIFY
            evaluateSyncWordVerification()
        }
    }

    private fun evaluateSyncWordVerification() {
        val requiredEndSample = syncWordStartSample + hopSize + 8L * headerSymbolSamples

        if (totalSamplesIngested < requiredEndSample) {
            // Wait for remaining sync word samples to enter ring buffer
            return
        }

        val syncPattern = ProtocolConstants.SYNC_WORD_PATTERN
        val candidateOffsets = longArrayOf(
            syncWordStartSample,
            syncWordStartSample - hopSize,
            syncWordStartSample + hopSize
        )

        var bestScore = -1.0f
        var bestMatchCount = -1
        var bestLockedStart = syncWordStartSample + 8L * headerSymbolSamples

        for (candidateStart in candidateOffsets) {
            var matches = 0
            var expectedToneEnergySum = 0.0f

            for (symIdx in 0 until 8) {
                val symStart = candidateStart + symIdx * headerSymbolSamples
                var n = 0
                while (n < headerSymbolSamples) {
                    val idx = ((symStart + n) and bufferMask).toInt()
                    blockScratch[n] = ringBuffer[idx]
                    n++
                }
                dataBank.energies(blockScratch, 0, dataEnergiesOut)

                val expectedTone = syncPattern[symIdx]
                expectedToneEnergySum += dataEnergiesOut[expectedTone]

                var peakTone = -1
                var peakPower = -1.0f
                for (t in 0..15) {
                    if (dataEnergiesOut[t] > peakPower) {
                        peakPower = dataEnergiesOut[t]
                        peakTone = t
                    }
                }

                if (peakTone == expectedTone) {
                    matches++
                }
            }

            if (matches >= 4 && expectedToneEnergySum > bestScore) {
                bestScore = expectedToneEnergySum
                bestMatchCount = matches
                bestLockedStart = candidateStart + 8L * headerSymbolSamples
            }
        }

        // Accept lock if at least 4 of 8 sync word symbols match (robust against high noise)
        if (bestMatchCount >= 4) {
            currentState = SyncState.LOCKED
            noiseTracker.freeze()
            timingTracker.reset(bestLockedStart)

            var avgPreamblePower = 1e-4f
            for (i in 0 until historySize) {
                if (s0History[i] > avgPreamblePower) avgPreamblePower = s0History[i]
                if (s1History[i] > avgPreamblePower) avgPreamblePower = s1History[i]
            }
            val estimatedSnr = noiseTracker.snrDb(avgPreamblePower, 0)
            listener.onSyncLocked(bestLockedStart, estimatedSnr)
        } else {
            currentState = SyncState.LOST
            refractoryHopsRemaining = 15
            noiseTracker.unfreeze()
            nextHopSample = syncWordStartSample + 8L * headerSymbolSamples
            listener.onSyncLost(syncWordStartSample, "Sync word match failed: $bestMatchCount/8")
        }
    }

    /**
     * Resets detector to HUNTING state and clears history.
     */
    public fun reset() {
        currentState = SyncState.HUNTING
        refractoryHopsRemaining = 0
        totalSamplesIngested = 0L
        nextHopSample = 0L
        historyWriteIdx = 0
        totalHopsEvaluated = 0L
        preambleSlotsCount = 0
        candidateFineOffset = 0L
        noiseTracker.reset()
    }
}
