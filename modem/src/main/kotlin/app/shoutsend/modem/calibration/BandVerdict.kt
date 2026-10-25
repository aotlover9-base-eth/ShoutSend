package app.shoutsend.modem.calibration

/**
 * Quality rating for an acoustic frequency band assessed during calibration.
 */
public enum class BandVerdict {
    /**
     * Excellent signal-to-noise ratio (>= 20 dB) and flat frequency response (<= 12 dB variation).
     * Recommended for high-speed acoustic communication.
     */
    GOOD,

    /**
     * Adequate signal-to-noise ratio (>= 12 dB) and acceptable frequency response (<= 18 dB variation).
     * Suitable for standard communication speed.
     */
    USABLE,

    /**
     * Marginal signal-to-noise ratio (>= 6 dB) or pronounced high-frequency roll-off (<= 24 dB variation).
     * Requires robust redundancy and slower symbol rates.
     */
    POOR,

    /**
     * Insufficient signal-to-noise ratio (< 6 dB), extreme roll-off (> 24 dB), or signal not detected.
     * Cannot reliably convey acoustic data on this hardware.
     */
    UNUSABLE
}
