package app.shoutsend.feature.calibration

import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.OutputRouteInfo
import app.shoutsend.core.audio.transmitter.TransmitProgress
import app.shoutsend.core.audio.transmitter.TransmitResult
import app.shoutsend.core.calibration.BandReportSummary
import app.shoutsend.core.calibration.CalibrationResult
import app.shoutsend.core.calibration.CalibrationRunner
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.settings.Settings
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.calibration.BandVerdict
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalibrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettings: FakeSettingsRepository
    private lateinit var fakeReceiver: TestAcousticReceiver
    private lateinit var fakeTransmitter: TestAcousticTransmitter
    private lateinit var runner: CalibrationRunner
    private lateinit var viewModel: CalibrationViewModel

    private class TestAcousticReceiver : AcousticReceiver {
        override val events: SharedFlow<ReceiveEvent> = MutableSharedFlow()
        override val diagnostics: StateFlow<ReceiverDiagnostics> =
            MutableStateFlow(ReceiverDiagnostics()).asStateFlow()
        override val spectrumRows: ReceiveChannel<FloatArray> = Channel()
        override var rawAudioTap: ((ShortArray, Int, Int) -> Unit)? = null
        override val isRunning: Boolean = false
        override fun start(band: Band, passphrase: String?, tuning: ModemTuning) {
            // Emulate synthetic recording into rawAudioTap
            val mockBuffer = ShortArray(4800)
            rawAudioTap?.invoke(mockBuffer, 0, mockBuffer.size)
        }
        override fun stop() {}
    }

    private class TestAcousticTransmitter : AcousticTransmitter {
        override suspend fun transmit(
            source: AudioStreamSource,
            onProgress: (progress: TransmitProgress) -> Unit
        ): TransmitResult {
            onProgress(
                TransmitProgress(
                    samplesWritten = 2400,
                    totalSamples = 4800,
                    fraction = 0.5f,
                    elapsedSeconds = 0.05,
                    remainingSeconds = 0.05
                )
            )
            return TransmitResult.Success(100L, 100L, 0)
        }
    }

    private val sampleResult = CalibrationResult(
        timestampMs = 1700000000000L,
        retroReport = BandReportSummary(
            band = Band.RETRO,
            verdict = BandVerdict.GOOD,
            strongestDbfs = -20.0,
            weakestDbfs = -30.0,
            inBandVariationDb = 10.0,
            noiseFloorDbfs = -60.0,
            minSnrDb = 20.0,
            reason = "Pass"
        ),
        stealthReport = BandReportSummary(
            band = Band.STEALTH,
            verdict = BandVerdict.POOR,
            strongestDbfs = -25.0,
            weakestDbfs = -40.0,
            inBandVariationDb = 15.0,
            noiseFloorDbfs = -65.0,
            minSnrDb = 15.0,
            reason = "Marginal"
        ),
        recommendedBand = Band.RETRO,
        recommendedSpeed = Speed.FAST,
        reasonCode = "RETRO_PASS",
        warnings = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSettings = FakeSettingsRepository()
        fakeReceiver = TestAcousticReceiver()
        fakeTransmitter = TestAcousticTransmitter()

        runner = CalibrationRunner(
            receiver = fakeReceiver,
            transmitter = fakeTransmitter,
            audioSpec = AudioSpec.RATE_48K,
            routeInfoProvider = {
                OutputRouteInfo(
                    volumeFraction = 0.9f,
                    hasHeadset = false,
                    hasBluetooth = false,
                    hasUsb = false,
                    nativeSampleRateHz = 48000
                )
            },
            dispatcher = testDispatcher
        )
        viewModel = CalibrationViewModel(runner, fakeSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateWithoutSavedCalibration() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(CalibrationStage.Idle, state.stage)
        assertNull(state.lastResult)
        assertEquals(false, state.recommendationApplied)
    }

    @Test
    fun testInitialStateWithSavedCalibration() = runTest {
        val repoWithSaved = FakeSettingsRepository(Settings(lastCalibration = sampleResult.toJson()))
        val vmWithSaved = CalibrationViewModel(runner, repoWithSaved)
        advanceUntilIdle()

        val state = vmWithSaved.uiState.value
        assertEquals(CalibrationStage.Idle, state.stage)
        assertNotNull(state.lastResult)
        assertEquals(Band.RETRO, state.lastResult?.recommendedBand)
        assertEquals(Speed.FAST, state.lastResult?.recommendedSpeed)
    }

    @Test
    fun testStartCalibrationCompletesAndPersistsResult() = runTest {
        advanceUntilIdle()
        viewModel.startCalibration()

        // Advance coroutines through calibration run
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Stage should be Completed, was ${state.stage}", state.stage is CalibrationStage.Completed)
        assertNotNull(state.lastResult)

        // Verify persistence to SettingsRepository
        val persistedJson = fakeSettings.settings.value.lastCalibration
        assertNotNull(persistedJson)
        val decoded = persistedJson?.let { CalibrationResult.fromJson(it) }
        assertNotNull(decoded)
        assertEquals(state.lastResult?.recommendedBand, decoded?.recommendedBand)
    }

    @Test
    fun testApplyRecommendationsUpdatesSettings() = runTest {
        fakeSettings.setLastCalibration(sampleResult.toJson())
        viewModel.loadLastResult()
        advanceUntilIdle()

        assertEquals(Settings.DEFAULT_BAND, fakeSettings.settings.value.band)
        assertEquals(Settings.DEFAULT_SPEED, fakeSettings.settings.value.speed)

        viewModel.applyRecommendations()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.recommendationApplied)
        assertEquals(Band.RETRO, fakeSettings.settings.value.band)
        assertEquals(Speed.FAST, fakeSettings.settings.value.speed)
    }

    @Test
    fun testCancelCalibrationResetsToIdle() = runTest {
        viewModel.startCalibration()
        viewModel.cancelCalibration()
        advanceUntilIdle()

        assertEquals(CalibrationStage.Idle, viewModel.uiState.value.stage)
    }
}
