package app.shoutsend.feature.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.session.AudioRouteMonitor
import app.shoutsend.core.session.AudioRouteState
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.modulator.TransmissionEnvelope
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.WifiSecurity
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

private class PreviewTransmitSession(
    initialState: TransmitState = TransmitState.Idle,
    initialEnvelope: TransmissionEnvelope? = null,
    initialPayload: Payload? = null,
    initialProfile: ModemProfile? = null
) : TransmitSession {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<TransmitState> = _state.asStateFlow()
    override val activeEnvelope: StateFlow<TransmissionEnvelope?> = MutableStateFlow(initialEnvelope)
    override val activePayload: StateFlow<Payload?> = MutableStateFlow(initialPayload)
    override val activeProfile: StateFlow<ModemProfile?> = MutableStateFlow(initialProfile)
    override val activePassphrase: StateFlow<String?> = MutableStateFlow(null)

    override suspend fun send(
        payload: Payload,
        profile: ModemProfile?,
        securePassphrase: String?
    ): TransmitState = _state.value

    override fun cancel(reason: String?) {
        _state.value = TransmitState.Cancelled
    }
}

private class FakeVaultRepository : VaultRepository {
    override fun observeItems(): Flow<List<VaultItem>> = emptyFlow()
    override suspend fun save(
        direction: Direction,
        payload: Payload,
        profileSummary: String,
        durationMs: Long,
        snrDb: Float?,
        isSecure: Boolean
    ): Long = 1L
    override suspend fun delete(id: Long) {}
    override suspend fun deleteAll() {}
    override suspend fun setStarred(id: Long, isStarred: Boolean) {}
    override suspend fun prune(maxEntries: Int) {}
}

private class FakeAudioRouteMonitor : AudioRouteMonitor {
    override val routeState: StateFlow<AudioRouteState> = MutableStateFlow(AudioRouteState())
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

@Preview(name = "Send Screen - Wi-Fi Composer (Dark)", showBackground = true)
@Composable
fun SendScreenWifiPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession()
    val viewModel = SendViewModel(settingsRepo, session)
    viewModel.selectPayloadType(SendPayloadType.WIFI)
    viewModel.onWifiSsidChanged("Office_Guest", debounceMs = 0L)
    viewModel.onWifiPasswordChanged("SuperSecretPass123", debounceMs = 0L)

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

@Preview(name = "Send Screen - Contact Composer (Dark)", showBackground = true)
@Composable
fun SendScreenContactPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession()
    val viewModel = SendViewModel(settingsRepo, session)
    viewModel.selectPayloadType(SendPayloadType.CONTACT)
    viewModel.applyPickedContact(
        PickedContactData(
            displayName = "Faraday Researcher",
            phones = listOf("+1-555-0199", "555-0144"),
            email = "faraday@shoutsend.app"
        )
    )

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
        ),
        initialEnvelope = TransmissionEnvelope(20, floatArrayOf(0.7f), intArrayOf(3)),
        initialProfile = ModemProfile.DEFAULT_RETRO
    )
    val viewModel = TransmitViewModel(
        transmitSession = session,
        settingsRepository = settingsRepo,
        vaultRepository = FakeVaultRepository(),
        audioRouteMonitor = FakeAudioRouteMonitor()
    )

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

@Preview(name = "Transmit Screen - Completed Sent (Dark)", showBackground = true)
@Composable
fun TransmitScreenCompletedPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession(
        initialState = TransmitState.Completed(durationMs = 4500L),
        initialProfile = ModemProfile.DEFAULT_RETRO
    )
    val viewModel = TransmitViewModel(
        transmitSession = session,
        settingsRepository = settingsRepo,
        vaultRepository = FakeVaultRepository(),
        audioRouteMonitor = FakeAudioRouteMonitor()
    )

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

@Preview(name = "Transmit Screen - Failed Error (Dark)", showBackground = true)
@Composable
fun TransmitScreenFailedPreview() {
    val settingsRepo = FakeSettingsRepository()
    val session = PreviewTransmitSession(
        initialState = TransmitState.Failed("AudioFocusDenied"),
        initialProfile = ModemProfile.DEFAULT_RETRO
    )
    val viewModel = TransmitViewModel(
        transmitSession = session,
        settingsRepository = settingsRepo,
        vaultRepository = FakeVaultRepository(),
        audioRouteMonitor = FakeAudioRouteMonitor()
    )

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
