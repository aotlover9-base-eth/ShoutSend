package app.shoutsend.core.devtools

import android.content.Context

fun createDevTools(context: Context): DevTools = DevToolsImpl(context)
