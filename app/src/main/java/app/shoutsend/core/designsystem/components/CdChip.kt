package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard AssistChip wrapper using ChirpTheme tokens.
 */
@Composable
fun CdAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ChirpTheme.shapes.chip,
    colors: ChipColors = AssistChipDefaults.assistChipColors(
        containerColor = ChirpTheme.colors.surfaceContainerLow,
        labelColor = ChirpTheme.colors.onSurface,
        leadingIconContentColor = ChirpTheme.colors.primary,
        trailingIconContentColor = ChirpTheme.colors.onSurfaceVariant
    ),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = ChirpTheme.colors.outlineVariant
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    AssistChip(
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}

/**
 * Standard FilterChip wrapper using ChirpTheme tokens.
 */
@Composable
fun CdFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ChirpTheme.shapes.chip,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = ChirpTheme.colors.surfaceContainerLow,
        labelColor = ChirpTheme.colors.onSurfaceVariant,
        selectedContainerColor = ChirpTheme.colors.secondaryContainer,
        selectedLabelColor = ChirpTheme.colors.onSecondaryContainer,
        selectedLeadingIconColor = ChirpTheme.colors.onSecondaryContainer
    ),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = if (selected) ChirpTheme.colors.secondary else ChirpTheme.colors.outlineVariant
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}

/**
 * Standard InputChip wrapper using ChirpTheme tokens.
 */
@Composable
fun CdInputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ChirpTheme.shapes.chip,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(
        containerColor = ChirpTheme.colors.surfaceContainerLow,
        labelColor = ChirpTheme.colors.onSurface,
        selectedContainerColor = ChirpTheme.colors.primaryContainer,
        selectedLabelColor = ChirpTheme.colors.onPrimaryContainer
    ),
    elevation: SelectableChipElevation? = InputChipDefaults.inputChipElevation(),
    border: BorderStroke? = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = ChirpTheme.colors.outlineVariant
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    InputChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}

/**
 * Standard SuggestionChip wrapper using ChirpTheme tokens.
 */
@Composable
fun CdSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = ChirpTheme.shapes.chip,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(
        containerColor = ChirpTheme.colors.surfaceContainerLow,
        labelColor = ChirpTheme.colors.onSurface
    ),
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    border: BorderStroke? = BorderStroke(
        width = ChirpTheme.strokes.thin,
        color = ChirpTheme.colors.outlineVariant
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    SuggestionChip(
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource
    )
}
