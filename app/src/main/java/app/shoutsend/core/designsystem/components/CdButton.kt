package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ShoutSendTheme

/**
 * Backward compatibility wrapper delegating to [ShoutButton].
 */
@Composable
fun CdButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ShoutSendTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ShoutSendTheme.spacing.lg,
        vertical = ShoutSendTheme.spacing.sm
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.FILLED,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun CdTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ShoutSendTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ShoutSendTheme.spacing.lg,
        vertical = ShoutSendTheme.spacing.sm
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.TONAL,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun CdOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ShoutSendTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ShoutSendTheme.spacing.lg,
        vertical = ShoutSendTheme.spacing.sm
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.OUTLINED,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content
)

@Composable
fun CdTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ShoutSendTheme.shapes.button,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = ShoutSendTheme.spacing.md,
        vertical = ShoutSendTheme.spacing.xs
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.TEXT,
    shape = shape,
    colors = colors,
    elevation = elevation,
    border = border,
    contentPadding = contentPadding,
    interactionSource = interactionSource,
    content = content
)
