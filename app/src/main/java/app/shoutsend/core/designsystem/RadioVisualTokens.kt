package app.shoutsend.core.designsystem

import androidx.compose.ui.unit.Dp

/**
 * Visual tuning and rendering tokens for acoustic radio meters, waterfall display,
 * and radar glyph animations.
 */
data class RadioVisualTokens(
    val glowRadius: Dp,
    val glowAlpha: Float,
    val ringStrokeWidths: Dp,
    val ringCount: Int,
    val ringSpawnIntervalMs: Int,
    val waterfallRowsPerSecond: Int,
    val waterfallHistorySeconds: Int,
    val colormapResolution: Int,
    val meterSegmentCount: Int
)
