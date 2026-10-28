package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
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
import app.shoutsend.core.designsystem.ShoutSendTheme

enum class ShoutButtonVariant {
    FILLED,
    TONAL,
    OUTLINED,
    TEXT
}

enum class ShoutButtonSize {
    S,
    M,
    L,
    XL
}

@Composable
fun ShoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ShoutButtonVariant = ShoutButtonVariant.FILLED,
    size: ShoutButtonSize = ShoutButtonSize.M,
    shape: Shape = ShoutSendTheme.shapes.button,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val minHeight = when (size) {
        ShoutButtonSize.S -> ShoutSendTheme.sizes.buttonHeight * 0.75f
        ShoutButtonSize.M -> ShoutSendTheme.sizes.buttonHeight
        ShoutButtonSize.L -> ShoutSendTheme.sizes.buttonHeight * 1.15f
        ShoutButtonSize.XL -> ShoutSendTheme.sizes.buttonHeight * 1.35f
    }

    val padding = contentPadding ?: when (size) {
        ShoutButtonSize.S -> PaddingValues(horizontal = ShoutSendTheme.spacing.md, vertical = ShoutSendTheme.spacing.xs)
        ShoutButtonSize.M -> PaddingValues(horizontal = ShoutSendTheme.spacing.lg, vertical = ShoutSendTheme.spacing.sm)
        ShoutButtonSize.L -> PaddingValues(horizontal = ShoutSendTheme.spacing.xl, vertical = ShoutSendTheme.spacing.md)
        ShoutButtonSize.XL -> PaddingValues(horizontal = ShoutSendTheme.spacing.xxl, vertical = ShoutSendTheme.spacing.lg)
    }

    val sizeModifier = modifier.defaultMinSize(minHeight = minHeight)

    when (variant) {
        ShoutButtonVariant.FILLED -> {
            Button(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.buttonColors(),
                elevation = elevation,
                border = border,
                contentPadding = padding,
                interactionSource = interactionSource,
                content = content
            )
        }
        ShoutButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.filledTonalButtonColors(),
                elevation = elevation,
                border = border,
                contentPadding = padding,
                interactionSource = interactionSource,
                content = content
            )
        }
        ShoutButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.outlinedButtonColors(),
                elevation = elevation,
                border = border ?: ButtonDefaults.outlinedButtonBorder(enabled),
                contentPadding = padding,
                interactionSource = interactionSource,
                content = content
            )
        }
        ShoutButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.textButtonColors(),
                elevation = elevation,
                border = border,
                contentPadding = padding,
                interactionSource = interactionSource,
                content = content
            )
        }
    }
}

@Composable
fun ShoutFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ShoutButtonSize = ShoutButtonSize.M,
    shape: Shape = ShoutSendTheme.shapes.button,
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.FILLED,
    size = size,
    shape = shape,
    content = content
)

@Composable
fun ShoutTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ShoutButtonSize = ShoutButtonSize.M,
    shape: Shape = ShoutSendTheme.shapes.button,
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.TONAL,
    size = size,
    shape = shape,
    content = content
)

@Composable
fun ShoutOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ShoutButtonSize = ShoutButtonSize.M,
    shape: Shape = ShoutSendTheme.shapes.button,
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.OUTLINED,
    size = size,
    shape = shape,
    content = content
)

@Composable
fun ShoutTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: ShoutButtonSize = ShoutButtonSize.M,
    shape: Shape = ShoutSendTheme.shapes.button,
    content: @Composable RowScope.() -> Unit
) = ShoutButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    variant = ShoutButtonVariant.TEXT,
    size = size,
    shape = shape,
    content = content
)
