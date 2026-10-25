package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Full-surface subtle horizontal scanline texture mimicking a vintage CRT phosphor display.
 *
 * Controlled entirely by the [ChirpTheme.colors.scanline] token. If alpha is 0.0f, rendering
 * is completely bypassed with zero drawing overhead.
 */
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    color: Color = ChirpTheme.colors.scanline
) {
    if (color.alpha <= 0.001f) return

    val lineSpacing = ChirpTheme.spacing.xs
    val strokeWidth = ChirpTheme.strokes.hairline

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokePx = strokeWidth.toPx()
        val spacingPx = lineSpacing.toPx()
        var y = 0.0f

        while (y < size.height) {
            drawLine(
                color = color,
                start = Offset(0.0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokePx
            )
            y += spacingPx
        }
    }
}
