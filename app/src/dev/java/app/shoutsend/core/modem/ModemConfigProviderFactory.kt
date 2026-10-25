package app.shoutsend.core.modem

import app.shoutsend.core.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope

fun createModemConfigProvider(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope
): ModemConfigProvider = OverridableModemConfigProvider(settingsRepository, scope)
