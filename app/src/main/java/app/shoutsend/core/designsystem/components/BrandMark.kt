package app.shoutsend.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import app.shoutsend.core.designsystem.ShoutSendTheme

/**
 * Animated acoustic emblem rendering concentric sound wave pulses expanding outward.
 * Drawn purely with Compose Canvas primitives. No neon glows or bloom shaders.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val ringCount = ShoutSendTheme.acousticVisuals.ringCount
    val strokeWidth = ShoutSendTheme.strokes.regular
    val isReducedMotion = ShoutSendTheme.isReducedMotion

    val phase = if (isReducedMotion) {
        0.0f
    } else {
        val transition = rememberInfiniteTransition(label = "brandMarkTransition")
        val progress by transition.animateFloat(
            initialValue = 0.0f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ShoutSendTheme.motion.durationExtraLong2,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "brandMarkProgress"
        )
        progress
    }

    val mutedOpacity = ShoutSendTheme.opacity.muted

    Canvas(
        modifier = modifier.size(ShoutSendTheme.sizes.iconXl)
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
            val alpha = (1.0f - ringProgress).coerceIn(0.0f, 1.0f) * mutedOpacity

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
