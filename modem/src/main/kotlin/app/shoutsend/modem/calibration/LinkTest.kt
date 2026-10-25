package app.shoutsend.modem.calibration

import kotlin.math.roundToInt

/**
 * Subjective link quality rating for two-phone acoustic field testing.
 */
public enum class LinkRating {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

/**
 * Result of link quality calculation.
 *
 * @param score Scaled integer quality score from 0 (unusable) to 100 (flawless).
 * @param rating Categorical rating for UI display.
 * @param summary Human-readable summary of SNR and error correction metrics.
 */
public data class LinkQualityResult(
    public val score: Int,
    public val rating: LinkRating,
    public val summary: String
)

/**
 * Fixed payload and mathematical link quality scorer for two-phone field testing.
 */
public object LinkTest {

    /** Canonical link test payload string. */
    public const val CANONICAL_PAYLOAD_TEXT: String = "ShoutSend Link Quality Test v1"

    /** Canonical UTF-8 bytes for link test packet. */
    public val CANONICAL_PAYLOAD_BYTES: ByteArray = CANONICAL_PAYLOAD_TEXT.toByteArray(Charsets.UTF_8)

    /**
     * Pure function computing link quality from physical reception metrics.
     *
     * @param snrDb Measured SNR in decibels at demodulator lock.
     * @param correctedBytes Total bytes reconstructed by Reed-Solomon decoder.
     * @param totalBytes Total payload bytes across all frames.
     * @param lostFrames Number of missing or CRC-corrupted frames.
     * @param totalFrames Total frames in transmission plan.
     */
    public fun calculateQuality(
        snrDb: Double,
        correctedBytes: Int,
        totalBytes: Int,
        lostFrames: Int,
        totalFrames: Int
    ): LinkQualityResult {
        if (totalFrames <= 0 || lostFrames >= totalFrames) {
            return LinkQualityResult(
                score = 0,
                rating = LinkRating.POOR,
                summary = "All frames lost or corrupt"
            )
        }

        // SNR factor: 6 dB (0.0) to 24 dB (1.0)
        val snrNorm = ((snrDb - 6.0) / 18.0).coerceIn(0.0, 1.0)

        // RS correction penalty factor
        val errorRatio = if (totalBytes > 0) correctedBytes.toDouble() / totalBytes.toDouble() else 0.0
        val rsNorm = (1.0 - (2.5 * errorRatio)).coerceIn(0.0, 1.0)

        // Frame delivery factor
        val frameDeliveryRatio = (totalFrames - lostFrames).toDouble() / totalFrames.toDouble()

        val rawScore = frameDeliveryRatio * ((0.55 * snrNorm) + (0.45 * rsNorm)) * 100.0
        val score = rawScore.roundToInt().coerceIn(0, 100)

        val rating = when {
            score >= 80 -> LinkRating.EXCELLENT
            score >= 60 -> LinkRating.GOOD
            score >= 40 -> LinkRating.FAIR
            else -> LinkRating.POOR
        }

        val summary = "Score $score/100 (${rating.name}): SNR ${snrDb.toInt()} dB, $correctedBytes RS corr, $lostFrames/$totalFrames lost"
        return LinkQualityResult(score, rating, summary)
    }
}
