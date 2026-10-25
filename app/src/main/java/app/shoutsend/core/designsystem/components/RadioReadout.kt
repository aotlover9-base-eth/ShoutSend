package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.shoutsend.core.designsystem.ChirpTheme

enum class ReadoutScale {
    LARGE,
    MEDIUM,
    SMALL
}

/**
 * Cyber radio readout displaying numeric metrics (frequencies, SNR, byte offsets, durations)
 * formatted in strict monospace typography.
 */
@Composable
fun RadioReadout(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    scale: ReadoutScale = ReadoutScale.MEDIUM,
    color: Color = ChirpTheme.colors.primary,
    labelColor: Color = ChirpTheme.colors.onSurfaceVariant
) {
    val textStyle = when (scale) {
        ReadoutScale.LARGE -> ChirpTheme.typography.radioReadoutLarge
        ReadoutScale.MEDIUM -> ChirpTheme.typography.radioReadoutMedium
        ReadoutScale.SMALL -> ChirpTheme.typography.radioReadoutSmall
    }

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label.uppercase(),
                style = ChirpTheme.typography.labelSmall,
                color = labelColor
            )
            Spacer(modifier = Modifier.height(ChirpTheme.spacing.xxs))
        }

        Text(
            text = value,
            style = textStyle,
            color = color
        )
    }
}
