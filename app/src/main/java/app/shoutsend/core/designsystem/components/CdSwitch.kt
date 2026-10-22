package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard Switch wrapper using ChirpTheme tokens.
 */
@Composable
fun CdSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = ChirpTheme.colors.onPrimary,
        checkedTrackColor = ChirpTheme.colors.primary,
        uncheckedThumbColor = ChirpTheme.colors.outline,
        uncheckedTrackColor = ChirpTheme.colors.surfaceContainerHighest,
        uncheckedBorderColor = ChirpTheme.colors.outline,
        disabledCheckedThumbColor = ChirpTheme.colors.surface.copy(alpha = ChirpTheme.opacity.disabled),
        disabledCheckedTrackColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle),
        disabledUncheckedThumbColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled),
        disabledUncheckedTrackColor = ChirpTheme.colors.surfaceContainerLowest
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}
