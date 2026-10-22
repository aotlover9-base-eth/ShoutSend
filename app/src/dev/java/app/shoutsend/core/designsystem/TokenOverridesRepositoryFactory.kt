package app.shoutsend.core.designsystem

import android.content.Context
import kotlinx.coroutines.CoroutineScope

fun createTokenOverridesRepository(
    context: Context,
    scope: CoroutineScope
): TokenOverridesRepository = DevDataStoreTokenOverridesRepository(context, scope)
