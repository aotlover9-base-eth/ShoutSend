package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard icon button wrapper using ChirpTheme tokens.
 */
@Composable
fun CdIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(
        contentColor = ChirpTheme.colors.onSurfaceVariant,
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Filled icon button wrapper for high-emphasis actions.
 */
@Composable
fun CdFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = ChirpTheme.colors.primary,
        contentColor = ChirpTheme.colors.onPrimary,
        disabledContainerColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle),
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Tonal icon button wrapper.
 */
@Composable
fun CdTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = ChirpTheme.colors.secondaryContainer,
        contentColor = ChirpTheme.colors.onSecondaryContainer,
        disabledContainerColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle),
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Outlined icon button wrapper.
 */
@Composable
fun CdOutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(
        contentColor = ChirpTheme.colors.onSurfaceVariant,
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    border: BorderStroke? = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = if (enabled) ChirpTheme.colors.outline else ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle)
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )
}
