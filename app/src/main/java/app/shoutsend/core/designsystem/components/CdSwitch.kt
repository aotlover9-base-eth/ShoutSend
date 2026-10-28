package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Backward compatibility wrapper delegating to [ShoutSwitch].
 */
@Composable
fun CdSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) = ShoutSwitch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    thumbContent = thumbContent,
    enabled = enabled,
    colors = colors,
    interactionSource = interactionSource
)
