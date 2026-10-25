package app.shoutsend.feature.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.ModemProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class PreviewTransmitSession(
    initialState: TransmitState = TransmitState.Idle
) : TransmitSession {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<TransmitState> = _state.asStateFlow()

    override suspend fun send(
        payload: Payload,
        profile: ModemProfile?,
        securePassphrase: String?
    ): TransmitState = _state.value

    override fun cancel(reason: String?) {
        _state.value = TransmitState.Cancelled
    }
}

@Preview(name = "Send Screen - Text Composer (Dark)", showBackground = true)
@Composable
fun SendScreenTextPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession()
    val viewModel = SendViewModel(settingsRepo, session)
    viewModel.onTextChanged("Hello from acoustic cyber radio! ShoutSend emits soundwaves.", debounceMs = 0L)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            SendScreen(viewModel = viewModel, onNavigateToTransmit = {})
        }
    }
}

@Preview(name = "Send Screen - URL Composer (Dark)", showBackground = true)
@Composable
fun SendScreenUrlPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession()
    val viewModel = SendViewModel(settingsRepo, session)
    viewModel.selectPayloadType(SendPayloadType.URL)
    viewModel.onUrlChanged("https://shoutsend.app/download", debounceMs = 0L)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            SendScreen(viewModel = viewModel, onNavigateToTransmit = {})
        }
    }
}

@Preview(name = "Send Screen - Over Limit (Dark)", showBackground = true)
@Composable
fun SendScreenOverLimitPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession()
    val viewModel = SendViewModel(settingsRepo, session)
    val hugeText = "A".repeat(350)
    viewModel.onTextChanged(hugeText, debounceMs = 0L)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            SendScreen(viewModel = viewModel, onNavigateToTransmit = {})
        }
    }
}

@Preview(name = "Transmit Screen - Active Transmitting (Dark)", showBackground = true)
@Composable
fun TransmitScreenActivePreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession(
        initialState = TransmitState.Transmitting(
            progress = 0.45f,
            elapsedSeconds = 2.7,
            remainingSeconds = 3.3,
            currentFrame = 2,
            totalFrames = 4
        )
    )
    val viewModel = SendViewModel(settingsRepo, session)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            TransmitScreen(viewModel = viewModel, onNavigateBack = {})
        }
    }
}
