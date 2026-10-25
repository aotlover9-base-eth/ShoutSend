package app.shoutsend.core.modem

import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Production implementation of [ModemConfigProvider] for the user flavor.
 *
 * Combines user settings with immutable [ModemTuning.DEFAULT].
 * Never allows DSP parameter tampering or audio source overrides (Rule 15).
 */
class FixedModemConfigProvider(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : ModemConfigProvider {

    override val config: StateFlow<ModemConfig> = settingsRepository.settings
        .map { settings ->
            ModemConfig(
                band = settings.band,
                speed = settings.speed,
                repeats = settings.repeats,
                tuning = ModemTuning.DEFAULT,
                audioSourceOverride = null
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ModemConfig()
        )
}
