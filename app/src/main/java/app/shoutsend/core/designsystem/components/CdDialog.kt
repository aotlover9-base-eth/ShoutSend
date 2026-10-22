package app.shoutsend.core.designsystem.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard AlertDialog wrapper using ChirpTheme tokens.
 */
@Composable
fun CdAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = ChirpTheme.shapes.dialog,
    containerColor: Color = ChirpTheme.colors.surfaceContainerHigh,
    iconContentColor: Color = ChirpTheme.colors.primary,
    titleContentColor: Color = ChirpTheme.colors.onSurface,
    textContentColor: Color = ChirpTheme.colors.onSurfaceVariant,
    tonalElevation: Dp = ChirpTheme.elevation.level3,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}
