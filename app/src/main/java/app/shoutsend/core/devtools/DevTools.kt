package app.shoutsend.core.devtools

import androidx.compose.runtime.Composable

/**
 * Minimal seam for developer tools.
 * Implemented as a no-op in the user flavor and as an active panel in the dev flavor.
 */
interface DevTools {
    val isAvailable: Boolean
    val entryPoint: (@Composable () -> Unit)?
}
