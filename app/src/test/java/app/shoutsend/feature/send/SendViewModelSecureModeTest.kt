package app.shoutsend.feature.send

import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.crypto.PassphraseStrengthClass
import app.shoutsend.modem.crypto.SecureBox
import app.shoutsend.modem.modulator.TransmissionEnvelope
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.ModemProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying Secure Mode UI interactions, passphrase strength evaluation,
 * session integration, wire overhead accounting, and in-place memory zeroing (Step 46).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SendViewModelSecureModeTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestTransmitSession : TransmitSession {
        private val _state = MutableStateFlow<TransmitState>(TransmitState.Idle)
        override val state: StateFlow<TransmitState> = _state.asStateFlow()
        override val activeEnvelope: StateFlow<TransmissionEnvelope?> = MutableStateFlow(null)
        override val activePayload: StateFlow<Payload?> = MutableStateFlow(null)
        override val activeProfile: StateFlow<ModemProfile?> = MutableStateFlow(null)
        override val activePassphrase: StateFlow<String?> = MutableStateFlow(null)

        var lastPayloadSent: Payload? = null
        var lastProfileSent: ModemProfile? = null
        var lastPassphraseSent: String? = null

        override suspend fun send(
            payload: Payload,
            profile: ModemProfile?,
            securePassphrase: String?
        ): TransmitState {
            lastPayloadSent = payload
            lastProfileSent = profile
            lastPassphraseSent = securePassphrase
            _state.value = TransmitState.Completed(durationMs = 2500L)
            return _state.value
        }

        override fun cancel(reason: String?) {
            _state.value = TransmitState.Cancelled
        }
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
    fun initialStateHasSecureModeDisabled() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSecureEnabled)
        assertFalse(state.showPassphraseDialog)
        assertEquals("", state.passphraseInput)
        assertFalse(state.isPassphraseVisible)
        assertNull(state.passphraseError)
        assertNull(state.passphraseStrength)
        assertNull(viewModel.getActivePassphraseForTesting())
    }

    @Test
    fun toggleSecureModeOnOpensPassphraseDialog() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onSecureModeToggled(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showPassphraseDialog)
        assertNull(state.passphraseError)
    }

    @Test
    fun dismissPassphraseDialogWhenUnconfiguredResetsSecureMode() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onSecureModeToggled(true)
        assertTrue(viewModel.uiState.value.showPassphraseDialog)

        viewModel.dismissPassphraseDialog()
        val state = viewModel.uiState.value
        assertFalse(state.showPassphraseDialog)
        assertFalse(state.isSecureEnabled)
        assertEquals("", state.passphraseInput)
        assertNull(viewModel.getActivePassphraseForTesting())
    }

    @Test
    fun passphraseInputEvaluatesShortLengthAsInvalid() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onPassphraseInputChanged("abc")
        val state = viewModel.uiState.value
        assertEquals("abc", state.passphraseInput)
        assertNotNull(state.passphraseError)
        assertTrue(state.passphraseError!!.contains("8 characters"))
        assertEquals(PassphraseStrengthClass.WEAK, state.passphraseStrength)
    }

    @Test
    fun passphraseInputEvaluatesValidLengthAndStrength() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onPassphraseInputChanged("CyberPassphrase2026!")
        val state = viewModel.uiState.value
        assertEquals("CyberPassphrase2026!", state.passphraseInput)
        assertNull(state.passphraseError)
        assertEquals(PassphraseStrengthClass.STRONG, state.passphraseStrength)
        assertNotNull(state.passphraseStrengthMessage)
    }

    @Test
    fun togglePassphraseVisibilityFlipsMasking() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPassphraseVisible)
        viewModel.togglePassphraseVisibility()
        assertTrue(viewModel.uiState.value.isPassphraseVisible)
        viewModel.togglePassphraseVisibility()
        assertFalse(viewModel.uiState.value.isPassphraseVisible)
    }

    @Test
    fun confirmPassphraseRejectsTooShortInput() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onPassphraseInputChanged("short")
        viewModel.confirmPassphrase()

        val state = viewModel.uiState.value
        assertFalse(state.isSecureEnabled)
        assertNotNull(state.passphraseError)
        assertNull(viewModel.getActivePassphraseForTesting())
    }

    @Test
    fun confirmPassphraseSucceedsWithValidPassphrase() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onPassphraseInputChanged("SuperSecretChirp123")
        viewModel.confirmPassphrase()

        val state = viewModel.uiState.value
        assertTrue(state.isSecureEnabled)
        assertFalse(state.showPassphraseDialog)
        assertEquals("", state.passphraseInput) // string cleared from UI state
        assertNull(state.passphraseError)
        assertNotNull(state.snackbarMessage)

        val active = viewModel.getActivePassphraseForTesting()
        assertNotNull(active)
        assertEquals("SuperSecretChirp123", String(active!!))
    }

    @Test
    fun setPassphraseDirectlyEnablesSecureMode() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.setPassphrase("ValidPassphrase123")

        val state = viewModel.uiState.value
        assertTrue(state.isSecureEnabled)
        assertFalse(state.showPassphraseDialog)
        assertEquals("ValidPassphrase123", String(viewModel.getActivePassphraseForTesting()!!))
    }

    @Test
    fun setPassphraseThrowsWhenBelowMinLength() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        assertThrows(IllegalArgumentException::class.java) {
            viewModel.setPassphrase("tiny")
        }
    }

    @Test
    fun toggleSecureModeOffZeroesMemoryAndDisables() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.setPassphrase("SecretPassphrase99")
        val memoryRef = viewModel.getActivePassphraseForTesting()
        assertNotNull(memoryRef)
        assertEquals("SecretPassphrase99", String(memoryRef!!))

        viewModel.onSecureModeToggled(false)

        val state = viewModel.uiState.value
        assertFalse(state.isSecureEnabled)
        assertNull(viewModel.getActivePassphraseForTesting())
        // In-place zeroing verification
        assertTrue(memoryRef.all { it == '\u0000' })
    }

    @Test
    fun clearSensitiveDataZeroesPassphraseMemory() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.setPassphrase("EphemeralPassphrase1")
        val memoryRef = viewModel.getActivePassphraseForTesting()
        assertNotNull(memoryRef)

        viewModel.clearSensitiveData()

        val state = viewModel.uiState.value
        assertFalse(state.isSecureEnabled)
        assertNull(viewModel.getActivePassphraseForTesting())
        assertTrue(memoryRef!!.all { it == '\u0000' })
    }

    @Test
    fun startTransmissionTransmitsPassphraseAndZeroesMemoryUponCompletion() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onTextChanged("Top secret acoustic transmission", debounceMs = 0L)
        viewModel.setPassphrase("AirGapEncryption2026!")
        advanceUntilIdle()

        val memoryRef = viewModel.getActivePassphraseForTesting()
        assertNotNull(memoryRef)

        var navigated = false
        viewModel.startTransmission { navigated = true }
        advanceUntilIdle()

        assertTrue(navigated)
        assertEquals("AirGapEncryption2026!", session.lastPassphraseSent)

        // Memory must be zeroed immediately in finally block
        assertNull(viewModel.getActivePassphraseForTesting())
        assertTrue(memoryRef!!.all { it == '\u0000' })
        assertFalse(viewModel.uiState.value.isSecureEnabled)
    }

    @Test
    fun recalculateAddsOverheadWhenSecureModeEnabled() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onTextChanged("Acoustic message payload bytes", debounceMs = 0L)
        advanceUntilIdle()

        val plainBytes = viewModel.uiState.value.onAirBytes
        assertTrue(plainBytes > 0)

        viewModel.setPassphrase("SealedPassphrase123")
        advanceUntilIdle()

        val secureBytes = viewModel.uiState.value.onAirBytes
        assertEquals(plainBytes + SecureBox.OVERHEAD_BYTES, secureBytes)
        assertEquals(28, secureBytes - plainBytes)
    }

    @Test
    fun enableSecureModeConvenienceMethod() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.enableSecureMode()

        val state = viewModel.uiState.value
        assertTrue(state.isSecureEnabled)
        assertTrue(state.showPassphraseDialog)
        assertNotNull(state.snackbarMessage)
    }

    @Test
    fun enableSecureModeWithPassphraseConvenienceMethod() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.enableSecureMode("DirectConveniencePass")

        val state = viewModel.uiState.value
        assertTrue(state.isSecureEnabled)
        assertFalse(state.showPassphraseDialog)
        val active = viewModel.getActivePassphraseForTesting()
        assertNotNull(active)
        assertEquals("DirectConveniencePass", String(active!!))
    }
}
