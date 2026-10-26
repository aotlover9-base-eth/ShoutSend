package app.shoutsend.feature.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.Band
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
    val currentFragment: Int = 0,
    val totalFragments: Int = 0,
    val activeToneIndex: Int = -1,
    val audioSource: String = "MIC",
    val sampleRateHz: Int = 48000,
    val isListening: Boolean = false,
    val lastReceivedPayload: Payload? = null,
    val lastError: String? = null
)

/**
 * ViewModel managing acoustic listening, demodulator event processing,
 * and high-frame-rate waterfall streaming.
 */
class ReceiveViewModel(
    private val acousticReceiver: AcousticReceiver,
    private val audioCoordinator: AudioCoordinator,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val waterfallBuffer: WaterfallBuffer = WaterfallBuffer()

    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    private val _frameClock = MutableStateFlow(0L)
    val frameClock: StateFlow<Long> = _frameClock.asStateFlow()

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
                    lastError = null
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
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LISTENING,
                        isPreambleLocked = false,
                        activeToneIndex = -1
                    )
                }
            }
            is ReceiveEvent.PreambleDetected -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LOCKED,
                        isPreambleLocked = true
                    )
                }
            }
            is ReceiveEvent.Locked -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LOCKED,
                        isPreambleLocked = true,
                        snrDb = event.snrDb
                    )
                }
            }
            is ReceiveEvent.ModeDecoded -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.RECEIVING,
                        isPreambleLocked = true
                    )
                }
            }
            is ReceiveEvent.FrameDecoded -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.RECEIVING,
                        isPreambleLocked = true,
                        currentFragment = event.fragmentIndex + 1,
                        totalFragments = event.fragmentCount
                    )
                }
            }
            is ReceiveEvent.Progress -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.RECEIVING,
                        currentFragment = event.receivedFragments,
                        totalFragments = event.totalFragments
                    )
                }
            }
            is ReceiveEvent.MessageComplete -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.SUCCESS,
                        isPreambleLocked = false,
                        lastReceivedPayload = event.payload
                    )
                }
            }
            is ReceiveEvent.Lost -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LISTENING,
                        isPreambleLocked = false,
                        activeToneIndex = -1
                    )
                }
            }
            is ReceiveEvent.Cooldown -> {
                _uiState.update {
                    it.copy(
                        status = AcousticStatus.LISTENING,
                        isPreambleLocked = false,
                        activeToneIndex = -1
                    )
                }
            }
            is ReceiveEvent.FrameFailed -> {
                // Keep receiving, RS parity or repeat will correct
            }
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
