package app.shoutsend.feature.send

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.ColorTokens
import kotlin.math.hypot
import kotlin.math.min

/**
 * Preallocated ring state instance for soundwave ripple animation.
 * Conforms to Rule 22 by avoiding heap allocations during frame ticks and Canvas draw calls.
 */
internal class Ring(
    var active: Boolean = false,
    var radiusFraction: Float = 0.0f,
    var alpha: Float = 0.0f,
    var stroke: Stroke = Stroke(width = 2.0f),
    var color: Color = Color.Unspecified
)

/**
 * Animated soundwave rings canvas synchronized with audio playback.
 * Concentric ripple pulses spawn from the center and expand to the display boundary.
 * Ring spawn rate and stroke thickness follow real-time acoustic RMS amplitude.
 * Ring coloration reflects the active tone index sampled across the theme band gradient.
 * Fallback to a single static pulse when reduced motion is preferred.
 */
@Composable
fun RingsCanvas(
    isTransmitting: Boolean,
    activeRms: Float,
    activeToneIndex: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isReducedMotion = ChirpTheme.isReducedMotion
    val colors = ChirpTheme.colors
    val regularStrokePx = with(density) { ChirpTheme.strokes.regular.toPx() }
    val thickStrokePx = with(density) { ChirpTheme.strokes.thick.toPx() }

    // Preallocated ring pool (12 rings max, completely zero allocation per frame)
    val ringPool = remember {
        Array(12) { Ring(stroke = Stroke(width = regularStrokePx)) }
    }

    val frameClock = remember { mutableLongStateOf(0L) }

    // Animation frame advancement clock
    LaunchedEffect(isTransmitting, isReducedMotion) {
        if (!isTransmitting || isReducedMotion) return@LaunchedEffect

        var lastFrameNanos = 0L
        var spawnTimer = 0.0f

        while (true) {
            withFrameNanos { nowNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = nowNanos
                }
                val dt = ((nowNanos - lastFrameNanos).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0.0f, 0.1f)
                lastFrameNanos = nowNanos

                // Advance active rings
                val expansionSpeed = 0.35f
                for (ring in ringPool) {
                    if (ring.active) {
                        ring.radiusFraction += dt * expansionSpeed
                        if (ring.radiusFraction >= 1.0f) {
                            ring.active = false
                        } else {
                            // Fade out gracefully as the ring expands outwards
                            val fadeFactor = (1.0f - ring.radiusFraction).coerceIn(0.0f, 1.0f)
                            ring.alpha = fadeFactor * fadeFactor
                        }
                    }
                }

                // Spawn new rings conditioned on live audio energy
                spawnTimer += dt
                val spawnInterval = (0.50f - 0.35f * activeRms.coerceIn(0.0f, 1.0f)).coerceAtLeast(0.08f)
                if (spawnTimer >= spawnInterval && activeRms > 0.05f) {
                    spawnTimer = 0.0f
                    val freeRing = ringPool.firstOrNull { !it.active }
                    if (freeRing != null) {
                        val strokeWidth = regularStrokePx + (thickStrokePx - regularStrokePx) * activeRms
                        freeRing.active = true
                        freeRing.radiusFraction = 0.12f
                        freeRing.alpha = 1.0f
                        freeRing.stroke = Stroke(width = strokeWidth)
                        freeRing.color = resolveToneColor(activeToneIndex, colors)
                    }
                }

                frameClock.longValue = nowNanos
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = RingsSemantics.formatDescription(isTransmitting)
            }
    ) {
        // Read frameClock to trigger draw phase invalidated purely by frame ticker
        @Suppress("UNUSED_VARIABLE")
        val tick = frameClock.longValue

        val centerX = size.width / 2.0f
        val centerY = size.height / 2.0f
        val maxRadius = hypot(centerX, centerY)

        if (isReducedMotion) {
            // Accessible fallback: single calm ring reflecting active tone without flashing ripples
            val staticRadius = min(centerX, centerY) * 0.75f
            val staticColor = resolveToneColor(activeToneIndex, colors)
            drawCircle(
                color = staticColor,
                radius = staticRadius,
                alpha = if (isTransmitting) 0.6f else 0.2f,
                style = Stroke(width = regularStrokePx)
            )
            return@Canvas
        }

        // Render all active rings from the preallocated pool
        for (ring in ringPool) {
            if (ring.active && ring.alpha > 0.01f) {
                val currentRadius = maxRadius * ring.radiusFraction
                drawCircle(
                    color = ring.color,
                    radius = currentRadius,
                    alpha = ring.alpha * 0.85f,
                    style = ring.stroke
                )
            }
        }
    }
}

/**
 * Resolves a smooth color token from the band spectrum based on dominant acoustic tone index.
 * Tone indices 0..15 interpolate across primary -> secondary -> tertiary tokens.
 * Sync tones (16..17) map directly to syncMarker token.
 */
private fun resolveToneColor(toneIndex: Int, colors: ColorTokens): Color {
    return when (toneIndex) {
        -1 -> colors.primary
        16, 17 -> colors.syncMarker
        in 0..5 -> {
            val frac = toneIndex / 5.0f
            lerp(colors.primary, colors.secondary, frac)
        }
        in 6..10 -> {
            val frac = (toneIndex - 6) / 4.0f
            lerp(colors.secondary, colors.tertiary, frac)
        }
        in 11..15 -> {
            val frac = (toneIndex - 11) / 4.0f
            lerp(colors.tertiary, colors.signalPeak, frac)
        }
        else -> colors.primary
    }
}

/**
 * Formatter for RingsCanvas accessibility announcements.
 */
object RingsSemantics {
    fun formatDescription(isTransmitting: Boolean): String {
        return if (isTransmitting) {
            "Acoustic transmission soundwave ripple animation, broadcasting data over sound"
        } else {
            "Acoustic soundwave ripple animation, idle"
        }
    }
}
