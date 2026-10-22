package app.shoutsend.core.devtools

import android.content.Context
import app.shoutsend.core.designsystem.ThemeController

fun createDevTools(
    context: Context,
    themeController: ThemeController
): DevTools = NoOpDevTools()
