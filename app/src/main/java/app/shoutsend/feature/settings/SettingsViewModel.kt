package app.shoutsend.feature.settings

import androidx.lifecycle.ViewModel
import app.shoutsend.core.data.settings.Settings
import app.shoutsend.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsRepository.settings
}
