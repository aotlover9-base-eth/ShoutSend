package app.shoutsend.core.capability

import app.shoutsend.core.audio.receiver.AudioEffectsState
import app.shoutsend.core.audio.transmitter.OutputRouteInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityReportTest {

    @Test
    fun testCapabilityReportFieldsAndSummaryFormatting() {
        val report = CapabilityReport(
            nativeOutputSampleRateHz = 48000,
            outputFramesPerBuffer = 192,
            supportedInputSampleRates = listOf(48000, 44100),
            isUnprocessedSupported = true,
            hasLowLatencyAudio = true,
            hasProAudio = false,
            deviceModel = "Google Pixel 8",
            effects = AudioEffectsState(aecAvailable = true, nsAvailable = true, agcAvailable = false),
            outputRoute = OutputRouteInfo(
                hasHeadset = false,
                hasBluetooth = false,
                hasUsb = false,
                volumeFraction = 0.85f,
                nativeSampleRateHz = 48000
            )
        )

        assertEquals(48000, report.nativeOutputSampleRateHz)
        assertEquals(192, report.outputFramesPerBuffer)
        assertEquals(2, report.supportedInputSampleRates.size)
        assertTrue(report.isUnprocessedSupported)
        assertTrue(report.hasLowLatencyAudio)
        assertFalse(report.hasProAudio)
        assertEquals("Google Pixel 8", report.deviceModel)

        val summary = report.toSummaryString()
        assertTrue(summary.contains("Google Pixel 8"))
        assertTrue(summary.contains("48000 Hz"))
        assertTrue(summary.contains("Unprocessed Mic: YES"))
        assertTrue(summary.contains("Low Latency: YES"))
        assertTrue(summary.contains("Pro Audio: NO"))
        assertTrue(summary.contains("AEC=true"))
    }
}
