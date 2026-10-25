package app.shoutsend.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.shoutsend.core.audio.permissions.MicPermissionRequestedProvider
import app.shoutsend.core.designsystem.ThemeController
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

/**
 * Single typed contract for accessing and modifying all user preferences.
 */
interface SettingsRepository {
    val settings: StateFlow<Settings>

    fun observe(): Flow<Settings> = settings

    suspend fun update(transform: (Settings) -> Settings)

    suspend fun reset()

    suspend fun setBand(band: Band)
    suspend fun setSpeed(speed: Speed)
    suspend fun setRepeats(repeats: Int)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setSoundEffectsEnabled(enabled: Boolean)
    suspend fun setSecureModeDefault(enabled: Boolean)
    suspend fun setHistoryEnabled(enabled: Boolean)
    suspend fun setHistoryMaxEntries(maxEntries: Int)
    suspend fun setKeepScreenOnDuringSessions(enabled: Boolean)
    suspend fun setLastCalibration(calibrationJson: String?)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setMicPermissionRequested(requested: Boolean)
    suspend fun setStealthWarningAccepted(accepted: Boolean)
    suspend fun setBackgroundGraceSeconds(seconds: Int)
    suspend fun setLastTab(tab: String)
}

/**
 * Production implementation of [SettingsRepository] backed by Jetpack Preferences DataStore.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope
) : SettingsRepository {

    override val settings: StateFlow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> mapPreferencesToSettings(preferences) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = Settings()
        )

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { preferences ->
            val current = mapPreferencesToSettings(preferences)
            val updated = transform(current)
            writeSettingsToPreferences(updated, preferences)
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun setBand(band: Band) = update { it.copy(band = band) }
    override suspend fun setSpeed(speed: Speed) = update { it.copy(speed = speed) }
    override suspend fun setRepeats(repeats: Int) = update {
        it.copy(repeats = repeats.coerceIn(Settings.REPEATS_MIN, Settings.REPEATS_MAX))
    }
    override suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }
    override suspend fun setHapticsEnabled(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    override suspend fun setSoundEffectsEnabled(enabled: Boolean) = update { it.copy(soundEffectsEnabled = enabled) }
    override suspend fun setSecureModeDefault(enabled: Boolean) = update { it.copy(secureModeDefault = enabled) }
    override suspend fun setHistoryEnabled(enabled: Boolean) = update { it.copy(historyEnabled = enabled) }
    override suspend fun setHistoryMaxEntries(maxEntries: Int) = update {
        it.copy(historyMaxEntries = maxEntries.coerceIn(Settings.HISTORY_MIN, Settings.HISTORY_MAX))
    }
    override suspend fun setKeepScreenOnDuringSessions(enabled: Boolean) = update { it.copy(keepScreenOnDuringSessions = enabled) }
    override suspend fun setLastCalibration(calibrationJson: String?) = update { it.copy(lastCalibration = calibrationJson) }
    override suspend fun setOnboardingCompleted(completed: Boolean) = update { it.copy(onboardingCompleted = completed) }
    override suspend fun setMicPermissionRequested(requested: Boolean) = update { it.copy(micPermissionRequested = requested) }
    override suspend fun setStealthWarningAccepted(accepted: Boolean) = update { it.copy(stealthWarningAccepted = accepted) }
    override suspend fun setBackgroundGraceSeconds(seconds: Int) = update {
        it.copy(backgroundGraceSeconds = seconds.coerceIn(Settings.GRACE_MIN, Settings.GRACE_MAX))
    }
    override suspend fun setLastTab(tab: String) = update { it.copy(lastTab = tab) }

    private fun mapPreferencesToSettings(prefs: Preferences): Settings {
        val band = try {
            prefs[KEY_BAND]?.let { Band.valueOf(it) } ?: Settings.DEFAULT_BAND
        } catch (_: Exception) {
            Settings.DEFAULT_BAND
        }

        val speed = try {
            prefs[KEY_SPEED]?.let { Speed.valueOf(it) } ?: Settings.DEFAULT_SPEED
        } catch (_: Exception) {
            Settings.DEFAULT_SPEED
        }

        val repeats = (prefs[KEY_REPEATS] ?: Settings.DEFAULT_REPEATS)
            .coerceIn(Settings.REPEATS_MIN, Settings.REPEATS_MAX)

        val themeMode = try {
            prefs[KEY_THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: Settings.DEFAULT_THEME_MODE
        } catch (_: Exception) {
            Settings.DEFAULT_THEME_MODE
        }

        val dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: Settings.DEFAULT_DYNAMIC_COLOR
        val hapticsEnabled = prefs[KEY_HAPTICS_ENABLED] ?: Settings.DEFAULT_HAPTICS_ENABLED
        val soundEffectsEnabled = prefs[KEY_SOUND_EFFECTS_ENABLED] ?: Settings.DEFAULT_SOUND_EFFECTS_ENABLED
        val secureModeDefault = prefs[KEY_SECURE_MODE_DEFAULT] ?: Settings.DEFAULT_SECURE_MODE_DEFAULT
        val historyEnabled = prefs[KEY_HISTORY_ENABLED] ?: Settings.DEFAULT_HISTORY_ENABLED
        val historyMaxEntries = (prefs[KEY_HISTORY_MAX_ENTRIES] ?: Settings.DEFAULT_HISTORY_MAX_ENTRIES)
            .coerceIn(Settings.HISTORY_MIN, Settings.HISTORY_MAX)
        val keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: Settings.DEFAULT_KEEP_SCREEN_ON
        val lastCalibration = prefs[KEY_LAST_CALIBRATION]
        val onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: Settings.DEFAULT_ONBOARDING_COMPLETED
        val micPermissionRequested = prefs[KEY_MIC_PERMISSION_REQUESTED] ?: Settings.DEFAULT_MIC_PERMISSION_REQUESTED
        val stealthWarningAccepted = prefs[KEY_STEALTH_WARNING_ACCEPTED] ?: Settings.DEFAULT_STEALTH_WARNING_ACCEPTED
        val backgroundGraceSeconds = (prefs[KEY_BACKGROUND_GRACE_SECONDS] ?: Settings.DEFAULT_BACKGROUND_GRACE_SECONDS)
            .coerceIn(Settings.GRACE_MIN, Settings.GRACE_MAX)

        return Settings(
            band = band,
            speed = speed,
            repeats = repeats,
            themeMode = themeMode,
            dynamicColor = dynamicColor,
            hapticsEnabled = hapticsEnabled,
            soundEffectsEnabled = soundEffectsEnabled,
            secureModeDefault = secureModeDefault,
            historyEnabled = historyEnabled,
            historyMaxEntries = historyMaxEntries,
            keepScreenOnDuringSessions = keepScreenOn,
            lastCalibration = lastCalibration,
            onboardingCompleted = onboardingCompleted,
            micPermissionRequested = micPermissionRequested,
            stealthWarningAccepted = stealthWarningAccepted,
            backgroundGraceSeconds = backgroundGraceSeconds,
            lastTab = prefs[KEY_LAST_TAB] ?: Settings.DEFAULT_LAST_TAB
        )
    }

    private fun writeSettingsToPreferences(settings: Settings, preferences: androidx.datastore.preferences.core.MutablePreferences) {
        preferences[KEY_BAND] = settings.band.name
        preferences[KEY_SPEED] = settings.speed.name
        preferences[KEY_REPEATS] = settings.repeats
        preferences[KEY_THEME_MODE] = settings.themeMode.name
        preferences[KEY_DYNAMIC_COLOR] = settings.dynamicColor
        preferences[KEY_HAPTICS_ENABLED] = settings.hapticsEnabled
        preferences[KEY_SOUND_EFFECTS_ENABLED] = settings.soundEffectsEnabled
        preferences[KEY_SECURE_MODE_DEFAULT] = settings.secureModeDefault
        preferences[KEY_HISTORY_ENABLED] = settings.historyEnabled
        preferences[KEY_HISTORY_MAX_ENTRIES] = settings.historyMaxEntries
        preferences[KEY_KEEP_SCREEN_ON] = settings.keepScreenOnDuringSessions
        if (settings.lastCalibration != null) {
            preferences[KEY_LAST_CALIBRATION] = settings.lastCalibration
        } else {
            preferences.remove(KEY_LAST_CALIBRATION)
        }
        preferences[KEY_ONBOARDING_COMPLETED] = settings.onboardingCompleted
        preferences[KEY_MIC_PERMISSION_REQUESTED] = settings.micPermissionRequested
        preferences[KEY_STEALTH_WARNING_ACCEPTED] = settings.stealthWarningAccepted
        preferences[KEY_BACKGROUND_GRACE_SECONDS] = settings.backgroundGraceSeconds
        preferences[KEY_LAST_TAB] = settings.lastTab
    }

    companion object {
        val KEY_BAND = stringPreferencesKey("band")
        val KEY_SPEED = stringPreferencesKey("speed")
        val KEY_REPEATS = intPreferencesKey("repeats")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val KEY_SECURE_MODE_DEFAULT = booleanPreferencesKey("secure_mode_default")
        val KEY_HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val KEY_HISTORY_MAX_ENTRIES = intPreferencesKey("history_max_entries")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_during_sessions")
        val KEY_LAST_CALIBRATION = stringPreferencesKey("last_calibration")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_MIC_PERMISSION_REQUESTED = booleanPreferencesKey("mic_permission_requested")
        val KEY_STEALTH_WARNING_ACCEPTED = booleanPreferencesKey("stealth_warning_accepted")
        val KEY_BACKGROUND_GRACE_SECONDS = intPreferencesKey("background_grace_seconds")
        val KEY_LAST_TAB = stringPreferencesKey("last_tab")
    }
}

/**
 * In-memory test fake implementation of [SettingsRepository].
 */
class FakeSettingsRepository(initialSettings: Settings = Settings()) : SettingsRepository {
    private val _settings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<Settings> = _settings.asStateFlow()

    override suspend fun update(transform: (Settings) -> Settings) {
        val updated = transform(_settings.value)
        _settings.value = updated
    }

    override suspend fun reset() {
        _settings.value = Settings()
    }

    override suspend fun setBand(band: Band) = update { it.copy(band = band) }
    override suspend fun setSpeed(speed: Speed) = update { it.copy(speed = speed) }
    override suspend fun setRepeats(repeats: Int) = update {
        it.copy(repeats = repeats.coerceIn(Settings.REPEATS_MIN, Settings.REPEATS_MAX))
    }
    override suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }
    override suspend fun setHapticsEnabled(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    override suspend fun setSoundEffectsEnabled(enabled: Boolean) = update { it.copy(soundEffectsEnabled = enabled) }
    override suspend fun setSecureModeDefault(enabled: Boolean) = update { it.copy(secureModeDefault = enabled) }
    override suspend fun setHistoryEnabled(enabled: Boolean) = update { it.copy(historyEnabled = enabled) }
    override suspend fun setHistoryMaxEntries(maxEntries: Int) = update {
        it.copy(historyMaxEntries = maxEntries.coerceIn(Settings.HISTORY_MIN, Settings.HISTORY_MAX))
    }
    override suspend fun setKeepScreenOnDuringSessions(enabled: Boolean) = update { it.copy(keepScreenOnDuringSessions = enabled) }
    override suspend fun setLastCalibration(calibrationJson: String?) = update { it.copy(lastCalibration = calibrationJson) }
    override suspend fun setOnboardingCompleted(completed: Boolean) = update { it.copy(onboardingCompleted = completed) }
    override suspend fun setMicPermissionRequested(requested: Boolean) = update { it.copy(micPermissionRequested = requested) }
    override suspend fun setStealthWarningAccepted(accepted: Boolean) = update { it.copy(stealthWarningAccepted = accepted) }
    override suspend fun setBackgroundGraceSeconds(seconds: Int) = update {
        it.copy(backgroundGraceSeconds = seconds.coerceIn(Settings.GRACE_MIN, Settings.GRACE_MAX))
    }
    override suspend fun setLastTab(tab: String) = update { it.copy(lastTab = tab) }
}

/**
 * Adapter exposing [MicPermissionRequestedProvider] contract backed by [SettingsRepository].
 */
class DataStoreMicPermissionRequestedProvider(
    private val repository: SettingsRepository
) : MicPermissionRequestedProvider {
    override suspend fun hasRequestedBefore(): Boolean = repository.settings.value.micPermissionRequested
    override suspend fun setRequestedBefore(requested: Boolean) = repository.setMicPermissionRequested(requested)
}

/**
 * Connects theme settings from [SettingsRepository] directly to [ThemeController], ensuring
 * instant reactivity across dark/light and dynamic color switches in User APK (Rule 11).
 */
fun SettingsRepository.bindToThemeController(
    controller: ThemeController,
    scope: CoroutineScope
): Job {
    return settings
        .map { it.themeMode to it.dynamicColor }
        .distinctUntilChanged()
        .onEach { (mode, dynamicColor) ->
            controller.updateThemeMode(mode)
            controller.updateDynamicColor(dynamicColor)
        }
        .launchIn(scope)
}
