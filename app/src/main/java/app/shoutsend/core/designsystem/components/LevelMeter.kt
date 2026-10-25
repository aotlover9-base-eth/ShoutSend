package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Horizontal acoustic energy level meter displaying continuous input/output dBFS with a peak-hold marker.
 *
 * Visual range: -70 dBFS to 0 dBFS.
 * Highlights in [ChirpTheme.colors.signalError] when audio level approaches analog clipping (-3 dBFS).
 */
@Composable
fun LevelMeter(
    levelDbfs: Float,
    modifier: Modifier = Modifier,
    peakDbfs: Float = levelDbfs,
    minDbfs: Float = -70.0f,
    maxDbfs: Float = 0.0f,
    showReadout: Boolean = true
) {
    val span = maxDbfs - minDbfs
    val normLevel = ((levelDbfs - minDbfs) / span).coerceIn(0.0f, 1.0f)
    val normPeak = ((peakDbfs - minDbfs) / span).coerceIn(0.0f, 1.0f)

    val isClipping = levelDbfs >= -3.0f
    val barColor = if (isClipping) ChirpTheme.colors.signalError else ChirpTheme.colors.signalActive

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChirpTheme.spacing.sm)
                .clip(ChirpTheme.shapes.extraSmall)
                .background(ChirpTheme.colors.surfaceContainerHighest)
        ) {
            // Active level fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(normLevel)
                    .fillMaxHeight()
                    .background(barColor)
            )

            // Peak hold marker line
            if (normPeak > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(normPeak)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .width(ChirpTheme.strokes.thick)
                            .fillMaxHeight()
                            .background(ChirpTheme.colors.noiseFloorLine)
                    )
                }
            }
        }

        if (showReadout) {
            Spacer(modifier = Modifier.height(ChirpTheme.spacing.xxs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${levelDbfs.toInt()} dBFS",
                    style = ChirpTheme.typography.radioReadoutSmall,
                    color = if (isClipping) ChirpTheme.colors.signalError else ChirpTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1.0f))
                if (isClipping) {
                    Text(
                        text = "CLIP",
                        style = ChirpTheme.typography.radioReadoutSmall,
                        color = ChirpTheme.colors.signalError
                    )
                }
            }
        }
    }
}
