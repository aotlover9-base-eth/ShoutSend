package app.shoutsend.modem.calibration

/**
 * Objective empirical thresholds for acoustic band qualification and recommendation.
 * Documented in DEC-048.
 */
public object CalibrationThresholds {
    // GOOD thresholds
    public const val GOOD_MIN_SNR_DB: Double = 20.0
    public const val GOOD_MAX_VARIATION_DB: Double = 12.0

    // USABLE thresholds
    public const val USABLE_MIN_SNR_DB: Double = 12.0
    public const val USABLE_MAX_VARIATION_DB: Double = 18.0

    // POOR thresholds
    public const val POOR_MIN_SNR_DB: Double = 6.0
    public const val POOR_MAX_VARIATION_DB: Double = 24.0

    // Absolute minimum level to distinguish signal from digital silence / noise floor
    public const val MIN_ABSOLUTE_LEVEL_DBFS: Double = -65.0

    // Minimum marker tone detection margin over ambient noise
    public const val MARKER_DETECTION_MARGIN_DB: Double = 10.0
}
