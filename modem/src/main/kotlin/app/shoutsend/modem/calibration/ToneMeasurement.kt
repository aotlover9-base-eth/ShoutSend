package app.shoutsend.modem.calibration

/**
 * Empirical measurement of an individual acoustic tone.
 *
 * @param index Tone index: 0..15 for data tones, 16 for syncTone0, 17 for syncTone1.
 * @param frequencyHz Physical center frequency of the tone in Hertz.
 * @param levelDbfs Absolute measured signal power level in decibels relative to full scale.
 * @param relativeLevelDb Power level in dB relative to the strongest tone in the evaluated band (<= 0.0 dB).
 * @param snrDb Estimated signal-to-noise ratio in decibels relative to ambient noise floor.
 */
public data class ToneMeasurement(
    public val index: Int,
    public val frequencyHz: Double,
    public val levelDbfs: Double,
    public val relativeLevelDb: Double,
    public val snrDb: Double
)
