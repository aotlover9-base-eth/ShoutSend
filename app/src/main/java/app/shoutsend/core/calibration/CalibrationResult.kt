package app.shoutsend.core.calibration

import app.shoutsend.modem.calibration.BandReport
import app.shoutsend.modem.calibration.BandVerdict
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val calibrationJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Concise serialized summary of an acoustic band report for persistence in settings.
 */
@Serializable
data class BandReportSummary(
    val band: Band,
    val verdict: BandVerdict,
    val strongestDbfs: Double,
    val weakestDbfs: Double,
    val inBandVariationDb: Double,
    val noiseFloorDbfs: Double,
    val minSnrDb: Double,
    val reason: String
) {
    companion object {
        fun fromReport(report: BandReport): BandReportSummary = BandReportSummary(
            band = report.band,
            verdict = report.verdict,
            strongestDbfs = report.strongestTone.levelDbfs,
            weakestDbfs = report.weakestTone.levelDbfs,
            inBandVariationDb = report.inBandVariationDb,
            noiseFloorDbfs = report.noiseFloorDbfs,
            minSnrDb = report.weakestTone.snrDb,
            reason = report.verdictReason
        )
    }
}

/**
 * Persisted acoustic self-calibration result.
 */
@Serializable
data class CalibrationResult(
    val timestampMs: Long,
    val retroReport: BandReportSummary,
    val stealthReport: BandReportSummary,
    val recommendedBand: Band,
    val recommendedSpeed: Speed,
    val reasonCode: String,
    val warnings: List<String>
) {
    fun toJson(): String = calibrationJson.encodeToString(this)

    companion object {
        fun fromJson(jsonStr: String): CalibrationResult? = try {
            calibrationJson.decodeFromString<CalibrationResult>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }
}
