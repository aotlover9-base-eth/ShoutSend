package app.shoutsend.feature.send

import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.session.AudioRouteMonitor
import app.shoutsend.core.session.AudioRouteState
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.modulator.TransmissionEnvelope
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransmitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestTransmitSession : TransmitSession {
        val _state = MutableStateFlow<TransmitState>(TransmitState.Idle)
        override val state: StateFlow<TransmitState> = _state.asStateFlow()

        val _activeEnvelope = MutableStateFlow<TransmissionEnvelope?>(null)
        override val activeEnvelope: StateFlow<TransmissionEnvelope?> = _activeEnvelope.asStateFlow()

        val _activePayload = MutableStateFlow<Payload?>(null)
        override val activePayload: StateFlow<Payload?> = _activePayload.asStateFlow()

        val _activeProfile = MutableStateFlow<ModemProfile?>(null)
        override val activeProfile: StateFlow<ModemProfile?> = _activeProfile.asStateFlow()

        val _activePassphrase = MutableStateFlow<String?>(null)
        override val activePassphrase: StateFlow<String?> = _activePassphrase.asStateFlow()

        var lastPayloadSent: Payload? = null
        var cancelReasonReceived: String? = null

        override suspend fun send(
            payload: Payload,
            profile: ModemProfile?,
            securePassphrase: String?
        ): TransmitState {
            lastPayloadSent = payload
            _activePayload.value = payload
            _activeProfile.value = profile
            _activePassphrase.value = securePassphrase
            _state.value = TransmitState.Transmitting(0.5f, 1.0, 1.0, 1, 2)
            return _state.value
        }

        override fun cancel(reason: String?) {
            cancelReasonReceived = reason
            _state.value = TransmitState.Cancelled
        }
    }

    private class TestVaultRepository : VaultRepository {
        val savedItems = mutableListOf<VaultItem>()

        override fun observeItems(): Flow<List<VaultItem>> = emptyFlow()

        override suspend fun save(
            direction: Direction,
            payload: Payload,
            profileSummary: String,
            durationMs: Long,
            snrDb: Float?,
            isSecure: Boolean
        ): Long {
            val item = VaultItem(
                id = (savedItems.size + 1).toLong(),
                timestamp = System.currentTimeMillis(),
                direction = direction,
                payload = payload,
                isUnreadable = false,
                isSecure = isSecure,
                isStarred = false,
                profileSummary = profileSummary,
                durationMs = durationMs,
                snrDb = snrDb
            )
            savedItems.add(item)
            return item.id
        }

        override suspend fun delete(id: Long) {}
        override suspend fun deleteAll() {}
        override suspend fun setStarred(id: Long, isStarred: Boolean) {}
        override suspend fun prune(maxEntries: Int) {}
    }

    private class TestAudioRouteMonitor : AudioRouteMonitor {
        val _routeState = MutableStateFlow(AudioRouteState())
        override val routeState: StateFlow<AudioRouteState> = _routeState.asStateFlow()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun transmittingStateMapsProgressAndActiveEnvelopeRmsAndTone() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        // 3 windows of 20ms: 0..19ms -> (0.2, 1), 20..39ms -> (0.8, 5), 40..59ms -> (0.4, 12)
        val envelope = TransmissionEnvelope(
            windowDurationMs = 20,
            rms = floatArrayOf(0.2f, 0.8f, 0.4f),
            toneIndex = intArrayOf(1, 5, 12)
        )
        session._activeEnvelope.value = envelope
        session._activeProfile.value = ModemProfile(Band.STEALTH, Scheme.MFSK16, Speed.FAST, 2)

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        session._state.value = TransmitState.Transmitting(
            progress = 0.5f,
            elapsedSeconds = 0.025, // falls into window 1 (20..39 ms)
            remainingSeconds = 0.025,
            currentFrame = 2,
            totalFrames = 4
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isTransmitting)
        assertEquals(0.5f, state.progress)
        assertEquals(0.8f, state.activeRms, 0.001f)
        assertEquals(5, state.activeToneIndex)
        assertEquals(2, state.currentFrame)
        assertEquals(4, state.totalFrames)
        assertEquals(Band.STEALTH, state.band)
        assertEquals(Speed.FAST, state.speed)
    }

    @Test
    fun completionStateTriggersVaultSaveWhenHistoryEnabled() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        settingsRepo.setHistoryEnabled(true)
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        val testPayload = Payload.Text("Acoustic Vault Archive Test")
        session._activePayload.value = testPayload
        session._activeProfile.value = ModemProfile.DEFAULT_RETRO

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        session._state.value = TransmitState.Completed(durationMs = 4200L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertTrue(state.isSavedToVault)
        assertEquals(1, vaultRepo.savedItems.size)
        assertEquals(Direction.SENT, vaultRepo.savedItems[0].direction)
        assertEquals(testPayload, vaultRepo.savedItems[0].payload)
        assertEquals(4200L, vaultRepo.savedItems[0].durationMs)
    }

    @Test
    fun completionStateDoesNotSaveToVaultWhenHistoryDisabled() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        settingsRepo.setHistoryEnabled(false)
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        session._activePayload.value = Payload.Text("Private text not to be saved")
        session._activeProfile.value = ModemProfile.DEFAULT_RETRO

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        session._state.value = TransmitState.Completed(durationMs = 1500L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCompleted)
        assertFalse(state.isSavedToVault)
        assertEquals(0, vaultRepo.savedItems.size)
    }

    @Test
    fun failedStateMapsHumanReadableErrorMessage() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        session._state.value = TransmitState.Failed("PayloadTooLarge")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFailed)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("exceeds maximum acoustic length"))
    }

    @Test
    fun singleTapCancelDelegatesToTransmitSession() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        viewModel.cancelTransmission()
        advanceUntilIdle()

        assertEquals("CancelledByUser", session.cancelReasonReceived)
    }

    @Test
    fun retryReExecutesLastPayload() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        val testPayload = Payload.Text("Retry payload")
        session._activePayload.value = testPayload
        session._activeProfile.value = ModemProfile.DEFAULT_RETRO

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        session._state.value = TransmitState.Failed("AudioFocusDenied")
        advanceUntilIdle()

        viewModel.retryTransmission()
        advanceUntilIdle()

        assertEquals(testPayload, session.lastPayloadSent)
    }

    @Test
    fun routeWarningPropagatesFromMonitor() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.routeWarning)

        routeMonitor._routeState.value = AudioRouteState(
            transmitWarning = "Bluetooth connected: sound may not emit from speaker"
        )
        advanceUntilIdle()

        assertEquals("Bluetooth connected: sound may not emit from speaker", viewModel.uiState.value.routeWarning)
    }

    @Test
    fun secureTransmissionUpdatesIsSecureFlagInUiState() = runTest {
        val session = TestTransmitSession()
        val settingsRepo = FakeSettingsRepository()
        val vaultRepo = TestVaultRepository()
        val routeMonitor = TestAudioRouteMonitor()

        val viewModel = TransmitViewModel(session, settingsRepo, vaultRepo, routeMonitor)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSecure)

        session._activePassphrase.value = "ActiveEncryptedPassphrase123"
        session._state.value = TransmitState.Transmitting(0.5f, 1.0, 1.0, 1, 2)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSecure)

        session._state.value = TransmitState.Completed(durationMs = 3000L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSecure)
    }
}
