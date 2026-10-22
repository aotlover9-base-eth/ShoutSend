package app.shoutsend

import android.content.Context
import app.shoutsend.core.devtools.DevTools
import app.shoutsend.core.devtools.createDevTools

class AppContainer(val context: Context) {
    val devTools: DevTools = createDevTools(context)
}
