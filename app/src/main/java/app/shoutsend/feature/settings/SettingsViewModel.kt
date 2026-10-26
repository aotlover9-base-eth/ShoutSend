package app.shoutsend.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.capability.CapabilityProbe
import app.shoutsend.core.data.settings.Settings
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the Settings screen.
 */
data class SettingsUiState(
    val settings: Settings = Settings(),
    val capabilitySummary: String = "",
    val unstarredVaultCount: Int = 0,
    val showStealthWarningDialog: Boolean = false,
    val showPruneConfirmDialog: Boolean = false,
    val pendingMaxEntries: Int? = null,
    val pendingPruneCount: Int = 0,
    val showDeleteAllDataDialog: Boolean = false,
    val dataResetCompleted: Boolean = false
)

/**
 * ViewModel orchestrating global user configuration, vault history pruning,
 * audio power preferences, and data reset (Step 44).
 *
 * Rules:
 * - Single source of truth backed by DataStore [SettingsRepository] (Rule 15).
 * - Pruning removes oldest unstarred entries only; starred entries are never deleted.
 * - Near-ultrasound Stealth disclosure dialog required if not previously accepted.
 * - "Delete all app data" wipes the encrypted vault and restores all settings to default.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val capabilityProbe: CapabilityProbe? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            settings = settingsRepository.settings.value,
            capabilitySummary = capabilityProbe?.probe()?.toSummaryString() ?: "Standard Audio (48 kHz, Low Latency)"
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { latestSettings ->
                _uiState.update { it.copy(settings = latestSettings) }
            }
        }

        viewModelScope.launch {
            vaultRepository.observeItems().collect { items ->
                val unstarred = items.count { !it.isStarred }
                _uiState.update { it.copy(unstarredVaultCount = unstarred) }
            }
        }
    }

    /**
     * Updates default transmission carrier band.
     * Enforces near-ultrasound disclosure if switching to Stealth without prior acceptance.
     */
    fun setBand(band: Band) {
        if (band == Band.STEALTH && !_uiState.value.settings.stealthWarningAccepted) {
            _uiState.update { it.copy(showStealthWarningDialog = true) }
        } else {
            viewModelScope.launch {
                settingsRepository.setBand(band)
            }
        }
    }

    fun onAcceptStealthWarning() {
        _uiState.update { it.copy(showStealthWarningDialog = false) }
        viewModelScope.launch {
            settingsRepository.setStealthWarningAccepted(true)
            settingsRepository.setBand(Band.STEALTH)
        }
    }

    fun onDismissStealthWarning() {
        _uiState.update { it.copy(showStealthWarningDialog = false) }
    }

    fun setSpeed(speed: Speed) {
        viewModelScope.launch {
            settingsRepository.setSpeed(speed)
        }
    }

    fun setRepeats(repeats: Int) {
        viewModelScope.launch {
            settingsRepository.setRepeats(repeats)
        }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHistoryEnabled(enabled)
        }
    }

    /**
     * Changes maximum history retention limit.
     * If lowering the limit would delete older unstarred entries, prompts confirmation first.
     */
    fun requestHistoryMaxEntries(newLimit: Int) {
        val unstarred = _uiState.value.unstarredVaultCount
        if (newLimit < _uiState.value.settings.historyMaxEntries && unstarred > newLimit) {
            _uiState.update {
                it.copy(
                    showPruneConfirmDialog = true,
                    pendingMaxEntries = newLimit,
                    pendingPruneCount = unstarred - newLimit
                )
            }
        } else {
            viewModelScope.launch {
                settingsRepository.setHistoryMaxEntries(newLimit)
                vaultRepository.prune(newLimit)
            }
        }
    }

    fun confirmPrune() {
        val newLimit = _uiState.value.pendingMaxEntries ?: return
        viewModelScope.launch {
            settingsRepository.setHistoryMaxEntries(newLimit)
            vaultRepository.prune(newLimit)
            _uiState.update { it.copy(showPruneConfirmDialog = false, pendingMaxEntries = null) }
        }
    }

    fun dismissPruneDialog() {
        _uiState.update { it.copy(showPruneConfirmDialog = false, pendingMaxEntries = null) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEffectsEnabled(enabled)
        }
    }

    fun setKeepScreenOnDuringSessions(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOnDuringSessions(enabled)
        }
    }

    fun setBackgroundGraceSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setBackgroundGraceSeconds(seconds)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun requestDeleteAllData() {
        _uiState.update { it.copy(showDeleteAllDataDialog = true) }
    }

    fun dismissDeleteAllDataDialog() {
        _uiState.update { it.copy(showDeleteAllDataDialog = false) }
    }

    /**
     * Purges all vault items and resets DataStore settings to factory defaults.
     */
    fun confirmDeleteAllData() {
        viewModelScope.launch {
            vaultRepository.deleteAll()
            settingsRepository.reset()
            _uiState.update {
                it.copy(
                    showDeleteAllDataDialog = false,
                    dataResetCompleted = true
                )
            }
        }
    }

    /**
     * Resets onboarding completion flag to allow replaying the intro guide.
     */
    fun replayOnboarding(onNavigateToOnboarding: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(false)
            onNavigateToOnboarding()
        }
    }
}
