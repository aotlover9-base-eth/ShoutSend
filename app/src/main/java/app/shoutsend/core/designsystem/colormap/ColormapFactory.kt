package app.shoutsend.core.designsystem.colormap

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.shoutsend.core.designsystem.waterfallPalette
import kotlin.math.sqrt

/**
 * Factory assembling a 256-entry ARGB lookup table for the acoustic waterfall display.
 *
 * Interpolates smoothly across four tokenized stops:
 * - Index 0: Stop 0 (ambient noise floor, surfaceContainerLowest)
 * - Index 85: Stop 1 (low signal activity, primaryContainer)
 * - Index 170: Stop 2 (demodulation candidate, primary)
 * - Index 255: Stop 3 (peak energy, tertiary)
 *
 * Interpolation executes in gamma-corrected linear photometric space to guarantee
 * smooth perceptual energy gradients without dark transition banding.
 */
object ColormapFactory {

    const val COLORMAP_SIZE = 256
    private const val STOP_LOW = 0
    private const val STOP_MID = 85
    private const val STOP_HIGH = 170
    private const val STOP_PEAK = 255

    fun build(colorScheme: ColorScheme): IntArray {
        val palette = waterfallPalette(colorScheme)
        return build(palette[0], palette[1], palette[2], palette[3])
    }

    fun build(
        signalLow: Color,
        signalMid: Color,
        signalHigh: Color,
        signalPeak: Color
    ): IntArray {
        val lut = IntArray(COLORMAP_SIZE)

        for (i in 0 until COLORMAP_SIZE) {
            lut[i] = when {
                i == STOP_LOW -> signalLow.toArgb()
                i < STOP_MID -> {
                    val frac = (i - STOP_LOW).toFloat() / (STOP_MID - STOP_LOW).toFloat()
                    interpolateGamma(signalLow, signalMid, frac)
                }
                i == STOP_MID -> signalMid.toArgb()
                i < STOP_HIGH -> {
                    val frac = (i - STOP_MID).toFloat() / (STOP_HIGH - STOP_MID).toFloat()
                    interpolateGamma(signalMid, signalHigh, frac)
                }
                i == STOP_HIGH -> signalHigh.toArgb()
                i < STOP_PEAK -> {
                    val frac = (i - STOP_HIGH).toFloat() / (STOP_PEAK - STOP_HIGH).toFloat()
                    interpolateGamma(signalHigh, signalPeak, frac)
                }
                else -> signalPeak.toArgb()
            }
        }

        return lut
    }

    private fun interpolateGamma(c1: Color, c2: Color, fraction: Float): Int {
        val f = fraction.coerceIn(0.0f, 1.0f)
        val a = c1.alpha + f * (c2.alpha - c1.alpha)

        // Gamma expansion to linear photometric space (gamma ~ 2.0 approximation for speed & smoothness)
        val r1 = c1.red * c1.red
        val g1 = c1.green * c1.green
        val b1 = c1.blue * c1.blue

        val r2 = c2.red * c2.red
        val g2 = c2.green * c2.green
        val b2 = c2.blue * c2.blue

        val rLin = r1 + f * (r2 - r1)
        val gLin = g1 + f * (g2 - g1)
        val bLin = b1 + f * (b2 - b1)

        // Gamma compression back to sRGB
        val r = sqrt(rLin.toDouble().coerceAtLeast(0.0)).toFloat()
        val g = sqrt(gLin.toDouble().coerceAtLeast(0.0)).toFloat()
        val b = sqrt(bLin.toDouble().coerceAtLeast(0.0)).toFloat()

        val aInt = (a.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt().coerceIn(0, 255)
        val rInt = (r.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt().coerceIn(0, 255)
        val gInt = (g.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt().coerceIn(0, 255)
        val bInt = (b.coerceIn(0.0f, 1.0f) * 255.0f + 0.5f).toInt().coerceIn(0, 255)

        return (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
    }
}
