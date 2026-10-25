package app.shoutsend.modem.modulator

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Precomputed acoustic transmission envelope sampled at regular time windows (default 20 ms).
 * Enables visual animations to synchronize with physical sound synthesis without per-frame allocations.
 *
 * @property windowDurationMs Duration of each discrete sampling window in milliseconds (default 20ms).
 * @property rms Normalized RMS energy per window in range [0.0f, 1.0f].
 * @property toneIndex Dominant active tone index per window (-1 if silent, 0..15 data, 16 S0, 17 S1).
 */
public class TransmissionEnvelope(
    public val windowDurationMs: Int,
    public val rms: FloatArray,
    public val toneIndex: IntArray
) {
    public val totalWindows: Int get() = rms.size

    /**
     * Resolves the envelope point at a given elapsed time in seconds.
     */
    public fun getAtTime(elapsedSeconds: Double): EnvelopePoint {
        if (totalWindows == 0 || elapsedSeconds < 0.0) {
            return EnvelopePoint(0.0f, -1)
        }
        val windowIndex = (elapsedSeconds * 1000.0 / windowDurationMs).toInt()
        val clampedIndex = windowIndex.coerceIn(0, totalWindows - 1)
        return EnvelopePoint(rms[clampedIndex], toneIndex[clampedIndex])
    }

    /**
     * Resolves the envelope point at a given played audio frame/sample count.
     */
    public fun getAtSample(samplePosition: Long, sampleRateHz: Int): EnvelopePoint {
        if (sampleRateHz <= 0) return EnvelopePoint(0.0f, -1)
        val elapsedSeconds = samplePosition.toDouble() / sampleRateHz
        return getAtTime(elapsedSeconds)
    }

    public data class EnvelopePoint(val rms: Float, val toneIndex: Int)

    public companion object {
        public const val DEFAULT_WINDOW_MS: Int = 20
        private const val NOMINAL_PEAK_LEVEL: Float = 0.8f
        private const val NOMINAL_PEAK_RMS: Float = NOMINAL_PEAK_LEVEL / 1.41421356f

        /**
         * Computes a [TransmissionEnvelope] from a synthesized [TransmissionPlan].
         */
        public fun compute(
            plan: TransmissionPlan,
            windowMs: Int = DEFAULT_WINDOW_MS
        ): TransmissionEnvelope {
            require(windowMs > 0) { "windowMs must be positive: $windowMs" }
            val sampleRateHz = plan.audioSpec.sampleRateHz
            val samplesPerWindow = (sampleRateHz.toLong() * windowMs) / 1000L
            val totalSamples = plan.totalSamples

            if (totalSamples <= 0L || samplesPerWindow <= 0L) {
                return TransmissionEnvelope(windowMs, FloatArray(0), IntArray(0))
            }

            val numWindows = ((totalSamples + samplesPerWindow - 1L) / samplesPerWindow).toInt()
            val rmsArray = FloatArray(numWindows)
            val toneArray = IntArray(numWindows) { -1 }

            val events = plan.schedule.events

            for (w in 0 until numWindows) {
                val wStart = w * samplesPerWindow
                val wEnd = min(totalSamples, (w + 1) * samplesPerWindow)
                val wLen = (wEnd - wStart).toInt()
                if (wLen <= 0) continue

                val startIndex = plan.schedule.findFirstCandidateIndex(wStart)
                var accumulatedPower = 0.0
                var maxOverlapSamples = 0
                var dominantTone = -1

                var idx = startIndex
                while (idx < events.size) {
                    val event = events[idx]
                    if (event.startSample >= wEnd) break

                    if (event.endSample > wStart) {
                        val overlapStart = max(wStart, event.startSample)
                        val overlapEnd = min(wEnd, event.endSample)
                        val overlapSamples = (overlapEnd - overlapStart).toInt()

                        if (overlapSamples > 0) {
                            val eventRms = event.levelPerTone / 1.41421356f
                            val weight = overlapSamples.toDouble() / wLen
                            accumulatedPower += (eventRms * eventRms) * weight

                            if (overlapSamples > maxOverlapSamples) {
                                maxOverlapSamples = overlapSamples
                                dominantTone = event.toneIndexes.firstOrNull() ?: -1
                            }
                        }
                    }
                    idx++
                }

                val windowRms = sqrt(accumulatedPower).toFloat()
                val normalizedRms = (windowRms / NOMINAL_PEAK_RMS).coerceIn(0.0f, 1.0f)

                rmsArray[w] = normalizedRms
                toneArray[w] = dominantTone
            }

            return TransmissionEnvelope(windowMs, rmsArray, toneArray)
        }
    }
}
