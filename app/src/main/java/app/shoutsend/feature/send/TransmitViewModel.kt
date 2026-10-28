package app.shoutsend.feature.send

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.feedback.AudioFeedbackPlayer
import app.shoutsend.core.feedback.TactileFeedbackController
import app.shoutsend.core.session.AudioRouteMonitor
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.modulator.TransmissionEnvelope
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state model for the TransmitScreen cyber-radio visual display.
 */
data class TransmitUiState(
    val transmitState: TransmitState = TransmitState.Idle,
    val progress: Float = 0.0f,
    val elapsedSeconds: Double = 0.0,
    val remainingSeconds: Double = 0.0,
    val currentFrame: Int = 1,
    val totalFrames: Int = 1,
    val currentRepeat: Int = 1,
    val repeats: Int = 2,
    val band: Band = Band.RETRO,
    val speed: Speed = Speed.NORMAL,
    val activeRms: Float = 0.0f,
    val activeToneIndex: Int = -1,
    val isLowVolume: Boolean = false,
    val routeWarning: String? = null,
    val isSavedToVault: Boolean = false,
    val isSecure: Boolean = false,
    val errorMessage: String? = null
) {
    val isTransmitting: Boolean
        get() = transmitState is TransmitState.Preparing || transmitState is TransmitState.Transmitting

    val isCompleted: Boolean
        get() = transmitState is TransmitState.Completed

    val isFailed: Boolean
        get() = transmitState is TransmitState.Failed

    val isCancelled: Boolean
        get() = transmitState is TransmitState.Cancelled
}

/**
 * ViewModel governing the transmit experience, soundwave ring synchronization,
 * countdown progress, hardware volume alerts, and history vault archiving.
 */
class TransmitViewModel(
    private val transmitSession: TransmitSession,
    private val settingsRepository: SettingsRepository,
    private val vaultRepository: VaultRepository,
    private val audioRouteMonitor: AudioRouteMonitor,
    applicationContext: Context? = null,
    private val audioManager: AudioManager? = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager,
    private val audioFeedbackPlayer: AudioFeedbackPlayer? = null,
    private val tactileFeedback: TactileFeedbackController? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransmitUiState())
    val uiState: StateFlow<TransmitUiState> = _uiState.asStateFlow()

    private var activeEnvelope: TransmissionEnvelope? = null
    private var lastSavedDurationMs: Long? = null
    private var isCurrentlyTransmitting = false
    private var lastObservedRepeat = 1

    init {
        // 1. Observe transmission state from modem session
        viewModelScope.launch {
            transmitSession.state.collectLatest { state ->
                handleTransmitState(state)
            }
        }

        // 2. Observe active envelope from session
        viewModelScope.launch {
            transmitSession.activeEnvelope.collectLatest { envelope ->
                activeEnvelope = envelope
            }
        }

        // 3. Observe audio routing peripheral warnings
        viewModelScope.launch {
            audioRouteMonitor.routeState.collectLatest { routeState ->
                _uiState.update { it.copy(routeWarning = routeState.transmitWarning) }
            }
        }

        // 4. Observe settings for band/speed profile defaults
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update { current ->
                    current.copy(
                        repeats = settings.repeats
                    )
                }
            }
        }

        // 5. Observe active encryption passphrase for secure mode badge
        viewModelScope.launch {
            transmitSession.activePassphrase.collectLatest { passphrase ->
                _uiState.update { it.copy(isSecure = !passphrase.isNullOrBlank()) }
            }
        }

        checkVolume()
    }

    private fun handleTransmitState(state: TransmitState) {
        val profile = transmitSession.activeProfile.value
        val effectiveBand = profile?.band ?: _uiState.value.band
        val effectiveSpeed = profile?.speed ?: _uiState.value.speed
        val effectiveRepeats = profile?.repeats ?: _uiState.value.repeats
        val effectiveIsSecure = !transmitSession.activePassphrase.value.isNullOrBlank()

        when (state) {
            is TransmitState.Idle -> {
                isCurrentlyTransmitting = false
                lastObservedRepeat = 1
                _uiState.update {
                    it.copy(
                        transmitState = state,
                        progress = 0f,
                        activeRms = 0f,
                        activeToneIndex = -1,
                        errorMessage = null
                    )
                }
            }
            is TransmitState.Preparing -> {
                isCurrentlyTransmitting = false
                lastObservedRepeat = 1
                _uiState.update {
                    it.copy(
                        transmitState = state,
                        progress = 0f,
                        activeRms = 0f,
                        activeToneIndex = -1,
                        errorMessage = null,
                        isSavedToVault = false,
                        band = effectiveBand,
                        speed = effectiveSpeed,
                        repeats = effectiveRepeats,
                        isSecure = effectiveIsSecure
                    )
                }
                checkVolume()
            }
            is TransmitState.Transmitting -> {
                if (!isCurrentlyTransmitting) {
                    isCurrentlyTransmitting = true
                    lastObservedRepeat = 1
                    audioFeedbackPlayer?.playTransmitStart()
                    tactileFeedback?.click()
                }

                val envelope = activeEnvelope ?: transmitSession.activeEnvelope.value
                val point = envelope?.getAtTime(state.elapsedSeconds)
                val rms = point?.rms ?: 0.5f
                val tone = point?.toneIndex ?: -1

                val currentRepeat = if (state.totalFrames > 0 && effectiveRepeats > 1) {
                    val frameInAll = (state.progress * effectiveRepeats * state.totalFrames).toInt()
                    (frameInAll / state.totalFrames).coerceIn(0, effectiveRepeats - 1) + 1
                } else {
                    1
                }

                if (currentRepeat > lastObservedRepeat) {
                    tactileFeedback?.tick()
                    lastObservedRepeat = currentRepeat
                }

                _uiState.update {
                    it.copy(
                        transmitState = state,
                        progress = state.progress,
                        elapsedSeconds = state.elapsedSeconds,
                        remainingSeconds = state.remainingSeconds,
                        currentFrame = state.currentFrame,
                        totalFrames = state.totalFrames,
                        currentRepeat = currentRepeat,
                        repeats = effectiveRepeats,
                        band = effectiveBand,
                        speed = effectiveSpeed,
                        isSecure = effectiveIsSecure,
                        activeRms = rms,
                        activeToneIndex = tone,
                        errorMessage = null
                    )
                }
            }
            is TransmitState.Completed -> {
                isCurrentlyTransmitting = false
                lastObservedRepeat = 1
                audioFeedbackPlayer?.playSuccess()
                tactileFeedback?.success()
                _uiState.update {
                    it.copy(
                        transmitState = state,
                        progress = 1.0f,
                        remainingSeconds = 0.0,
                        activeRms = 0.0f,
                        activeToneIndex = -1,
                        isSecure = effectiveIsSecure,
                        errorMessage = null
                    )
                }
                autoSaveToVaultIfEnabled(state.durationMs)
            }
            is TransmitState.Cancelled -> {
                isCurrentlyTransmitting = false
                lastObservedRepeat = 1
                _uiState.update {
                    it.copy(
                        transmitState = state,
                        activeRms = 0.0f,
                        activeToneIndex = -1
                    )
                }
            }
            is TransmitState.Failed -> {
                isCurrentlyTransmitting = false
                lastObservedRepeat = 1
                audioFeedbackPlayer?.playError()
                tactileFeedback?.error()
                val humanError = mapErrorMessage(state.reason)
                _uiState.update {
                    it.copy(
                        transmitState = state,
                        activeRms = 0.0f,
                        activeToneIndex = -1,
                        errorMessage = humanError
                    )
                }
            }
        }
    }

    private fun autoSaveToVaultIfEnabled(durationMs: Long) {
        if (lastSavedDurationMs == durationMs) return
        lastSavedDurationMs = durationMs

        viewModelScope.launch {
            val settings = settingsRepository.settings.value
            if (!settings.historyEnabled) return@launch

            val payload = transmitSession.activePayload.value ?: return@launch
            val profile = transmitSession.activeProfile.value
            val isSecure = !transmitSession.activePassphrase.value.isNullOrBlank()

            try {
                vaultRepository.save(
                    direction = Direction.SENT,
                    payload = payload,
                    profileSummary = "${profile?.band?.name ?: "RETRO"} · ${profile?.speed?.name ?: "NORMAL"}",
                    durationMs = durationMs,
                    snrDb = null,
                    isSecure = isSecure
                )
                _uiState.update { it.copy(isSavedToVault = true) }
            } catch (_: Exception) {
                // Non-fatal if vault persistence encounters temporary lock
            }
        }
    }

    fun checkVolume() {
        val manager = audioManager ?: return
        try {
            val currentVol = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) {
                val ratio = currentVol.toFloat() / maxVol.toFloat()
                _uiState.update { it.copy(isLowVolume = ratio < 0.70f) }
            }
        } catch (_: Exception) {}
    }

    fun openVolumePanel() {
        try {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (_: Exception) {}
    }

    fun cancelTransmission() {
        transmitSession.cancel("CancelledByUser")
    }

    fun retryTransmission() {
        val payload = transmitSession.activePayload.value ?: return
        val profile = transmitSession.activeProfile.value
        val passphrase = transmitSession.activePassphrase.value
        lastSavedDurationMs = null
        viewModelScope.launch {
            transmitSession.send(payload, profile, passphrase)
        }
    }

    private fun mapErrorMessage(reason: String): String {
        return when {
            reason.contains("PayloadTooLarge", ignoreCase = true) ->
                "Payload exceeds maximum acoustic length (320 bytes). Please shorten the content."
            reason.contains("AudioFocusDenied", ignoreCase = true) ->
                "Audio playback was blocked by another active sound app. Please close other media and retry."
            reason.contains("CoordinatorBusy", ignoreCase = true) ->
                "Acoustic receiver is currently active in another thread."
            reason.contains("Underrun", ignoreCase = true) ->
                "Hardware audio underrun occurred during acoustic synthesis."
            reason.contains("CancelledByUser", ignoreCase = true) ->
                "Transmission was stopped by user."
            else -> "Transmission failed: $reason"
        }
    }
}
