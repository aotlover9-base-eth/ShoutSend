package app.shoutsend.modem.detector

import app.shoutsend.modem.tuning.ModemTuning
import kotlin.math.pow

/**
 * Configuration parameters governing dynamic noise floor tracking, adaptation rates, and detection thresholds.
 *
 * @property hopIntervalSec Time interval between successive energy updates in seconds (hopSize / sampleRateHz).
 * @property attackTimeSec Adaptation time constant for increasing noise (upward change) in seconds.
 * @property decayTimeSec Adaptation time constant for decreasing noise (downward change) in seconds.
 * @property marginDb Relative detection margin above noise floor in dB (default 6.0 dB).
 * @property minFloorPower Absolute minimum floor power to prevent digital silence from triggering (-70 dBFS).
 * @property initialFloorPower Starting noise floor power estimate (-60 dBFS = 1e-6f).
 */
public data class NoiseTrackerParams(
    public val hopIntervalSec: Double = 0.010, // 10 ms default hop
    public val attackTimeSec: Double = 1.5,     // 1.5s upward smoothing
    public val decayTimeSec: Double = 0.25,     // 0.25s downward smoothing (faster)
    public val marginDb: Float = 6.0f,          // 6 dB detection margin
    public val minFloorPower: Float = 1e-7f,    // -70 dBFS absolute threshold
    public val initialFloorPower: Float = 1e-6f // -60 dBFS initial floor
) {
    init {
        require(hopIntervalSec > 0.0) { "hopIntervalSec must be positive: $hopIntervalSec" }
        require(attackTimeSec > 0.0) { "attackTimeSec must be positive: $attackTimeSec" }
        require(decayTimeSec > 0.0) { "decayTimeSec must be positive: $decayTimeSec" }
        require(marginDb > 0.0f) { "marginDb must be positive: $marginDb" }
        require(minFloorPower > 0.0f) { "minFloorPower must be positive: $minFloorPower" }
    }

    public companion object {
        public fun fromTuning(tuning: ModemTuning, hopIntervalSec: Double): NoiseTrackerParams =
            NoiseTrackerParams(
                hopIntervalSec = hopIntervalSec,
                attackTimeSec = tuning.noiseAttackSec,
                decayTimeSec = tuning.noiseDecaySec,
                marginDb = tuning.detectionMarginDb.toFloat(),
                minFloorPower = 10.0.pow(tuning.absoluteDetectionFloorDbfs / 10.0).toFloat()
            )
    }
}
