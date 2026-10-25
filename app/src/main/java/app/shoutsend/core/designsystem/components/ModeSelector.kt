package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.modem.profile.Band

/**
 * Segmented toggle control allowing users to switch between the Retro (Audible 1.5–3.5 kHz)
 * and Stealth (Near-Ultrasound 17.6–19.8 kHz) acoustic bands.
 */
@Composable
fun ModeSelector(
    selectedBand: Band,
    onBandSelected: (Band) -> Unit,
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
        val bands = listOf(
            Band.RETRO to "Retro (Audible)",
            Band.STEALTH to "Stealth (Ultrasound)"
        )

        for ((band, label) in bands) {
            val isSelected = band == selectedBand
            val containerColor = if (isSelected) ChirpTheme.colors.primaryContainer else ChirpTheme.colors.surfaceContainer
            val contentColor = if (isSelected) ChirpTheme.colors.onPrimaryContainer else ChirpTheme.colors.onSurfaceVariant

            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .clip(ChirpTheme.shapes.small)
                    .background(containerColor)
                    .clickable { onBandSelected(band) }
                    .padding(vertical = ChirpTheme.spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = ChirpTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}
