package app.shoutsend.core.modem

import app.shoutsend.core.data.settings.Settings
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FixedModemConfigProviderTest {

    private class FakeSettingsRepository(
        initialSettings: Settings = Settings()
    ) : SettingsRepository {
        private val _settings = MutableStateFlow(initialSettings)
        override val settings: StateFlow<Settings> = _settings.asStateFlow()

        override suspend fun update(transform: (Settings) -> Settings) {
            _settings.value = transform(_settings.value)
        }

        override suspend fun reset() {
            _settings.value = Settings()
        }

        override suspend fun setBand(band: Band) {
            update { it.copy(band = band) }
        }

        override suspend fun setSpeed(speed: Speed) {
            update { it.copy(speed = speed) }
        }

        override suspend fun setRepeats(repeats: Int) {
            update { it.copy(repeats = repeats) }
        }

        override suspend fun setThemeMode(mode: ThemeMode) {}
        override suspend fun setDynamicColor(enabled: Boolean) {}
        override suspend fun setHapticsEnabled(enabled: Boolean) {}
        override suspend fun setSoundEffectsEnabled(enabled: Boolean) {}
        override suspend fun setSecureModeDefault(enabled: Boolean) {}
        override suspend fun setHistoryEnabled(enabled: Boolean) {}
        override suspend fun setHistoryMaxEntries(maxEntries: Int) {}
        override suspend fun setKeepScreenOnDuringSessions(enabled: Boolean) {}
        override suspend fun setLastCalibration(calibrationJson: String?) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setMicPermissionRequested(requested: Boolean) {}
        override suspend fun setStealthWarningAccepted(accepted: Boolean) {}
        override suspend fun setBackgroundGraceSeconds(seconds: Int) {}
    }

    @Test
    fun testFixedConfigUsesImmutableDefaultsAndNullSourceOverride() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val provider = FixedModemConfigProvider(settingsRepo, scope = CoroutineScope(Dispatchers.Unconfined))

        val config = provider.config.value
        assertEquals(Band.RETRO, config.band)
        assertEquals(Speed.NORMAL, config.speed)
        assertEquals(2, config.repeats)
        assertEquals(ModemTuning.DEFAULT, config.tuning)
        assertNull(config.audioSourceOverride)
    }

    @Test
    fun testFixedConfigReflectsSettingsChanges() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val provider = FixedModemConfigProvider(settingsRepo, scope = CoroutineScope(Dispatchers.Unconfined))

        settingsRepo.setBand(Band.STEALTH)
        settingsRepo.setSpeed(Speed.FAST)
        settingsRepo.setRepeats(3)

        assertEquals(Band.STEALTH, provider.config.value.band)
        assertEquals(Speed.FAST, provider.config.value.speed)
        assertEquals(3, provider.config.value.repeats)
        assertEquals(ModemTuning.DEFAULT, provider.config.value.tuning)
        assertNull(provider.config.value.audioSourceOverride)
    }
}
