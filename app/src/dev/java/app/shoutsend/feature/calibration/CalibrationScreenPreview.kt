package app.shoutsend.feature.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.modem.calibration.BandVerdict
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.modulator.AudioStreamSource
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class PreviewReceiver : AcousticReceiver {
    override val events: SharedFlow<ReceiveEvent> = MutableSharedFlow()
    override val diagnostics: StateFlow<ReceiverDiagnostics> =
        MutableStateFlow(ReceiverDiagnostics()).asStateFlow()
    override val spectrumRows: ReceiveChannel<FloatArray> = Channel()
    override var rawAudioTap: ((ShortArray, Int, Int) -> Unit)? = null
    override val isRunning: Boolean = false
    override fun start(band: Band, passphrase: String?, tuning: ModemTuning) {}
    override fun stop() {}
}

private class PreviewTransmitter : AcousticTransmitter {
    override suspend fun transmit(
        source: AudioStreamSource,
        onProgress: (progress: TransmitProgress) -> Unit
    ): TransmitResult = TransmitResult.Success(100L, 500L, 0)
}

private fun createPreviewRunner(): CalibrationRunner = CalibrationRunner(
    receiver = PreviewReceiver(),
    transmitter = PreviewTransmitter(),
    routeInfoProvider = {
        OutputRouteInfo(
            volumeFraction = 0.8f,
            hasHeadset = false,
            hasBluetooth = false,
            hasUsb = false,
            nativeSampleRateHz = 48000
        )
    }
)

@Preview(name = "Calibration Screen - Dark (Completed)", showBackground = true)
@Composable
fun CalibrationScreenDarkPreview() {
    val sampleResult = CalibrationResult(
        timestampMs = 1700000000000L,
        retroReport = BandReportSummary(
            band = Band.RETRO,
            verdict = BandVerdict.GOOD,
            strongestDbfs = -18.2,
            weakestDbfs = -32.5,
            inBandVariationDb = 14.3,
            noiseFloorDbfs = -65.0,
            minSnrDb = 22.5,
            reason = "SNR exceeds 12 dB across all 16 bins"
        ),
        stealthReport = BandReportSummary(
            band = Band.STEALTH,
            verdict = BandVerdict.POOR,
            strongestDbfs = -28.0,
            weakestDbfs = -48.2,
            inBandVariationDb = 20.2,
            noiseFloorDbfs = -68.0,
            minSnrDb = 11.2,
            reason = "High transducer attenuation at 21 kHz"
        ),
        recommendedBand = Band.RETRO,
        recommendedSpeed = Speed.NORMAL,
        reasonCode = "RETRO_ROBUST",
        warnings = emptyList()
    )

    val fakeSettings = FakeSettingsRepository(Settings(lastCalibration = sampleResult.toJson()))
    val runner = createPreviewRunner()
    val viewModel = CalibrationViewModel(runner, fakeSettings)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            CalibrationScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
    }
}
