package app.shoutsend.core.devtools

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Pure Kotlin color utilities for HSV <-> RGB and hex conversions without Android SDK dependencies.
 */
object ColorUtils {

    fun rgbToHsv(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val h = when {
            delta == 0f -> 0f
            max == r -> (((g - b) / delta) % 6f) * 60f
            max == g -> (((b - r) / delta) + 2f) * 60f
            else -> (((r - g) / delta) + 4f) * 60f
        }.let { if (it < 0f) it + 360f else it }

        val s = if (max == 0f) 0f else delta / max
        val v = max
        return floatArrayOf(h, s, v)
    }

    fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
        val clampedH = h.coerceIn(0f, 360f)
        val clampedS = s.coerceIn(0f, 1f)
        val clampedV = v.coerceIn(0f, 1f)

        val c = clampedV * clampedS
        val x = c * (1f - abs(((clampedH / 60f) % 2f) - 1f))
        val m = clampedV - c

        val (rPrime, gPrime, bPrime) = when ((clampedH.toInt() / 60) % 6) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    fun colorToHex(color: Color): String {
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun hexToColor(input: String): Color? {
        val clean = input.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> {
                    val num = clean.toLong(16)
                    val r = ((num shr 16) and 255L).toFloat() / 255f
                    val g = ((num shr 8) and 255L).toFloat() / 255f
                    val b = (num and 255L).toFloat() / 255f
                    Color(red = r, green = g, blue = b, alpha = 1f)
                }
                8 -> {
                    val num = clean.toLong(16)
                    val a = ((num shr 24) and 255L).toFloat() / 255f
                    val r = ((num shr 16) and 255L).toFloat() / 255f
                    val g = ((num shr 8) and 255L).toFloat() / 255f
                    val b = (num and 255L).toFloat() / 255f
                    Color(red = r, green = g, blue = b, alpha = a)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
