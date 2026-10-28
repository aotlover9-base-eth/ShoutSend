package app.shoutsend.core.designsystem.components

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CdSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { data ->
        ShoutSnackbarHost(hostState = hostState, modifier = modifier, snackbar = { data })
    }
) = ShoutSnackbarHost(
    hostState = hostState,
    modifier = modifier,
    snackbar = snackbar
)
