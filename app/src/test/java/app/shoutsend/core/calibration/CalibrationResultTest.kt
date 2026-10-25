package app.shoutsend.core.calibration

import app.shoutsend.modem.calibration.BandVerdict
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationResultTest {

    @Test
    fun testJsonSerializationRoundtrip() {
        val retroSummary = BandReportSummary(
            band = Band.RETRO,
            verdict = BandVerdict.GOOD,
            strongestDbfs = -18.5,
            weakestDbfs = -24.0,
            inBandVariationDb = 5.5,
            noiseFloorDbfs = -55.0,
            minSnrDb = 31.0,
            reason = "Optimal SNR and response"
        )
        val stealthSummary = BandReportSummary(
            band = Band.STEALTH,
            verdict = BandVerdict.GOOD,
            strongestDbfs = -22.0,
            weakestDbfs = -31.0,
            inBandVariationDb = 9.0,
            noiseFloorDbfs = -60.0,
            minSnrDb = 29.0,
            reason = "Optimal Stealth SNR"
        )

        val original = CalibrationResult(
            timestampMs = 1700000000000L,
            retroReport = retroSummary,
            stealthReport = stealthSummary,
            recommendedBand = Band.STEALTH,
            recommendedSpeed = Speed.NORMAL,
            reasonCode = "STEALTH_GOOD",
            warnings = listOf("HEADPHONES_CONNECTED")
        )

        val json = original.toJson()
        assertTrue(json.contains("STEALTH_GOOD"))
        assertTrue(json.contains("HEADPHONES_CONNECTED"))

        val parsed = CalibrationResult.fromJson(json)
        assertNotNull(parsed)
        assertEquals(original.timestampMs, parsed!!.timestampMs)
        assertEquals(original.recommendedBand, parsed.recommendedBand)
        assertEquals(original.recommendedSpeed, parsed.recommendedSpeed)
        assertEquals(original.reasonCode, parsed.reasonCode)
        assertEquals(1, parsed.warnings.size)
        assertEquals("HEADPHONES_CONNECTED", parsed.warnings[0])

        assertEquals(original.retroReport.verdict, parsed.retroReport.verdict)
        assertEquals(original.retroReport.inBandVariationDb, parsed.retroReport.inBandVariationDb, 0.01)
        assertEquals(original.stealthReport.verdict, parsed.stealthReport.verdict)
    }

    @Test
    fun testMalformedJsonReturnsNullGracefully() {
        assertNull(CalibrationResult.fromJson("invalid json"))
        assertNull(CalibrationResult.fromJson("{}"))
        assertNull(CalibrationResult.fromJson(""))
    }
}
