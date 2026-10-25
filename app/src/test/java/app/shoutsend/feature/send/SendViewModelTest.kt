package app.shoutsend.feature.send

import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.settings.Settings
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Speed
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestTransmitSession : TransmitSession {
        private val _state = MutableStateFlow<TransmitState>(TransmitState.Idle)
        override val state: StateFlow<TransmitState> = _state.asStateFlow()

        var lastPayloadSent: Payload? = null
        var lastProfileSent: ModemProfile? = null
        var lastPassphraseSent: String? = null
        var cancelReasonReceived: String? = null

        override suspend fun send(
            payload: Payload,
            profile: ModemProfile?,
            securePassphrase: String?
        ): TransmitState {
            lastPayloadSent = payload
            lastProfileSent = profile
            lastPassphraseSent = securePassphrase
            _state.value = TransmitState.Transmitting(
                progress = 0.5f,
                elapsedSeconds = 1.0,
                remainingSeconds = 1.0,
                currentFrame = 1,
                totalFrames = 2
            )
            return _state.value
        }

        override fun cancel(reason: String?) {
            cancelReasonReceived = reason
            _state.value = TransmitState.Cancelled
        }

        fun emitState(newState: TransmitState) {
            _state.value = newState
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

    // =========================================================================
    // 1. SharedTextParser Tests
    // =========================================================================

    @Test
    fun sharedTextParserHandlesNullBlankAndEmpty() {
        val nullResult = SharedTextParser.parse(null)
        assertEquals(SendPayloadType.TEXT, nullResult.type)
        assertEquals("", nullResult.content)
        assertFalse(nullResult.isTruncated)

        val emptyResult = SharedTextParser.parse("   ")
        assertEquals(SendPayloadType.TEXT, emptyResult.type)
        assertEquals("", emptyResult.content)
        assertFalse(emptyResult.isTruncated)
    }

    @Test
    fun sharedTextParserDetectsHttpAndHttpsUrls() {
        val httpResult = SharedTextParser.parse("http://example.org/path")
        assertEquals(SendPayloadType.URL, httpResult.type)
        assertEquals("http://example.org/path", httpResult.content)
        assertFalse(httpResult.isTruncated)

        val httpsResult = SharedTextParser.parse("  HTTPS://SHOUTSEND.APP/DOWNLOAD  ")
        assertEquals(SendPayloadType.URL, httpsResult.type)
        assertEquals("HTTPS://SHOUTSEND.APP/DOWNLOAD", httpsResult.content)
        assertFalse(httpsResult.isTruncated)
    }

    @Test
    fun sharedTextParserDetectsPlainText() {
        val plainText = "Meet me at the cafe at 4 PM"
        val result = SharedTextParser.parse(plainText)
        assertEquals(SendPayloadType.TEXT, result.type)
        assertEquals(plainText, result.content)
        assertFalse(result.isTruncated)
    }

    @Test
    fun sharedTextParserTruncatesHugeTextAt280Bytes() {
        val hugeAscii = "A".repeat(350)
        val result = SharedTextParser.parse(hugeAscii)
        assertEquals(SendPayloadType.TEXT, result.type)
        assertTrue(result.isTruncated)
        assertEquals(350, result.originalByteCount)
        assertEquals(280, result.content.toByteArray(Charsets.UTF_8).size)
        assertEquals("A".repeat(280), result.content)
    }

    @Test
    fun sharedTextParserPreservesMultiByteUtf8CodePointIntegrityOnTruncation() {
        // Japanese character "あ" is 3 UTF-8 bytes: 93 * 3 = 279 bytes. 94 * 3 = 282 bytes.
        val repeatedJapanese = "あ".repeat(100)
        val result = SharedTextParser.parse(repeatedJapanese)
        assertTrue(result.isTruncated)
        assertEquals(300, result.originalByteCount)
        assertEquals(93, result.content.length)
        assertEquals(279, result.content.toByteArray(Charsets.UTF_8).size)
    }

    // =========================================================================
    // 2. ClipboardSanitizer Tests
    // =========================================================================

    @Test
    fun clipboardSanitizerStripsForbiddenControlCharsFromText() {
        val dirtyText = "Line 1\r\nLine 2\u0000\u0007with bell\u007Fand del"
        val sanitized = ClipboardSanitizer.sanitizeText(dirtyText)
        assertEquals("Line 1\nLine 2with belland del", sanitized)
    }

    @Test
    fun clipboardSanitizerNormalizesAndCleansUrls() {
        val dirtyUrl = "  https://example.com/path\nwith\tspaces\u0000 "
        val sanitized = ClipboardSanitizer.sanitizeUrl(dirtyUrl)
        assertEquals("https://example.com/pathwithspaces", sanitized)
    }

    // =========================================================================
    // 3. SendViewModel Tests
    // =========================================================================

    @Test
    fun initialViewModelStateRestoresSettings() = runTest {
        val settingsRepo = FakeSettingsRepository(
            Settings(
                band = Band.STEALTH,
                speed = Speed.FAST,
                repeats = 3,
                stealthWarningAccepted = true,
                lastSendType = "url"
            )
        )
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SendPayloadType.URL, state.selectedType)
        assertEquals(Band.STEALTH, state.selectedBand)
        assertEquals(Speed.FAST, state.selectedSpeed)
        assertEquals(3, state.repeats)
        assertTrue(state.stealthWarningAccepted)
    }

    @Test
    fun textCompositionUpdatesOnAirBytesAndDurationLive() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.onAirBytes)
        assertEquals(0.0, viewModel.uiState.value.estimatedDurationSeconds, 0.001)
        assertFalse(viewModel.uiState.value.isValid)

        viewModel.onTextChanged("Hello ShoutSend acoustic network!", debounceMs = 0L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("onAirBytes should be positive", state.onAirBytes > 0)
        assertTrue("estimatedDurationSeconds should be positive", state.estimatedDurationSeconds > 0.0)
        assertTrue(state.isValid)
        assertFalse(state.isTooLong)
        assertNull(state.validationError)
    }

    @Test
    fun sevenBitAsciiCompactionReducesOnAirBytes() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        // 280 bytes of 7-bit ASCII compresses with 2-byte prefix to 247 bytes on wire (247 < 280)
        val asciiText = "A".repeat(280)
        viewModel.onTextChanged(asciiText, debounceMs = 0L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(247, state.onAirBytes)
        assertTrue(state.isValid)
        assertFalse(state.isTooLong)
    }

    @Test
    fun textExceeding280BytesMarksTooLongAndDisablesAction() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        val hugeText = "X".repeat(281)
        viewModel.onTextChanged(hugeText, debounceMs = 0L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isTooLong)
        assertFalse(state.isValid)
    }

    @Test
    fun urlValidationPassesValidUrlsAndRejectsMalformed() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        viewModel.selectPayloadType(SendPayloadType.URL)
        advanceUntilIdle()

        // Valid HTTPS URL
        viewModel.onUrlChanged("https://shoutsend.app", debounceMs = 0L)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isValid)
        assertNull(viewModel.uiState.value.validationError)

        // Invalid scheme URL (e.g. ftp)
        viewModel.onUrlChanged("ftp://files.example.com", debounceMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isValid)
        assertNotNull(viewModel.uiState.value.validationError)

        // Empty host URL
        viewModel.onUrlChanged("http://", debounceMs = 0L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isValid)
        assertNotNull(viewModel.uiState.value.validationError)
    }

    @Test
    fun stealthModeWarningGateEnforcesUserAcceptance() = runTest {
        val settingsRepo = FakeSettingsRepository(
            Settings(
                band = Band.RETRO,
                stealthWarningAccepted = false
            )
        )
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        // Attempting to select Stealth shows disclosure dialog
        viewModel.onBandSelected(Band.STEALTH)
        assertTrue(viewModel.uiState.value.showStealthWarningDialog)
        assertEquals(Band.RETRO, viewModel.uiState.value.selectedBand)

        // Dismissing keeps Retro
        viewModel.onDismissStealthWarning()
        assertFalse(viewModel.uiState.value.showStealthWarningDialog)
        assertEquals(Band.RETRO, viewModel.uiState.value.selectedBand)

        // Confirming disclosure updates SettingsRepository and selects Stealth
        viewModel.onBandSelected(Band.STEALTH)
        viewModel.onAcceptStealthWarning()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showStealthWarningDialog)
        assertEquals(Band.STEALTH, viewModel.uiState.value.selectedBand)
        assertTrue(settingsRepo.settings.value.stealthWarningAccepted)
        assertEquals(Band.STEALTH, settingsRepo.settings.value.band)
    }

    @Test
    fun profileTuningPersistsToSettingsRepository() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onSpeedSelected(Speed.FAST)
        viewModel.onRepeatsSelected(3)
        advanceUntilIdle()

        assertEquals(Speed.FAST, settingsRepo.settings.value.speed)
        assertEquals(3, settingsRepo.settings.value.repeats)
        assertEquals(Speed.FAST, viewModel.uiState.value.selectedSpeed)
        assertEquals(3, viewModel.uiState.value.repeats)
    }

    @Test
    fun payloadTypeSelectionPersistsLastSendType() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.selectPayloadType(SendPayloadType.URL)
        advanceUntilIdle()

        assertEquals("url", settingsRepo.settings.value.lastSendType)
        assertEquals(SendPayloadType.URL, viewModel.uiState.value.selectedType)
    }

    @Test
    fun consumeSharedRouteArgsSetsContentAndTruncationNotice() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        val longSharedText = "Shared text ".repeat(30)
        viewModel.consumeSharedRouteArgs(longSharedText, "TEXT")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SendPayloadType.TEXT, state.selectedType)
        assertNotNull(state.truncatedNotice)
        assertTrue(state.isValid)
        assertEquals(280, state.textValue.toByteArray(Charsets.UTF_8).size)
    }

    @Test
    fun startTransmissionDelegatesToTransmitSession() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.onTextChanged("Chirp message test", debounceMs = 0L)
        advanceUntilIdle()

        var navigated = false
        viewModel.startTransmission { navigated = true }
        advanceUntilIdle()

        assertTrue(navigated)
        assertNotNull(session.lastPayloadSent)
        assertTrue(session.lastPayloadSent is Payload.Text)
        assertEquals("Chirp message test", (session.lastPayloadSent as Payload.Text).value)
        assertTrue(viewModel.uiState.value.isTransmitting)
    }

    @Test
    fun cancelTransmissionDelegatesToTransmitSession() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val session = TestTransmitSession()
        val viewModel = SendViewModel(settingsRepo, session)
        advanceUntilIdle()

        viewModel.cancelTransmission()
        assertEquals("CancelledByUser", session.cancelReasonReceived)
    }
}
