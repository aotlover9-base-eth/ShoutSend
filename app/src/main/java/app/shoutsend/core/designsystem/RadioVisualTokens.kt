package app.shoutsend.core.designsystem

import androidx.compose.ui.unit.Dp

/**
 * Visual tuning and rendering tokens for acoustic meters and spectrogram waterfall display.
 * Purely functional parameters; no glows, blooms, or neon shaders.
 */
data class AcousticVisualTokens(
    val ringStrokeWidths: Dp,
    val ringCount: Int,
    val ringSpawnIntervalMs: Int,
    val waterfallRowsPerSecond: Int,
    val waterfallHistorySeconds: Int,
    val colormapResolution: Int,
    val meterSegmentCount: Int
)

typealias RadioVisualTokens = AcousticVisualTokens
