package app.shoutsend.feature.receive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.core.session.SessionMode
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

private class PreviewAcousticReceiver : AcousticReceiver {
    private val _events = MutableSharedFlow<ReceiveEvent>()
    override val events: SharedFlow<ReceiveEvent> = _events

    private val _diagnostics = MutableStateFlow(
        ReceiverDiagnostics(
            ambientLevelDbfs = -28.0f,
            peakLevelDbfs = -14.0f,
            sourceName = "UNPROCESSED / MIC",
            sampleRateHz = 48000
        )
    )
    override val diagnostics: StateFlow<ReceiverDiagnostics> = _diagnostics

    private val _spectrumChannel = Channel<FloatArray>(capacity = 60)
    override val spectrumRows: ReceiveChannel<FloatArray> = _spectrumChannel

    override var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)? = null
    override val isRunning: Boolean = true

    override fun start(band: Band, passphrase: String?, tuning: ModemTuning) {}
    override fun stop() {}
}

private class PreviewAudioCoordinator : AudioCoordinator {
    override val activeMode: StateFlow<SessionMode> = MutableStateFlow(SessionMode.RECEIVING)
    override val keepScreenOn: StateFlow<Boolean> = MutableStateFlow(true)
    override suspend fun acquireTransmitLock(): Boolean = true
    override fun releaseTransmitLock() {}
    override suspend fun acquireReceiveLock(): Boolean = true
    override fun releaseReceiveLock() {}
    override suspend fun acquireCalibrationLock(): Boolean = true
    override fun releaseCalibrationLock() {}
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
    ): Long = 101L

    override suspend fun delete(id: Long) {}
    override suspend fun deleteAll() {}
    override suspend fun setStarred(id: Long, isStarred: Boolean) {}
    override suspend fun prune(maxEntries: Int) {}
}

@Preview(name = "Receive Screen - Listening (Dark)", showBackground = true)
@Composable
fun ReceiveScreenListeningPreview() {
    val settingsRepo = FakeSettingsRepository()
    val receiver = PreviewAcousticReceiver()
    val coordinator = PreviewAudioCoordinator()
    val vault = FakeVaultRepository()
    val viewModel = ReceiveViewModel(receiver, coordinator, settingsRepo, vault)

    val mockRow = FloatArray(256) { idx ->
        if (idx in 50..180) (0.25f + (idx % 16) * 0.04f).coerceIn(0.0f, 1.0f) else 0.05f
    }
    repeat(40) {
        viewModel.waterfallBuffer.pushRow(mockRow)
    }

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            ReceiveScreen(viewModel = viewModel)
        }
    }
}

@Preview(name = "Receive Screen - In-Flight Reassembly (Dark)", showBackground = true)
@Composable
fun ReceiveScreenReassemblyPreview() {
    val settingsRepo = FakeSettingsRepository()
    val receiver = PreviewAcousticReceiver()
    val coordinator = PreviewAudioCoordinator()
    val vault = FakeVaultRepository()
    val viewModel = ReceiveViewModel(receiver, coordinator, settingsRepo, vault)

    val lockedRow = FloatArray(256) { idx ->
        if (idx in 80..95) 0.95f else 0.10f
    }
    repeat(60) {
        viewModel.waterfallBuffer.pushRow(lockedRow)
    }

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            ReceiveScreen(viewModel = viewModel)
        }
    }
}

@Preview(name = "Receive Screen - Success Payload Card (Dark)", showBackground = true)
@Composable
fun ReceiveScreenSuccessPreview() {
    val settingsRepo = FakeSettingsRepository()
    val receiver = PreviewAcousticReceiver()
    val coordinator = PreviewAudioCoordinator()
    val vault = FakeVaultRepository()
    val viewModel = ReceiveViewModel(receiver, coordinator, settingsRepo, vault)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            ReceiveScreen(viewModel = viewModel)
        }
    }
}

@Preview(name = "Action Card - Wi-Fi Credential", showBackground = true)
@Composable
fun ActionCardWiFiPreview() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .background(ChirpTheme.colors.background)
        ) {
            ReceivedPayloadCard(
                payload = Payload.WiFi(
                    ssid = "CyberRadio-Guest",
                    password = "SecretPassphrase123",
                    security = app.shoutsend.modem.payload.WifiSecurity.WPA2
                ),
                snrDb = 22.4,
                profileSummary = "Retro Normal",
                isStarred = false,
                isSavedToVault = true,
                hasAutoRepairedNoise = true,
                onToggleStar = {},
                onReceiveAnother = {}
            )
        }
    }
}

@Preview(name = "Action Card - Contact Card", showBackground = true)
@Composable
fun ActionCardContactPreview() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .background(ChirpTheme.colors.background)
        ) {
            ReceivedPayloadCard(
                payload = Payload.Contact(
                    displayName = "Alex Rivera",
                    phones = listOf("+15550192834", "+15550199999"),
                    email = "alex.rivera@example.com"
                ),
                snrDb = 19.8,
                profileSummary = "Retro Normal",
                isStarred = true,
                isSavedToVault = true,
                hasAutoRepairedNoise = false,
                onToggleStar = {},
                onReceiveAnother = {}
            )
        }
    }
}

@Preview(name = "Action Card - Web Link HTTP Warning", showBackground = true)
@Composable
fun ActionCardUrlWarningPreview() {
    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .background(ChirpTheme.colors.background)
        ) {
            ReceivedPayloadCard(
                payload = Payload.Url("http://insecure-domain.org/data?id=42"),
                snrDb = 24.1,
                profileSummary = "Stealth Fast",
                isStarred = false,
                isSavedToVault = true,
                hasAutoRepairedNoise = false,
                onToggleStar = {},
                onReceiveAnother = {}
            )
        }
    }
}
