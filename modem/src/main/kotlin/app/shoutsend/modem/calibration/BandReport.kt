package app.shoutsend.modem.calibration

import app.shoutsend.modem.profile.Band

/**
 * Detailed calibration report for a single acoustic band ([Band.RETRO] or [Band.STEALTH]).
 *
 * @param band Frequency band evaluated.
 * @param toneMeasurements Measurements for each of the 18 test tones (16 data + 2 sync).
 * @param strongestTone The tone exhibiting highest received power in the band.
 * @param weakestTone The tone exhibiting lowest received power in the band.
 * @param inBandVariationDb Dynamic range variation between strongest and weakest tone (dB).
 * @param noiseFloorDbfs Ambient in-band noise floor level measured prior to test sequence.
 * @param verdict Overall qualification rating ([BandVerdict.GOOD], [BandVerdict.USABLE], etc.).
 * @param verdictReason Human-readable justification for the verdict.
 */
public data class BandReport(
    public val band: Band,
    public val toneMeasurements: List<ToneMeasurement>,
    public val strongestTone: ToneMeasurement,
    public val weakestTone: ToneMeasurement,
    public val inBandVariationDb: Double,
    public val noiseFloorDbfs: Double,
    public val verdict: BandVerdict,
    public val verdictReason: String
) {
    public val isUsable: Boolean
        get() = verdict != BandVerdict.UNUSABLE
}
