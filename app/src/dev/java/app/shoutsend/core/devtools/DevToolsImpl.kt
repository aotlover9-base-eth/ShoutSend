package app.shoutsend.core.devtools

import android.content.Context
import androidx.compose.runtime.Composable

class DevToolsImpl(val context: Context) : DevTools {
    override val isAvailable: Boolean = true
    override val entryPoint: (@Composable () -> Unit)? = null
}
