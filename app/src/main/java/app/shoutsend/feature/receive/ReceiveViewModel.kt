package app.shoutsend.feature.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the acoustic receiver and spectrogram waterfall screen.
 */
data class ReceiveUiState(
    val status: AcousticStatus = AcousticStatus.IDLE,
    val band: Band = Band.RETRO,
    val snrDb: Double = 0.0,
    val levelDbfs: Float = -90.0f,
    val peakDbfs: Float = -90.0f,
    val isClipping: Boolean = false,
    val isSilent: Boolean = false,
    val isPreambleLocked: Boolean = false,
    val isReadingHeader: Boolean = false,
    val currentFragment: Int = 0,
    val totalFragments: Int = 0,
    val estimatedRemainingSeconds: Double? = null,
    val activeToneIndex: Int = -1,
    val audioSource: String = "MIC",
    val sampleRateHz: Int = 48000,
    val isListening: Boolean = false,
    val hasAutoRepairedNoise: Boolean = false,
    val autoRepairedBytes: Int = 0,
    val lastReceivedPayload: Payload? = null,
    val savedVaultId: Long? = null,
    val isSavedToVault: Boolean = false,
    val isStarred: Boolean = false,
    val activeProfileSummary: String = "RETRO · NORMAL",
    val retryHint: String? = null,
    val lastError: String? = null
)

/**
 * ViewModel managing acoustic listening, demodulator event processing,
 * segmented reassembly progress, and automatic encrypted vault archiving.
 */
class ReceiveViewModel(
    private val acousticReceiver: AcousticReceiver,
    private val audioCoordinator: AudioCoordinator,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    val waterfallBuffer: WaterfallBuffer = WaterfallBuffer()

    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    private val _frameClock = MutableStateFlow(0L)
    val frameClock: StateFlow<Long> = _frameClock.asStateFlow()

    private var activeProfile: ModemProfile? = null
    private var receptionStartTimeMs: Long = 0L

    private var spectrumJob: Job? = null
    private var diagnosticsJob: Job? = null
    private var eventsJob: Job? = null
    private var settingsJob: Job? = null

    init {
        settingsJob = viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(band = settings.band) }
            }
        }
    }

    /**
     * Starts listening for acoustic data and initiates live spectrum waterfall streaming.
     */
    fun startListening() {
        if (_uiState.value.isListening) return

        viewModelScope.launch {
            val acquired = audioCoordinator.acquireReceiveLock()
            if (!acquired) {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.ERROR,
                        lastError = "Transmitter or calibration is currently active"
                    )
                }
                return@launch
            }

            val band = _uiState.value.band
            acousticReceiver.start(band = band)

            _uiState.update {
                it.copy(
                    isListening = true,
                    status = AcousticStatus.LISTENING,
                    lastError = null,
                    retryHint = null
                )
            }

            // Cancel any stale collection jobs
            spectrumJob?.cancel()
            diagnosticsJob?.cancel()
            eventsJob?.cancel()

            // 1. Collect spectrum rows and stream directly to waterfall buffer
            spectrumJob = launch {
                for (row in acousticReceiver.spectrumRows) {
                    waterfallBuffer.pushRow(row)
                    _frameClock.value = System.nanoTime()
                }
            }

            // 2. Collect hardware diagnostics
            diagnosticsJob = launch {
                acousticReceiver.diagnostics.collect { diag ->
                    _uiState.update { current ->
                        current.copy(
                            levelDbfs = diag.ambientLevelDbfs,
                            peakDbfs = diag.peakLevelDbfs,
                            isClipping = diag.isClipping,
                            isSilent = diag.isSilent,
                            audioSource = diag.sourceName,
                            sampleRateHz = diag.sampleRateHz
                        )
                    }
                }
            }

            // 3. Collect demodulator events
            eventsJob = launch {
                acousticReceiver.events.collect { event ->
                    handleReceiveEvent(event)
                }
            }
        }
    }

    /**
     * Updates UI state based on pipeline demodulator events.
     */
    private fun handleReceiveEvent(event: ReceiveEvent) {
        when (event) {
            is ReceiveEvent.Listening -> {
                if (_uiState.value.status != AcousticStatus.SUCCESS) {
                    _uiState.update {
                        it.copy(
                            status = AcousticStatus.LISTENING,
                            isPreambleLocked = false,
                            isReadingHeader = false,
                            activeToneIndex = -1
                        )
                    }
                }
            }
            is ReceiveEvent.PreambleDetected -> {
                receptionStartTimeMs = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LOCKED,
                        isPreambleLocked = true,
                        isReadingHeader = true,
                        retryHint = null,
                        hasAutoRepairedNoise = false,
                        autoRepairedBytes = 0
                    )
                }
            }
            is ReceiveEvent.Locked -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LOCKED,
                        isPreambleLocked = true,
                        isReadingHeader = true,
                        snrDb = event.snrDb
                    )
                }
            }
            is ReceiveEvent.ModeDecoded -> {
                activeProfile = event.profile
                val summary = "${event.profile.band.name} · ${event.profile.speed.name}"
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.RECEIVING,
                        isPreambleLocked = true,
                        activeProfileSummary = summary
                    )
                }
            }
            is ReceiveEvent.FrameDecoded -> {
                val fragmentCount = event.fragmentCount
                val fragmentIdx = event.fragmentIndex
                val profile = activeProfile
                val speedDurationMs = profile?.speed?.durationMs ?: 30
                val frameDurationSec = (124 * speedDurationMs) / 1000.0
                val remainingBlocks = maxOf(0, fragmentCount - (fragmentIdx + 1))
                val remainingSec = remainingBlocks * frameDurationSec

                _uiState.update { current ->
                    current.copy(
                        status = AcousticStatus.RECEIVING,
                        isPreambleLocked = true,
                        isReadingHeader = false,
                        currentFragment = fragmentIdx + 1,
                        totalFragments = fragmentCount,
                        estimatedRemainingSeconds = remainingSec,
                        hasAutoRepairedNoise = current.hasAutoRepairedNoise || (event.correctedBytes > 0),
                        autoRepairedBytes = current.autoRepairedBytes + event.correctedBytes
                    )
                }
            }
            is ReceiveEvent.Progress -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.RECEIVING,
                        isReadingHeader = false,
                        currentFragment = event.receivedFragments,
                        totalFragments = event.totalFragments
                    )
                }
            }
            is ReceiveEvent.MessageComplete -> {
                val durationMs = (System.currentTimeMillis() - receptionStartTimeMs).coerceAtLeast(100L)
                val payload = event.payload

                _uiState.update {
                    it.copy(
                        status = AcousticStatus.SUCCESS,
                        isPreambleLocked = false,
                        isReadingHeader = false,
                        estimatedRemainingSeconds = 0.0,
                        lastReceivedPayload = payload
                    )
                }

                // Auto-save to encrypted Vault if enabled in Settings (A4)
                if (payload != null) {
                    val settings = settingsRepository.settings.value
                    if (settings.historyEnabled) {
                        viewModelScope.launch {
                            try {
                                val isSecure = event.message.flags.isEncrypted
                                val summary = _uiState.value.activeProfileSummary
                                val savedId = vaultRepository.save(
                                    direction = Direction.RECEIVED,
                                    payload = payload,
                                    profileSummary = summary,
                                    durationMs = durationMs,
                                    snrDb = _uiState.value.snrDb.toFloat(),
                                    isSecure = isSecure
                                )
                                _uiState.update {
                                    it.copy(
                                        savedVaultId = savedId,
                                        isSavedToVault = true,
                                        isStarred = false
                                    )
                                }
                            } catch (_: Exception) {
                                // Non-fatal
                            }
                        }
                    }
                }
            }
            is ReceiveEvent.Lost -> {
                val hadLock = _uiState.value.isPreambleLocked || _uiState.value.currentFragment > 0
                _uiState.update {
                    it.copy(
                        status = if (hadLock) AcousticStatus.ERROR else AcousticStatus.LISTENING,
                        isPreambleLocked = false,
                        isReadingHeader = false,
                        retryHint = if (hadLock) "Move closer, raise the volume, try again" else null,
                        activeToneIndex = -1
                    )
                }
            }
            is ReceiveEvent.Cooldown -> {
                if (_uiState.value.status != AcousticStatus.SUCCESS) {
                    _uiState.update {
                        it.copy(
                            status = AcousticStatus.LISTENING,
                            isPreambleLocked = false,
                            isReadingHeader = false,
                            activeToneIndex = -1
                        )
                    }
                }
            }
            is ReceiveEvent.FrameFailed -> {
                val hadLock = _uiState.value.isPreambleLocked || _uiState.value.currentFragment > 0
                if (hadLock) {
                    _uiState.update {
                        it.copy(retryHint = "Move closer, raise the volume, try again")
                    }
                }
            }
        }
    }

    /**
     * Toggles one-tap star status for the newly received vault message.
     */
    fun toggleStar() {
        val vaultId = _uiState.value.savedVaultId ?: return
        val newStarred = !_uiState.value.isStarred
        _uiState.update { it.copy(isStarred = newStarred) }

        viewModelScope.launch {
            try {
                vaultRepository.setStarred(vaultId, newStarred)
            } catch (_: Exception) {
                // Revert on failure
                _uiState.update { it.copy(isStarred = !newStarred) }
            }
        }
    }

    /**
     * Resets received state and prepares receiver to listen for a new transmission.
     */
    fun resetReception() {
        _uiState.update {
            it.copy(
                status = if (it.isListening) AcousticStatus.LISTENING else AcousticStatus.IDLE,
                isPreambleLocked = false,
                isReadingHeader = false,
                currentFragment = 0,
                totalFragments = 0,
                estimatedRemainingSeconds = null,
                lastReceivedPayload = null,
                savedVaultId = null,
                isSavedToVault = false,
                isStarred = false,
                hasAutoRepairedNoise = false,
                autoRepairedBytes = 0,
                retryHint = null
            )
        }
    }

    /**
     * Sets active acoustic carrier band (Retro vs Stealth).
     */
    fun setBand(band: Band) {
        if (_uiState.value.band == band) return
        _uiState.update { it.copy(band = band) }
        waterfallBuffer.clear()
        if (_uiState.value.isListening) {
            acousticReceiver.stop()
            acousticReceiver.start(band = band)
        }
    }

    /**
     * Halts acoustic listening and releases the audio coordinator lock.
     */
    fun stopListening() {
        spectrumJob?.cancel()
        diagnosticsJob?.cancel()
        eventsJob?.cancel()
        spectrumJob = null
        diagnosticsJob = null
        eventsJob = null

        acousticReceiver.stop()
        audioCoordinator.releaseReceiveLock()

        _uiState.update {
            it.copy(
                isListening = false,
                status = AcousticStatus.IDLE,
                isPreambleLocked = false,
                isReadingHeader = false,
                activeToneIndex = -1
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        settingsJob?.cancel()
    }
}
