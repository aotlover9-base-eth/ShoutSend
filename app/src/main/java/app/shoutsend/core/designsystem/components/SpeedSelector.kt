package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.modem.profile.Speed

/**
 * 3-way preset selector for transmission speed ([Speed.ROBUST], [Speed.NORMAL], [Speed.FAST])
 * displaying duration estimates based on simulation benchmarks.
 */
@Composable
fun SpeedSelector(
    selectedSpeed: Speed,
    onSpeedSelected: (Speed) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ChirpTheme.shapes.small)
            .background(ChirpTheme.colors.surfaceContainer)
            .border(
                width = ChirpTheme.strokes.thin,
                color = ChirpTheme.colors.outlineVariant,
                shape = ChirpTheme.shapes.small
            )
            .padding(ChirpTheme.spacing.xxs)
    ) {
        val speeds = listOf(
            Triple(Speed.ROBUST, "Robust", "~16.5 s"),
            Triple(Speed.NORMAL, "Normal", "~7.2 s"),
            Triple(Speed.FAST, "Fast", "~3.8 s")
        )

        for ((speed, title, estDuration) in speeds) {
            val isSelected = speed == selectedSpeed
            val containerColor = if (isSelected) ChirpTheme.colors.primaryContainer else ChirpTheme.colors.surfaceContainer
            val contentColor = if (isSelected) ChirpTheme.colors.onPrimaryContainer else ChirpTheme.colors.onSurfaceVariant
            val subColor = if (isSelected) ChirpTheme.colors.primary else ChirpTheme.colors.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(ChirpTheme.shapes.small)
                    .background(containerColor)
                    .clickable { onSpeedSelected(speed) }
                    .padding(vertical = ChirpTheme.spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = ChirpTheme.typography.labelMedium,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(ChirpTheme.spacing.xxs))
                    Text(
                        text = estDuration,
                        style = ChirpTheme.typography.radioReadoutSmall,
                        color = subColor
                    )
                }
            }
        }
    }
}
