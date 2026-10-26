package app.shoutsend.feature.receive

import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.designsystem.colormap.ColormapFactory
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.core.session.SessionMode
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiveViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeReceiver: FakeAcousticReceiver
    private lateinit var fakeCoordinator: FakeAudioCoordinator
    private lateinit var fakeSettings: FakeSettingsRepository
    private lateinit var viewModel: ReceiveViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeReceiver = FakeAcousticReceiver()
        fakeCoordinator = FakeAudioCoordinator()
        fakeSettings = FakeSettingsRepository()
        viewModel = ReceiveViewModel(fakeReceiver, fakeCoordinator, fakeSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsIdle() {
        val state = viewModel.uiState.value
        assertEquals(AcousticStatus.IDLE, state.status)
        assertFalse(state.isListening)
        assertFalse(state.isPreambleLocked)
        assertEquals(Band.RETRO, state.band)
    }

    @Test
    fun testStartListeningAcquiresCoordinatorLockAndStartsReceiver() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        assertTrue(fakeCoordinator.isReceiveLocked)
        assertTrue(fakeReceiver.isRunning)
        assertTrue(viewModel.uiState.value.isListening)
        assertEquals(AcousticStatus.LISTENING, viewModel.uiState.value.status)
    }

    @Test
    fun testStartListeningFailsWhenCoordinatorRejectsLock() = runTest {
        fakeCoordinator.rejectNextReceiveLock = true
        viewModel.startListening()
        advanceUntilIdle()

        assertFalse(fakeReceiver.isRunning)
        assertFalse(viewModel.uiState.value.isListening)
        assertEquals(AcousticStatus.ERROR, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.lastError)
    }

    @Test
    fun testStopListeningReleasesLockAndStopsReceiver() = runTest {
        viewModel.startListening()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.stopListening()
        advanceUntilIdle()

        assertFalse(fakeCoordinator.isReceiveLocked)
        assertFalse(fakeReceiver.isRunning)
        assertFalse(viewModel.uiState.value.isListening)
        assertEquals(AcousticStatus.IDLE, viewModel.uiState.value.status)
    }

    @Test
    fun testPreambleDetectedEventTriggersLockedState() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        fakeReceiver.emitEvent(ReceiveEvent.PreambleDetected)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AcousticStatus.LOCKED, state.status)
        assertTrue(state.isPreambleLocked)
    }

    @Test
    fun testLockedEventUpdatesSnrAndMaintainsPreambleLock() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        fakeReceiver.emitEvent(ReceiveEvent.Locked(snrDb = 21.5))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AcousticStatus.LOCKED, state.status)
        assertTrue(state.isPreambleLocked)
        assertEquals(21.5, state.snrDb, 0.01)
    }

    @Test
    fun testFrameDecodedEventUpdatesProgress() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        fakeReceiver.emitEvent(ReceiveEvent.FrameDecoded(fragmentIndex = 0, fragmentCount = 3, correctedBytes = 1))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AcousticStatus.RECEIVING, state.status)
        assertTrue(state.isPreambleLocked)
        assertEquals(1, state.currentFragment)
        assertEquals(3, state.totalFragments)
    }

    @Test
    fun testMessageCompleteEventDeliversPayloadAndResetsLock() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        val testMessage = ReceivedMessage(
            type = MessageType.TEXT,
            flags = MessageFlags.NONE,
            messageId = 42,
            payload = "Hello World".toByteArray(Charsets.UTF_8)
        )
        val testPayload = Payload.Text("Hello World")

        fakeReceiver.emitEvent(ReceiveEvent.MessageComplete(testMessage, testPayload))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AcousticStatus.SUCCESS, state.status)
        assertFalse(state.isPreambleLocked)
        assertEquals(testPayload, state.lastReceivedPayload)
    }

    @Test
    fun testLostEventReturnsToListeningState() = runTest {
        viewModel.startListening()
        advanceUntilIdle()

        fakeReceiver.emitEvent(ReceiveEvent.Locked(snrDb = 19.0))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isPreambleLocked)

        fakeReceiver.emitEvent(ReceiveEvent.Lost("Carrier signal faded"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AcousticStatus.LISTENING, state.status)
        assertFalse(state.isPreambleLocked)
    }

    @Test
    fun testSetBandUpdatesBandAndRestartsReceiverIfListening() = runTest {
        viewModel.startListening()
        advanceUntilIdle()
        assertEquals(Band.RETRO, fakeReceiver.currentBand)

        viewModel.setBand(Band.STEALTH)
        advanceUntilIdle()

        assertEquals(Band.STEALTH, viewModel.uiState.value.band)
        assertEquals(Band.STEALTH, fakeReceiver.currentBand)
    }

    @Test
    fun testWaterfallBufferRowPushAndShift() {
        val buffer = WaterfallBuffer(width = 16, height = 4)
        val dummyLut = IntArray(ColormapFactory.COLORMAP_SIZE) { it }
        buffer.updateColormap(dummyLut)

        // Push row 1 with value 0.5f (index 128)
        val row1 = FloatArray(16) { 0.5f }
        buffer.pushRow(row1)

        assertEquals(128, buffer.pixelBuffer[0])
        assertEquals(128, buffer.pixelBuffer[15])

        // Push row 2 with value 1.0f (index 255)
        val row2 = FloatArray(16) { 1.0f }
        buffer.pushRow(row2)

        // Row 0 is now 255, Row 1 (offset 16..31) shifted down to 128
        assertEquals(255, buffer.pixelBuffer[0])
        assertEquals(128, buffer.pixelBuffer[16])

        buffer.clear()
        assertEquals(0, buffer.pixelBuffer[0])
    }

    // --- Test Doubles ---

    private class FakeAcousticReceiver : AcousticReceiver {
        private val _events = MutableSharedFlow<ReceiveEvent>(replay = 16)
        override val events: SharedFlow<ReceiveEvent> = _events

        private val _diagnostics = MutableStateFlow(ReceiverDiagnostics())
        override val diagnostics: StateFlow<ReceiverDiagnostics> = _diagnostics

        private val _spectrumChannel = Channel<FloatArray>(capacity = 60)
        override val spectrumRows: ReceiveChannel<FloatArray> = _spectrumChannel

        override var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)? = null
        override var isRunning: Boolean = false
        var currentBand: Band? = null

        override fun start(band: Band, passphrase: String?, tuning: ModemTuning) {
            isRunning = true
            currentBand = band
        }

        override fun stop() {
            isRunning = false
        }

        suspend fun emitEvent(event: ReceiveEvent) {
            _events.emit(event)
        }
    }

    private class FakeAudioCoordinator : AudioCoordinator {
        var isReceiveLocked: Boolean = false
        var rejectNextReceiveLock: Boolean = false

        override val activeMode: StateFlow<SessionMode> = MutableStateFlow(SessionMode.IDLE)
        override val keepScreenOn: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun acquireTransmitLock(): Boolean = true
        override fun releaseTransmitLock() {}

        override suspend fun acquireReceiveLock(): Boolean {
            if (rejectNextReceiveLock) return false
            isReceiveLocked = true
            return true
        }

        override fun releaseReceiveLock() {
            isReceiveLocked = false
        }

        override suspend fun acquireCalibrationLock(): Boolean = true
        override fun releaseCalibrationLock() {}
    }
}
