package app.shoutsend.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Animated acoustic emblem rendering concentric sound wave pulses expanding outward.
 *
 * Drawn purely with Compose Canvas primitives using [ChirpTheme.radioVisuals] tokens.
 * Automatically falls back to static concentric rings when [ChirpTheme.isReducedMotion] is active.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    color: Color = ChirpTheme.colors.primary
) {
    val ringCount = ChirpTheme.radioVisuals.ringCount
    val strokeWidth = ChirpTheme.radioVisuals.ringStrokeWidths
    val isReducedMotion = ChirpTheme.isReducedMotion
    val glowOpacity = ChirpTheme.opacity.glow

    val phase = if (isReducedMotion) {
        0.0f
    } else {
        val transition = rememberInfiniteTransition(label = "brandMarkTransition")
        val progress by transition.animateFloat(
            initialValue = 0.0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ChirpTheme.motion.durationExtraLong2,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "brandMarkProgress"
        )
        progress
    }

    Canvas(
        modifier = modifier.size(ChirpTheme.sizes.iconXl)
    ) {
        val center = this.center
        val maxRadius = size.minDimension / 2.0f
        val strokePx = strokeWidth.toPx()

        // Center acoustic core emitter dot
        drawCircle(
            color = color,
            radius = strokePx * 1.5f,
            center = center
        )

        // Concentric acoustic wavefront rings
        for (i in 0 until ringCount) {
            val ringProgress = if (isReducedMotion) {
                (i + 1).toFloat() / ringCount.toFloat()
            } else {
                ((phase + i.toFloat() / ringCount.toFloat()) % 1.0f)
            }

            val radius = maxRadius * ringProgress
            val alpha = (1.0f - ringProgress).coerceIn(0.0f, 1.0f) * glowOpacity

            if (radius > strokePx) {
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokePx)
                )
            }
        }
    }
}
