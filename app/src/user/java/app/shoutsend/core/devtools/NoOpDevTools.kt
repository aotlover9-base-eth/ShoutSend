package app.shoutsend.core.devtools

import androidx.compose.runtime.Composable

class NoOpDevTools : DevTools {
    override val isAvailable: Boolean = false
    override val entryPoint: (@Composable () -> Unit)? = null
}
