package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Cyber radio card container featuring a neon perimeter stroke and a subtle luminous halo.
 *
 * Implements subtle glow without expensive blur shaders by drawing a multi-pass
 * translucent stroke along the card perimeter using [ChirpTheme.radioVisuals.glowRadius]
 * and [ChirpTheme.radioVisuals.glowAlpha].
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    shape: Shape = ChirpTheme.shapes.card,
    glowColor: Color = ChirpTheme.colors.glowPrimary,
    containerColor: Color = ChirpTheme.colors.surfaceContainer,
    contentColor: Color = ChirpTheme.colors.onSurface,
    borderColor: Color = ChirpTheme.colors.primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haloAlpha = ChirpTheme.radioVisuals.glowAlpha
    val haloWidth = ChirpTheme.radioVisuals.ringStrokeWidths

    val haloModifier = modifier.drawBehind {
        // Multi-pass subtle outer halo
        drawRoundRect(
            color = glowColor.copy(alpha = haloAlpha),
            style = Stroke(width = haloWidth.toPx() * 2f)
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = haloModifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            border = BorderStroke(ChirpTheme.strokes.thin, borderColor),
            content = content
        )
    } else {
        Card(
            modifier = haloModifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            border = BorderStroke(ChirpTheme.strokes.thin, borderColor),
            content = content
        )
    }
}
