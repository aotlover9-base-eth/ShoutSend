package app.shoutsend.feature.settings

import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.settings.Settings
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettings: FakeSettingsRepository
    private lateinit var fakeVault: FakeVaultRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettings = FakeSettingsRepository()
        fakeVault = FakeVaultRepository()
        viewModel = SettingsViewModel(fakeSettings, fakeVault)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDefaultsMatchUiGateSpecification() = runTest {
        advanceUntilIdle()
        val s = viewModel.uiState.value.settings
        assertEquals(Band.RETRO, s.band)
        assertEquals(Speed.NORMAL, s.speed)
        assertEquals(1, s.repeats)
        assertTrue(s.historyEnabled)
        assertTrue(s.hapticsEnabled)
        assertTrue(s.soundEffectsEnabled)
        assertFalse(s.keepScreenOnDuringSessions)
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertFalse(s.dynamicColor)
        assertEquals(5, s.backgroundGraceSeconds)
    }

    @Test
    fun testPersistenceOfTransmissionAndAppearanceSettings() = runTest {
        advanceUntilIdle()

        viewModel.setSpeed(Speed.FAST)
        viewModel.setRepeats(3)
        viewModel.setHapticsEnabled(false)
        viewModel.setSoundEffectsEnabled(false)
        viewModel.setKeepScreenOnDuringSessions(true)
        viewModel.setBackgroundGraceSeconds(15)
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setDynamicColor(true)
        advanceUntilIdle()

        val s = fakeSettings.settings.value
        assertEquals(Speed.FAST, s.speed)
        assertEquals(3, s.repeats)
        assertFalse(s.hapticsEnabled)
        assertFalse(s.soundEffectsEnabled)
        assertTrue(s.keepScreenOnDuringSessions)
        assertEquals(15, s.backgroundGraceSeconds)
        assertEquals(ThemeMode.LIGHT, s.themeMode)
        assertTrue(s.dynamicColor)
    }

    @Test
    fun testStealthDisclosureFlow() = runTest {
        advanceUntilIdle()
        assertFalse(fakeSettings.settings.value.stealthWarningAccepted)

        // Attempt to switch to Stealth band without prior acceptance
        viewModel.setBand(Band.STEALTH)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showStealthWarningDialog)
        assertEquals(Band.RETRO, fakeSettings.settings.value.band)

        // Dismiss dialog
        viewModel.onDismissStealthWarning()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showStealthWarningDialog)
        assertEquals(Band.RETRO, fakeSettings.settings.value.band)

        // Switch again and accept
        viewModel.setBand(Band.STEALTH)
        viewModel.onAcceptStealthWarning()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showStealthWarningDialog)
        assertTrue(fakeSettings.settings.value.stealthWarningAccepted)
        assertEquals(Band.STEALTH, fakeSettings.settings.value.band)

        // Switching back to Retro and back to Stealth requires no dialog
        viewModel.setBand(Band.RETRO)
        advanceUntilIdle()
        assertEquals(Band.RETRO, fakeSettings.settings.value.band)

        viewModel.setBand(Band.STEALTH)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showStealthWarningDialog)
        assertEquals(Band.STEALTH, fakeSettings.settings.value.band)
    }

    @Test
    fun testPruningFlowKeepsStarredEntries() = runTest {
        advanceUntilIdle()

        // Setup 150 unstarred items and 2 starred items (152 total)
        for (i in 1..150) {
            fakeVault.addTestItem(id = i.toLong(), isStarred = false)
        }
        fakeVault.addTestItem(id = 201L, isStarred = true)
        fakeVault.addTestItem(id = 202L, isStarred = true)
        advanceUntilIdle()

        assertEquals(150, viewModel.uiState.value.unstarredVaultCount)

        // Request lowering max limit to 100 -> would delete 50 unstarred items
        viewModel.requestHistoryMaxEntries(100)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPruneConfirmDialog)
        assertEquals(100, viewModel.uiState.value.pendingMaxEntries)
        assertEquals(50, viewModel.uiState.value.pendingPruneCount)
        assertEquals(500, fakeSettings.settings.value.historyMaxEntries)

        // Confirm prune
        viewModel.confirmPrune()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showPruneConfirmDialog)
        assertNull(viewModel.uiState.value.pendingMaxEntries)
        assertEquals(100, fakeSettings.settings.value.historyMaxEntries)

        // Verify remaining vault items: 100 unstarred + 2 starred = 102 total
        val remaining = fakeVault.currentItems
        assertEquals(102, remaining.size)
        val remainingStarred = remaining.filter { it.isStarred }
        assertEquals(2, remainingStarred.size)
        assertTrue(remainingStarred.any { it.id == 201L })
        assertTrue(remainingStarred.any { it.id == 202L })
    }

    @Test
    fun testDeleteAllAppDataPurgesVaultAndResetsSettings() = runTest {
        advanceUntilIdle()
        fakeVault.addTestItem(id = 1L, isStarred = true)
        fakeSettings.setSpeed(Speed.FAST)
        fakeSettings.setOnboardingCompleted(true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteAllDataDialog)
        viewModel.requestDeleteAllData()
        assertTrue(viewModel.uiState.value.showDeleteAllDataDialog)

        viewModel.dismissDeleteAllDataDialog()
        assertFalse(viewModel.uiState.value.showDeleteAllDataDialog)

        viewModel.requestDeleteAllData()
        viewModel.confirmDeleteAllData()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteAllDataDialog)
        assertTrue(viewModel.uiState.value.dataResetCompleted)
        assertTrue(fakeVault.currentItems.isEmpty())
        assertEquals(Settings.DEFAULT_SPEED, fakeSettings.settings.value.speed)
        assertFalse(fakeSettings.settings.value.onboardingCompleted)
    }

    @Test
    fun testReplayOnboardingResetsFlagAndExecutesCallback() = runTest {
        fakeSettings.setOnboardingCompleted(true)
        advanceUntilIdle()
        assertTrue(fakeSettings.settings.value.onboardingCompleted)

        var navigated = false
        viewModel.replayOnboarding { navigated = true }
        advanceUntilIdle()

        assertFalse(fakeSettings.settings.value.onboardingCompleted)
        assertTrue(navigated)
    }

    private class FakeVaultRepository : VaultRepository {
        val currentItems = mutableListOf<VaultItem>()
        private val _flow = MutableStateFlow<List<VaultItem>>(emptyList())

        override fun observeItems(): Flow<List<VaultItem>> = _flow.asStateFlow()

        fun addTestItem(id: Long, isStarred: Boolean) {
            val item = VaultItem(
                id = id,
                timestamp = id * 1000L,
                direction = Direction.SENT,
                payload = Payload.Text("Test $id"),
                isUnreadable = false,
                isSecure = false,
                isStarred = isStarred,
                profileSummary = "Retro Normal",
                durationMs = 3000L,
                snrDb = null
            )
            currentItems.add(item)
            _flow.value = currentItems.toList()
        }

        override suspend fun save(
            direction: Direction,
            payload: Payload,
            profileSummary: String,
            durationMs: Long,
            snrDb: Float?,
            isSecure: Boolean
        ): Long = 1L

        override suspend fun delete(id: Long) {
            currentItems.removeAll { it.id == id }
            _flow.value = currentItems.toList()
        }

        override suspend fun deleteAll() {
            currentItems.clear()
            _flow.value = emptyList()
        }

        override suspend fun setStarred(id: Long, isStarred: Boolean) {
            val idx = currentItems.indexOfFirst { it.id == id }
            if (idx >= 0) {
                currentItems[idx] = currentItems[idx].copy(isStarred = isStarred)
                _flow.value = currentItems.toList()
            }
        }

        override suspend fun prune(maxEntries: Int) {
            val unstarred = currentItems.filter { !it.isStarred }.sortedByDescending { it.timestamp }
            if (unstarred.size > maxEntries) {
                val toKeep = unstarred.take(maxEntries).map { it.id }.toSet()
                currentItems.removeAll { !it.isStarred && it.id !in toKeep }
                _flow.value = currentItems.toList()
            }
        }
    }
}
