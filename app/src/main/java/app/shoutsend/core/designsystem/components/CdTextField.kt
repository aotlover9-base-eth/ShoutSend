package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard filled text field wrapper using ChirpTheme tokens.
 */
@Composable
fun CdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = ChirpTheme.typography.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ChirpTheme.shapes.textField,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = ChirpTheme.colors.surfaceContainerHigh,
        unfocusedContainerColor = ChirpTheme.colors.surfaceContainer,
        disabledContainerColor = ChirpTheme.colors.surfaceContainerLowest,
        errorContainerColor = ChirpTheme.colors.errorContainer.copy(alpha = ChirpTheme.opacity.subtle),
        focusedTextColor = ChirpTheme.colors.onSurface,
        unfocusedTextColor = ChirpTheme.colors.onSurface,
        focusedIndicatorColor = ChirpTheme.colors.primary,
        unfocusedIndicatorColor = ChirpTheme.colors.outline,
        disabledIndicatorColor = ChirpTheme.colors.outlineVariant.copy(alpha = ChirpTheme.opacity.disabled),
        errorIndicatorColor = ChirpTheme.colors.error,
        focusedLabelColor = ChirpTheme.colors.primary,
        unfocusedLabelColor = ChirpTheme.colors.onSurfaceVariant,
        cursorColor = ChirpTheme.colors.primary
    )
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}

/**
 * Standard outlined text field wrapper using ChirpTheme tokens.
 */
@Composable
fun CdOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = ChirpTheme.typography.bodyLarge,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ChirpTheme.shapes.textField,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedBorderColor = ChirpTheme.colors.primary,
        unfocusedBorderColor = ChirpTheme.colors.outline,
        disabledBorderColor = ChirpTheme.colors.outlineVariant.copy(alpha = ChirpTheme.opacity.disabled),
        errorBorderColor = ChirpTheme.colors.error,
        focusedTextColor = ChirpTheme.colors.onSurface,
        unfocusedTextColor = ChirpTheme.colors.onSurface,
        focusedLabelColor = ChirpTheme.colors.primary,
        unfocusedLabelColor = ChirpTheme.colors.onSurfaceVariant,
        cursorColor = ChirpTheme.colors.primary
    )
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors
    )
}
