package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.shoutsend.core.designsystem.ShoutSendTheme

enum class ShoutIconButtonVariant {
    STANDARD,
    FILLED,
    TONAL,
    OUTLINED
}

@Composable
fun ShoutIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ShoutIconButtonVariant = ShoutIconButtonVariant.STANDARD,
    colors: IconButtonColors? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val sizeModifier = modifier.defaultMinSize(
        minWidth = ShoutSendTheme.sizes.touchTargetMin,
        minHeight = ShoutSendTheme.sizes.touchTargetMin
    )

    when (variant) {
        ShoutIconButtonVariant.STANDARD -> {
            IconButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.iconButtonColors(),
                interactionSource = interactionSource,
                content = content
            )
        }
        ShoutIconButtonVariant.FILLED -> {
            FilledIconButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledIconButtonColors(),
                interactionSource = interactionSource,
                content = content
            )
        }
        ShoutIconButtonVariant.TONAL -> {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledTonalIconButtonColors(),
                interactionSource = interactionSource,
                content = content
            )
        }
        ShoutIconButtonVariant.OUTLINED -> {
            OutlinedIconButton(
                onClick = onClick,
                modifier = sizeModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.outlinedIconButtonColors(),
                interactionSource = interactionSource,
                content = content
            )
        }
    }
}
