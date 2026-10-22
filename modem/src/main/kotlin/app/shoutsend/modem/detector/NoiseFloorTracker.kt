package app.shoutsend.modem.detector

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Dynamic per-tone acoustic noise floor tracker and SNR estimator.
 *
 * Implements:
 * 1. Asymmetric exponential moving average (EMA): follows downward drops quickly and upward rises slowly.
 * 2. Transmission lock freezing: freezes floor updates during active transmissions so signals are not learned as noise.
 * 3. Dynamic detection thresholds: requires signal energy >= floor * margin (default +6 dB) and >= absolute minimum.
 * 4. Calibrated confidence metric combining runner-up separation and SNR above threshold (for erasure marking).
 * 5. Broadband ambient noise level estimator in dBFS for UI meters.
 * 6. Zero heap allocations on update path.
 */
public class NoiseFloorTracker(
    public val toneCount: Int,
    public val params: NoiseTrackerParams = NoiseTrackerParams()
) {
    init {
        require(toneCount > 0) { "toneCount must be positive: $toneCount" }
    }

    private val floors = FloatArray(toneCount) { params.initialFloorPower }
    private var isFrozenState = false

    // Precomputed smoothing coefficients: alpha = 1.0 - exp(-dt / tau)
    private val alphaUp = (1.0 - exp(-params.hopIntervalSec / params.attackTimeSec)).toFloat()
    private val alphaDown = (1.0 - exp(-params.hopIntervalSec / params.decayTimeSec)).toFloat()
    private val marginLinear = 10.0.pow(params.marginDb / 10.0).toFloat()
    private val minFloorPower = params.minFloorPower

    /**
     * True if noise floor adaptation is currently frozen.
     */
    public fun isFrozen(): Boolean = isFrozenState

    /**
     * Freezes tracker adaptation (invoked when transmission preamble is acquired).
     */
    public fun freeze() {
        isFrozenState = true
    }

    /**
     * Resumes tracker adaptation (invoked when transmission ends or loses sync).
     */
    public fun unfreeze() {
        isFrozenState = false
    }

    /**
     * Resets tracker state across all tones to [initialLevelPower].
     */
    public fun reset(initialLevelPower: Float = params.initialFloorPower) {
        val safeInitial = max(initialLevelPower, minFloorPower)
        var i = 0
        while (i < toneCount) {
            floors[i] = safeInitial
            i++
        }
        isFrozenState = false
    }

    /**
     * Ingests a new vector of tone energies evaluated over the last hop interval.
     * Updates tracked floor per tone using asymmetric time constants. Zero allocations.
     */
    public fun update(energies: FloatArray, offset: Int = 0) {
        require(offset >= 0) { "offset cannot be negative: $offset" }
        require(offset + toneCount <= energies.size) { "energies array too small: ${energies.size} < ${offset + toneCount}" }

        if (isFrozenState) return

        var i = 0
        while (i < toneCount) {
            val e = energies[offset + i]
            val current = floors[i]
            if (e > current) {
                floors[i] = current + alphaUp * (e - current)
            } else {
                val updated = current + alphaDown * (e - current)
                floors[i] = if (updated > minFloorPower) updated else minFloorPower
            }
            i++
        }
    }

    /**
     * Current estimated noise floor power for [toneIndex].
     */
    public fun noiseFloor(toneIndex: Int): Float {
        require(toneIndex in 0 until toneCount) { "toneIndex out of bounds: $toneIndex" }
        return floors[toneIndex]
    }

    /**
     * Minimum energy required for a detection decision on [toneIndex].
     * Computed as `max(noiseFloor * margin, minFloorPower * margin)`.
     */
    public fun threshold(toneIndex: Int): Float {
        require(toneIndex in 0 until toneCount) { "toneIndex out of bounds: $toneIndex" }
        val f = floors[toneIndex]
        val effectiveFloor = if (f > minFloorPower) f else minFloorPower
        return effectiveFloor * marginLinear
    }

    /**
     * Computes the Signal-to-Noise Ratio (SNR) in decibels for [signalEnergy] on [toneIndex].
     */
    public fun snrDb(signalEnergy: Float, toneIndex: Int): Float {
        require(toneIndex in 0 until toneCount) { "toneIndex out of bounds: $toneIndex" }
        val floor = floors[toneIndex]
        val safeFloor = if (floor > minFloorPower) floor else minFloorPower
        val safeSignal = if (signalEnergy > 1e-12f) signalEnergy else 1e-12f
        return (10.0 * log10((safeSignal / safeFloor).toDouble())).toFloat()
    }

    /**
     * Computes a broadband ambient acoustic noise level estimate across all tracked tones in dBFS.
     * Useful for UI noise meters and environment diagnostics.
     */
    public fun ambientLevelDbfs(): Float {
        var sum = 0.0
        var i = 0
        while (i < toneCount) {
            sum += floors[i]
            i++
        }
        val avgPower = sum / toneCount.toDouble()
        val safePower = if (avgPower > 1e-12) avgPower else 1e-12
        return (10.0 * log10(safePower)).toFloat()
    }

    /**
     * Evaluates demodulator confidence `[0.0, 1.0]` for a decision between [winnerEnergy] and [runnerUpEnergy].
     *
     * Invariant:
     * - Returns `0.0f` if [winnerEnergy] falls below [threshold].
     * - Combines winner-to-runner separation ratio with SNR factor above threshold.
     */
    public fun confidence(winnerEnergy: Float, runnerUpEnergy: Float, toneIndex: Int): Float {
        val th = threshold(toneIndex)
        if (winnerEnergy < th) return 0.0f

        val safeWinner = if (winnerEnergy > 1e-12f) winnerEnergy else 1e-12f
        val separation = ((safeWinner - runnerUpEnergy) / safeWinner).coerceIn(0.0f, 1.0f)

        val snr = snrDb(winnerEnergy, toneIndex)
        // SNR factor scales linearly from 0.0 at marginDb to 1.0 at marginDb + 12 dB
        val snrFactor = ((snr - params.marginDb) / 12.0f).coerceIn(0.0f, 1.0f)

        return (separation * snrFactor).coerceIn(0.0f, 1.0f)
    }
}
