package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard filled button wrapped from Material 3 with ChirpTheme tokens.
 */
@Composable
fun CdButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = ChirpTheme.colors.primary,
        contentColor = ChirpTheme.colors.onPrimary,
        disabledContainerColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle),
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ChirpTheme.spacing.lg,
        vertical = ChirpTheme.spacing.sm
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Tonal button wrapper using secondary/tertiary container colors.
 */
@Composable
fun CdTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = ChirpTheme.colors.secondaryContainer,
        contentColor = ChirpTheme.colors.onSecondaryContainer,
        disabledContainerColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle),
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ChirpTheme.spacing.lg,
        vertical = ChirpTheme.spacing.sm
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Outlined button wrapper using theme stroke and outline tokens.
 */
@Composable
fun CdOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = ChirpTheme.colors.primary,
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = if (enabled) ChirpTheme.colors.outline else ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.subtle)
    ),
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ChirpTheme.spacing.lg,
        vertical = ChirpTheme.spacing.sm
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Text button wrapper for low-emphasis actions.
 */
@Composable
fun CdTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ChirpTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.textButtonColors(
        contentColor = ChirpTheme.colors.primary,
        disabledContentColor = ChirpTheme.colors.onSurface.copy(alpha = ChirpTheme.opacity.disabled)
    ),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ChirpTheme.spacing.md,
        vertical = ChirpTheme.spacing.xs
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}
