package app.shoutsend.core.modem

import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Developer implementation of [ModemConfigProvider] for the dev flavor.
 *
 * Supports live runtime DSP parameter tuning and hardware audio source overrides
 * from the developer panel (Step 50).
 * Excluded completely from the User APK by Gradle source sets.
 */
class OverridableModemConfigProvider(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : ModemConfigProvider {

    val customTuning = MutableStateFlow(ModemTuning.DEFAULT)
    val customAudioSourceOverride = MutableStateFlow<Int?>(null)

    override val config: StateFlow<ModemConfig> = combine(
        settingsRepository.settings,
        customTuning,
        customAudioSourceOverride
    ) { settings, tuning, sourceOverride ->
        ModemConfig(
            band = settings.band,
            speed = settings.speed,
            repeats = settings.repeats,
            tuning = tuning,
            audioSourceOverride = sourceOverride
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ModemConfig()
    )

    fun updateTuning(newTuning: ModemTuning) {
        customTuning.value = newTuning
    }

    fun updateAudioSourceOverride(source: Int?) {
        customAudioSourceOverride.value = source
    }

    fun resetTuning() {
        customTuning.value = ModemTuning.DEFAULT
        customAudioSourceOverride.value = null
    }
}
