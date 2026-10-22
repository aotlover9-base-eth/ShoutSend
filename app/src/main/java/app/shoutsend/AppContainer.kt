package app.shoutsend

import android.content.Context
import app.shoutsend.core.designsystem.ThemeController
import app.shoutsend.core.designsystem.createTokenOverridesRepository
import app.shoutsend.core.devtools.DevTools
import app.shoutsend.core.devtools.createDevTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val devTools: DevTools = createDevTools(context)
    val tokenOverridesRepository = createTokenOverridesRepository(context, applicationScope)
    val themeController: ThemeController = ThemeController(
        repository = tokenOverridesRepository,
        scope = applicationScope
    )
}
