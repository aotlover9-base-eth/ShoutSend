package app.shoutsend.core.designsystem.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Standard SnackbarHost wrapper using ChirpTheme tokens.
 */
@Composable
fun CdSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { CdSnackbar(it) }
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = snackbar
    )
}

/**
 * Standard Snackbar wrapper using ChirpTheme inverse tokens.
 */
@Composable
fun CdSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = ChirpTheme.shapes.extraSmall,
    containerColor: Color = ChirpTheme.colors.inverseSurface,
    contentColor: Color = ChirpTheme.colors.inverseOnSurface,
    actionColor: Color = ChirpTheme.colors.inversePrimary,
    actionContentColor: Color = ChirpTheme.colors.inversePrimary,
    dismissActionContentColor: Color = ChirpTheme.colors.inverseOnSurface
) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        actionColor = actionColor,
        actionContentColor = actionContentColor,
        dismissActionContentColor = dismissActionContentColor
    )
}
