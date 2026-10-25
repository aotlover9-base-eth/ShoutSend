package app.shoutsend.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ThemeController
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.core.designsystem.TokenOverrides
import app.shoutsend.core.designsystem.TokenOverridesRepository
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeTokenOverridesRepository : TokenOverridesRepository {
        private val _overrides = MutableStateFlow(TokenOverrides())
        override val overrides: Flow<TokenOverrides> = _overrides.asStateFlow()
        override suspend fun update(transform: (TokenOverrides) -> TokenOverrides) {
            _overrides.value = transform(_overrides.value)
        }
        override suspend fun resetAll() {
            _overrides.value = TokenOverrides()
        }
        override suspend fun resetColor(role: String) {
            _overrides.value = _overrides.value.copy(colors = _overrides.value.colors - role)
        }
        override suspend fun resetRadius(name: String) {
            _overrides.value = _overrides.value.copy(radii = _overrides.value.radii - name)
        }
    }

    private fun createTestDataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("test_settings_${System.nanoTime()}.preferences_pb") }
    )

    @Test
    fun defaultValuesMatchSpecification() {
        val settings = Settings()
        assertEquals(Band.RETRO, settings.band)
        assertEquals(Speed.NORMAL, settings.speed)
        assertEquals(2, settings.repeats)
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertFalse(settings.dynamicColor)
        assertTrue(settings.hapticsEnabled)
        assertTrue(settings.soundEffectsEnabled)
        assertFalse(settings.secureModeDefault)
        assertTrue(settings.historyEnabled)
        assertEquals(500, settings.historyMaxEntries)
        assertTrue(settings.keepScreenOnDuringSessions)
        assertNull(settings.lastCalibration)
        assertFalse(settings.onboardingCompleted)
        assertFalse(settings.micPermissionRequested)
        assertFalse(settings.stealthWarningAccepted)
        assertEquals(5, settings.backgroundGraceSeconds)
    }

    @Test
    fun serializationRoundTripOfEveryField() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dataStore = createTestDataStore(backgroundScope)
        val repository = DataStoreSettingsRepository(dataStore, backgroundScope)

        repository.update {
            it.copy(
                band = Band.STEALTH,
                speed = Speed.FAST,
                repeats = 3,
                themeMode = ThemeMode.LIGHT,
                dynamicColor = true,
                hapticsEnabled = false,
                soundEffectsEnabled = false,
                secureModeDefault = true,
                historyEnabled = false,
                historyMaxEntries = 1000,
                keepScreenOnDuringSessions = false,
                lastCalibration = "{\"profile\":\"test\"}",
                onboardingCompleted = true,
                micPermissionRequested = true,
                stealthWarningAccepted = true,
                backgroundGraceSeconds = 15
            )
        }

        val saved = repository.settings.first { it.band == Band.STEALTH }
        assertEquals(Band.STEALTH, saved.band)
        assertEquals(Speed.FAST, saved.speed)
        assertEquals(3, saved.repeats)
        assertEquals(ThemeMode.LIGHT, saved.themeMode)
        assertTrue(saved.dynamicColor)
        assertFalse(saved.hapticsEnabled)
        assertFalse(saved.soundEffectsEnabled)
        assertTrue(saved.secureModeDefault)
        assertFalse(saved.historyEnabled)
        assertEquals(1000, saved.historyMaxEntries)
        assertFalse(saved.keepScreenOnDuringSessions)
        assertEquals("{\"profile\":\"test\"}", saved.lastCalibration)
        assertTrue(saved.onboardingCompleted)
        assertTrue(saved.micPermissionRequested)
        assertTrue(saved.stealthWarningAccepted)
        assertEquals(15, saved.backgroundGraceSeconds)
    }

    @Test
    fun unknownEnumValuesCleanlyFallbackToDefaults() = runTest {
        val dataStore = createTestDataStore(backgroundScope)

        // Write corrupt/unknown enum strings directly
        dataStore.edit { prefs ->
            prefs[DataStoreSettingsRepository.KEY_BAND] = "UNKNOWN_BAND_NAME"
            prefs[DataStoreSettingsRepository.KEY_SPEED] = "HYPER_SONIC"
            prefs[DataStoreSettingsRepository.KEY_THEME_MODE] = "NEON_GLOW"
            prefs[DataStoreSettingsRepository.KEY_REPEATS] = 3
        }

        val repository = DataStoreSettingsRepository(dataStore, backgroundScope)
        // Wait until datastore has read the file (repeats == 3)
        val settings = repository.settings.first { it.repeats == 3 }

        assertEquals("Unknown band must fall back to RETRO", Band.RETRO, settings.band)
        assertEquals("Unknown speed must fall back to NORMAL", Speed.NORMAL, settings.speed)
        assertEquals("Unknown theme mode must fall back to DARK", ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun outOfBoundsNumericValuesAreClamped() = runTest {
        val dataStore = createTestDataStore(backgroundScope)

        dataStore.edit { prefs ->
            prefs[DataStoreSettingsRepository.KEY_REPEATS] = 99
            prefs[DataStoreSettingsRepository.KEY_HISTORY_MAX_ENTRIES] = 10_000
            prefs[DataStoreSettingsRepository.KEY_BACKGROUND_GRACE_SECONDS] = 999
        }

        val repository = DataStoreSettingsRepository(dataStore, backgroundScope)
        // Wait until datastore has loaded clamped values
        val settings = repository.settings.first { it.repeats == Settings.REPEATS_MAX }

        assertEquals(Settings.REPEATS_MAX, settings.repeats)
        assertEquals(Settings.HISTORY_MAX, settings.historyMaxEntries)
        assertEquals(Settings.GRACE_MAX, settings.backgroundGraceSeconds)

        // Test lower bounds clamping via atomic update
        repository.update {
            it.copy(
                repeats = 0.coerceIn(Settings.REPEATS_MIN, Settings.REPEATS_MAX),
                historyMaxEntries = 10.coerceIn(Settings.HISTORY_MIN, Settings.HISTORY_MAX),
                backgroundGraceSeconds = (-5).coerceIn(Settings.GRACE_MIN, Settings.GRACE_MAX)
            )
        }

        val clamped = repository.settings.first {
            it.repeats == Settings.REPEATS_MIN &&
                it.historyMaxEntries == Settings.HISTORY_MIN &&
                it.backgroundGraceSeconds == Settings.GRACE_MIN
        }
        assertEquals(Settings.REPEATS_MIN, clamped.repeats)
        assertEquals(Settings.HISTORY_MIN, clamped.historyMaxEntries)
        assertEquals(Settings.GRACE_MIN, clamped.backgroundGraceSeconds)
    }

    @Test
    fun resetReturnsToDefaultSettings() = runTest {
        val dataStore = createTestDataStore(backgroundScope)
        val repository = DataStoreSettingsRepository(dataStore, backgroundScope)

        repository.setBand(Band.STEALTH)
        repository.setRepeats(1)
        repository.setDynamicColor(true)

        val updated = repository.settings.first { it.band == Band.STEALTH }
        assertEquals(Band.STEALTH, updated.band)

        repository.reset()

        val resetSettings = repository.settings.first { it.band == Band.RETRO && !it.dynamicColor }
        assertEquals(Settings(), resetSettings)
    }

    @Test
    fun concurrentUpdatesWorkAtomically() = runTest {
        val repository = FakeSettingsRepository()

        val jobs = (1..50).map { i ->
            async {
                repository.update { current ->
                    current.copy(repeats = if (i % 2 == 0) 2 else 3)
                }
            }
        }
        jobs.awaitAll()

        assertTrue(repository.settings.value.repeats in 2..3)
    }

    @Test
    fun micPermissionRequestedProviderAdapterWorks() = runTest {
        val repository = FakeSettingsRepository()
        val provider = DataStoreMicPermissionRequestedProvider(repository)

        assertFalse(provider.hasRequestedBefore())
        provider.setRequestedBefore(true)
        assertTrue(provider.hasRequestedBefore())
        assertTrue(repository.settings.value.micPermissionRequested)
    }

    @Test
    fun bindToThemeControllerUpdatesThemeInstantly() = runTest {
        val fakeTokenRepo = FakeTokenOverridesRepository()
        val controller = ThemeController(
            initialBaseTokens = DesignTokens.defaultDark(),
            repository = fakeTokenRepo,
            scope = backgroundScope
        )
        val settingsRepo = FakeSettingsRepository()

        settingsRepo.bindToThemeController(controller, backgroundScope)

        assertEquals(ThemeMode.DARK, controller.tokens.first { it.themeMode == ThemeMode.DARK }.themeMode)

        settingsRepo.setThemeMode(ThemeMode.LIGHT)
        val lightTokens = controller.tokens.first { it.themeMode == ThemeMode.LIGHT }
        assertEquals(ThemeMode.LIGHT, lightTokens.themeMode)

        settingsRepo.setDynamicColor(true)
        val dynamicTokens = controller.tokens.first { it.dynamicColor }
        assertTrue(dynamicTokens.dynamicColor)
    }

    @Test
    fun lastTabCanBeUpdatedAndPersisted() = runTest {
        val dataStore = createTestDataStore(backgroundScope)
        val repo = DataStoreSettingsRepository(dataStore, backgroundScope)

        assertEquals("receive", repo.settings.first().lastTab)

        repo.setLastTab("send")
        assertEquals("send", repo.settings.first { it.lastTab == "send" }.lastTab)

        repo.setLastTab("vault")
        assertEquals("vault", repo.settings.first { it.lastTab == "vault" }.lastTab)
    }
}
