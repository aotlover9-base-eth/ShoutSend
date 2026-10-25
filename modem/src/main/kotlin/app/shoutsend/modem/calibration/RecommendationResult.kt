package app.shoutsend.modem.calibration

import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed

/**
 * Categorical reason for modem configuration recommendation.
 */
public enum class RecommendationReason {
    /** Near-ultrasound Stealth band qualified as GOOD; silent transmission enabled. */
    STEALTH_GOOD,

    /** Audible Retro band qualified as GOOD; high-throughput FAST speed recommended. */
    RETRO_GOOD_FAST,

    /** Audible Retro band qualified as USABLE; balanced NORMAL speed recommended. */
    RETRO_USABLE_NORMAL,

    /** Audible Retro band qualified as POOR; error-resilient ROBUST speed recommended. */
    RETRO_POOR_ROBUST,

    /** Both bands failed due to ambient acoustic noise floor exceeding safe SNR. */
    ALL_UNUSABLE_HIGH_NOISE,

    /** Both bands failed because test tones were not detected or microphone muted. */
    ALL_UNUSABLE_NO_SIGNAL
}

/**
 * Result of modem profile recommendation engine.
 */
public data class RecommendationResult(
    public val recommendedBand: Band,
    public val recommendedSpeed: Speed,
    public val reason: RecommendationReason
)

/**
 * Pure recommendation engine selecting optimal band and speed based on empirical calibration reports.
 */
public object CalibrationRecommender {

    /**
     * Recommends optimal [Band] and [Speed] according to the hierarchy:
     * 1. STEALTH if and only if Stealth verdict is GOOD.
     * 2. Otherwise RETRO:
     *    - FAST if Retro is GOOD.
     *    - NORMAL if Retro is USABLE.
     *    - ROBUST if Retro is POOR or UNUSABLE.
     */
    public fun recommend(retroReport: BandReport, stealthReport: BandReport): RecommendationResult {
        if (stealthReport.verdict == BandVerdict.GOOD) {
            return RecommendationResult(
                recommendedBand = Band.STEALTH,
                recommendedSpeed = Speed.NORMAL,
                reason = RecommendationReason.STEALTH_GOOD
            )
        }

        return when (retroReport.verdict) {
            BandVerdict.GOOD -> RecommendationResult(
                recommendedBand = Band.RETRO,
                recommendedSpeed = Speed.FAST,
                reason = RecommendationReason.RETRO_GOOD_FAST
            )
            BandVerdict.USABLE -> RecommendationResult(
                recommendedBand = Band.RETRO,
                recommendedSpeed = Speed.NORMAL,
                reason = RecommendationReason.RETRO_USABLE_NORMAL
            )
            BandVerdict.POOR -> RecommendationResult(
                recommendedBand = Band.RETRO,
                recommendedSpeed = Speed.ROBUST,
                reason = RecommendationReason.RETRO_POOR_ROBUST
            )
            BandVerdict.UNUSABLE -> {
                val reason = if (retroReport.noiseFloorDbfs > -35.0) {
                    RecommendationReason.ALL_UNUSABLE_HIGH_NOISE
                } else {
                    RecommendationReason.ALL_UNUSABLE_NO_SIGNAL
                }
                RecommendationResult(
                    recommendedBand = Band.RETRO,
                    recommendedSpeed = Speed.ROBUST,
                    reason = reason
                )
            }
        }
    }
}
