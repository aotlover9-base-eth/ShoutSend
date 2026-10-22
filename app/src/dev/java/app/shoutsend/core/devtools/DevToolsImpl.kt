package app.shoutsend.core.devtools

import android.content.Context
import androidx.compose.runtime.Composable
import app.shoutsend.core.designsystem.ThemeController

class DevToolsImpl(
    val context: Context,
    val themeController: ThemeController
) : DevTools {
    override val isAvailable: Boolean = true
    override val entryPoint: @Composable () -> Unit = {
        FloatingDevHandle(themeController = themeController)
    }
}
