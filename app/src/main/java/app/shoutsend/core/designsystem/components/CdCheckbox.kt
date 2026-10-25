package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard Checkbox wrapper using ChirpTheme tokens.
 */
@Composable
fun CdCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = ChirpTheme.colors.primary,
        uncheckedColor = ChirpTheme.colors.outline,
        checkmarkColor = ChirpTheme.colors.onPrimary,
        disabledCheckedColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled),
        disabledUncheckedColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle),
        disabledIndeterminateColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}
