package app.shoutsend.core.designsystem.colormap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.waterfallPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColormapFactoryTest {

    private fun luminance(argb: Int): Double {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.299 * r + 0.587 * g + 0.114 * b
    }

    @Test
    fun testColormapEndpointsAndStops() {
        val colors = DesignTokens.defaultDark().colors
        val lut = ColormapFactory.build(
            signalLow = colors.signalLow,
            signalMid = colors.signalMid,
            signalHigh = colors.signalHigh,
            signalPeak = colors.signalPeak
        )

        assertEquals("LUT size must be 256", 256, lut.size)
        assertEquals("Index 0 must match signalLow", colors.signalLow.toArgb(), lut[0])
        assertEquals("Index 85 must match signalMid", colors.signalMid.toArgb(), lut[85])
        assertEquals("Index 170 must match signalHigh", colors.signalHigh.toArgb(), lut[170])
        assertEquals("Index 255 must match signalPeak", colors.signalPeak.toArgb(), lut[255])
    }

    @Test
    fun testColormapFromColorSchemeMatchesWaterfallPalette() {
        val scheme = DesignTokens.defaultDark().colors.colorScheme
        val palette = waterfallPalette(scheme)
        val lut = ColormapFactory.build(scheme)

        assertEquals(256, lut.size)
        assertEquals(palette[0].toArgb(), lut[0])
        assertEquals(palette[1].toArgb(), lut[85])
        assertEquals(palette[2].toArgb(), lut[170])
        assertEquals(palette[3].toArgb(), lut[255])
    }

    @Test
    fun testColormapBrightnessIncreasesFromAmbientToSignal() {
        val colors = DesignTokens.defaultDark().colors
        val lut = ColormapFactory.build(
            signalLow = colors.signalLow,
            signalMid = colors.signalMid,
            signalHigh = colors.signalHigh,
            signalPeak = colors.signalPeak
        )

        val lumLow = luminance(lut[0])
        val lumMid = luminance(lut[85])
        val lumHigh = luminance(lut[170])

        assertTrue("Luminance at mid ($lumMid) must exceed low ($lumLow)", lumMid > lumLow)
        assertTrue("Luminance at high ($lumHigh) must exceed mid ($lumMid)", lumHigh > lumMid)
    }
}
