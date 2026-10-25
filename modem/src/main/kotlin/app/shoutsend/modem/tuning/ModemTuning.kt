package app.shoutsend.modem.tuning

import app.shoutsend.modem.profile.Speed

/**
 * Result of validating [ModemTuning] parameters against physical transceiver constraints.
 */
public sealed interface ModemTuningValidationResult {
    public object Valid : ModemTuningValidationResult
    public data class Invalid(public val errors: List<String>) : ModemTuningValidationResult
}

/**
 * Complete set of tunable acoustic modem DSP parameters.
 *
 * All parameters default to the empirical values tuned in Step 23.
 * The User APK uses [DEFAULT] strictly, while the Dev APK allows runtime overrides
 * via the developer DSP Lab (Step 50).
 */
public data class ModemTuning(
    public val outputLevel: Float = 0.85f,
    public val leadInSilenceMs: Int = 250,
    public val tailSilenceMs: Int = 150,
    public val robustDurationMs: Int = 60,
    public val normalDurationMs: Int = 30,
    public val fastDurationMs: Int = 20,
    public val robustRampMs: Int = 6,
    public val normalRampMs: Int = 4,
    public val fastRampMs: Int = 3,
    public val erasureThreshold: Float = 0.20f,
    public val detectionMarginDb: Double = 6.0,
    public val absoluteDetectionFloorDbfs: Double = -70.0,
    public val minPreambleSlots: Int = 6,
    public val preambleSnrThresholdDb: Double = 6.0,
    public val markerMissLimit: Int = 2,
    public val noiseAttackSec: Double = 1.5,
    public val noiseDecaySec: Double = 0.25,
    public val fftSize: Int = 1024,
    public val spectrumMinDbfs: Float = -90.0f,
    public val spectrumMaxDbfs: Float = 0.0f
) {
    /**
     * Resolves the symbol duration in milliseconds for the given [speed].
     */
    public fun durationMs(speed: Speed): Int = when (speed) {
        Speed.ROBUST -> robustDurationMs
        Speed.NORMAL -> normalDurationMs
        Speed.FAST -> fastDurationMs
    }

    /**
     * Resolves the raised-cosine ramp duration in milliseconds for the given [speed].
     */
    public fun rampMs(speed: Speed): Int = when (speed) {
        Speed.ROBUST -> robustRampMs
        Speed.NORMAL -> normalRampMs
        Speed.FAST -> fastRampMs
    }

    /**
     * Validates all tuning parameters, ensuring numerical stability and physical feasibility.
     */
    public fun validate(): ModemTuningValidationResult {
        val errors = mutableListOf<String>()

        if (outputLevel !in 0.1f..1.0f) {
            errors.add("outputLevel must be in 0.1..1.0, got $outputLevel")
        }
        if (leadInSilenceMs !in 50..2000) {
            errors.add("leadInSilenceMs must be in 50..2000 ms, got $leadInSilenceMs")
        }
        if (tailSilenceMs !in 50..2000) {
            errors.add("tailSilenceMs must be in 50..2000 ms, got $tailSilenceMs")
        }
        if (robustDurationMs !in 30..150) {
            errors.add("robustDurationMs must be in 30..150 ms, got $robustDurationMs")
        }
        if (normalDurationMs !in 15..60) {
            errors.add("normalDurationMs must be in 15..60 ms, got $normalDurationMs")
        }
        if (fastDurationMs !in 10..40) {
            errors.add("fastDurationMs must be in 10..40 ms, got $fastDurationMs")
        }
        if (robustRampMs !in 1..(robustDurationMs / 2)) {
            errors.add("robustRampMs must be in 1..${robustDurationMs / 2} ms, got $robustRampMs")
        }
        if (normalRampMs !in 1..(normalDurationMs / 2)) {
            errors.add("normalRampMs must be in 1..${normalDurationMs / 2} ms, got $normalRampMs")
        }
        if (fastRampMs !in 1..(fastDurationMs / 2)) {
            errors.add("fastRampMs must be in 1..${fastDurationMs / 2} ms, got $fastRampMs")
        }
        if (erasureThreshold !in 0.05f..0.95f) {
            errors.add("erasureThreshold must be in 0.05..0.95, got $erasureThreshold")
        }
        if (detectionMarginDb !in 1.0..24.0) {
            errors.add("detectionMarginDb must be in 1.0..24.0 dB, got $detectionMarginDb")
        }
        if (absoluteDetectionFloorDbfs !in -100.0..-20.0) {
            errors.add("absoluteDetectionFloorDbfs must be in -100.0..-20.0 dBFS, got $absoluteDetectionFloorDbfs")
        }
        if (minPreambleSlots !in 3..16) {
            errors.add("minPreambleSlots must be in 3..16, got $minPreambleSlots")
        }
        if (preambleSnrThresholdDb !in 1.0..24.0) {
            errors.add("preambleSnrThresholdDb must be in 1.0..24.0 dB, got $preambleSnrThresholdDb")
        }
        if (markerMissLimit !in 0..6) {
            errors.add("markerMissLimit must be in 0..6, got $markerMissLimit")
        }
        if (noiseAttackSec !in 0.1..10.0) {
            errors.add("noiseAttackSec must be in 0.1..10.0 s, got $noiseAttackSec")
        }
        if (noiseDecaySec !in 0.01..5.0) {
            errors.add("noiseDecaySec must be in 0.01..5.0 s, got $noiseDecaySec")
        }
        if (fftSize !in 256..4096 || (fftSize and (fftSize - 1)) != 0) {
            errors.add("fftSize must be a power of 2 between 256 and 4096, got $fftSize")
        }
        if (spectrumMinDbfs >= spectrumMaxDbfs) {
            errors.add("spectrumMinDbfs must be strictly less than spectrumMaxDbfs ($spectrumMinDbfs >= $spectrumMaxDbfs)")
        }

        return if (errors.isEmpty()) ModemTuningValidationResult.Valid else ModemTuningValidationResult.Invalid(errors)
    }

    public companion object {
        /**
         * Default immutable production tuning matching Protocol v1 constants.
         */
        public val DEFAULT: ModemTuning = ModemTuning()
    }
}
