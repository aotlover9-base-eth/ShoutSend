package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Backward compatibility wrapper delegating to [ShoutIconButton].
 */
@Composable
fun CdIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) = ShoutIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutIconButtonVariant.STANDARD,
    colors = colors,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun CdFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) = ShoutIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutIconButtonVariant.FILLED,
    colors = colors,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun CdTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) = ShoutIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutIconButtonVariant.TONAL,
    colors = colors,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun CdOutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) = ShoutIconButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutIconButtonVariant.OUTLINED,
    colors = colors,
    interactionSource = interactionSource,
    content = content
)
