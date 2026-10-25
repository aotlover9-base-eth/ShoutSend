package app.shoutsend.core.session

import app.shoutsend.core.audio.transmitter.AcousticTransmitter
import app.shoutsend.core.audio.transmitter.TransmitResult
import app.shoutsend.core.modem.ModemConfigProvider
import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.crypto.SecureBox
import app.shoutsend.modem.modulator.Modulator
import app.shoutsend.modem.modulator.OutgoingMessage
import app.shoutsend.modem.modulator.TransmissionBuilder
import app.shoutsend.modem.modulator.TransmissionEnvelope
import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCodec
import app.shoutsend.modem.payload.PayloadCompactor
import app.shoutsend.modem.payload.PayloadValidationResult
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.protocol.ProtocolConstants
import app.shoutsend.modem.tuning.ModemTuning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-level session contract managing acoustic payload transmission.
 */
interface TransmitSession {
    val state: StateFlow<TransmitState>
    val activeEnvelope: StateFlow<TransmissionEnvelope?>
    val activePayload: StateFlow<Payload?>
    val activeProfile: StateFlow<ModemProfile?>
    val activePassphrase: StateFlow<String?>

    /**
     * Compacts, encrypts (if passphrase provided), modulates, and transmits [payload].
     */
    suspend fun send(
        payload: Payload,
        profile: ModemProfile? = null,
        securePassphrase: String? = null
    ): TransmitState

    /**
     * Cancels active transmission immediately.
     */
    fun cancel(reason: String? = null)
}

/**
 * Production implementation of [TransmitSession].
 */
class DefaultTransmitSession(
    private val coordinator: AudioCoordinator,
    private val focusController: AudioFocusController,
    private val transmitter: AcousticTransmitter,
    private val audioSpec: AudioSpec = AudioSpec.RATE_48K,
    private val configProvider: ModemConfigProvider? = null
) : TransmitSession {

    private val _state = MutableStateFlow<TransmitState>(TransmitState.Idle)
    override val state: StateFlow<TransmitState> = _state.asStateFlow()

    private val _activeEnvelope = MutableStateFlow<TransmissionEnvelope?>(null)
    override val activeEnvelope: StateFlow<TransmissionEnvelope?> = _activeEnvelope.asStateFlow()

    private val _activePayload = MutableStateFlow<Payload?>(null)
    override val activePayload: StateFlow<Payload?> = _activePayload.asStateFlow()

    private val _activeProfile = MutableStateFlow<ModemProfile?>(null)
    override val activeProfile: StateFlow<ModemProfile?> = _activeProfile.asStateFlow()

    private val _activePassphrase = MutableStateFlow<String?>(null)
    override val activePassphrase: StateFlow<String?> = _activePassphrase.asStateFlow()

    @Volatile
    private var activePlaybackJob: Job? = null
    @Volatile
    private var cancellationReason: String? = null

    override suspend fun send(
        payload: Payload,
        profile: ModemProfile?,
        securePassphrase: String?
    ): TransmitState = withContext(Dispatchers.Default) {
        _state.value = TransmitState.Preparing
        cancellationReason = null

        // 1. Enforce payload validation and size limits
        val validation = payload.validate()
        if (validation is PayloadValidationResult.Invalid) {
            val isSizeExceeded = validation.error.reason.contains("exceeds", ignoreCase = true)
            val failed = TransmitState.Failed(if (isSizeExceeded) "PayloadTooLarge" else "ValidationFailed: ${validation.error.reason}")
            _state.value = failed
            return@withContext failed
        }

        // 2. Encode canonical payload bytes
        val canonicalBytes = try {
            PayloadCodec.encode(payload)
        } catch (e: Exception) {
            val failed = TransmitState.Failed("EncodingFailed: ${e.message}")
            _state.value = failed
            return@withContext failed
        }

        // 3. Domain-specific compaction (Step 14)
        val compactResult = PayloadCompactor.compact(payload)
        val isCompact = compactResult.isCompact
        var dataBytes = compactResult.bytes

        // 4. Optional PBKDF2/AES-GCM encryption (Step 15)
        val isEncrypted = !securePassphrase.isNullOrBlank()
        if (isEncrypted) {
            try {
                dataBytes = SecureBox.seal(
                    plainBytes = dataBytes,
                    type = payload.messageType,
                    passphrase = securePassphrase!!.toCharArray()
                )
            } catch (e: Exception) {
                val failed = TransmitState.Failed("EncryptionFailed: ${e.message}")
                _state.value = failed
                return@withContext failed
            }
        }

        val flags = MessageFlags(
            isEncrypted = isEncrypted,
            isCompact = isCompact,
            isReserved = false
        )

        // 5. Enforce 320-byte wire limit (Step 12)
        if (dataBytes.size > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES) {
            val failed = TransmitState.Failed("PayloadTooLarge")
            _state.value = failed
            return@withContext failed
        }

        // 6. Acquire half-duplex transmit lock from coordinator
        val lockAcquired = coordinator.acquireTransmitLock()
        if (!lockAcquired) {
            val failed = TransmitState.Failed("CoordinatorBusy")
            _state.value = failed
            return@withContext failed
        }

        // 7. Request transient audio focus
        val focusGranted = focusController.requestFocus { reason ->
            cancel(reason)
        }
        if (!focusGranted) {
            coordinator.releaseTransmitLock()
            val failed = TransmitState.Failed("AudioFocusDenied")
            _state.value = failed
            return@withContext failed
        }

        try {
            // 8. Assemble transmission plan and modulator
            val currentConfig = configProvider?.config?.value
            val effectiveProfile = profile ?: currentConfig?.profile ?: ModemProfile.DEFAULT_RETRO
            val effectiveTuning = currentConfig?.tuning ?: ModemTuning.DEFAULT

            val message = OutgoingMessage(
                type = payload.messageType,
                flags = flags,
                payload = dataBytes
            )
            val plan = TransmissionBuilder.build(
                message = message,
                profile = effectiveProfile,
                audioSpec = audioSpec,
                tuning = effectiveTuning
            )
            val envelope = TransmissionEnvelope.compute(plan)
            _activePayload.value = payload
            _activeProfile.value = effectiveProfile
            _activePassphrase.value = securePassphrase
            _activeEnvelope.value = envelope
            val modulator = Modulator(plan)
            val totalFrames = plan.frames.size

            // Launch playback in child coroutine so cancellation stops playback without aborting send()
            var transmitResult: TransmitResult? = null
            coroutineScope {
                val playJob = launch {
                    transmitResult = transmitter.transmit(modulator) { prog ->
                        val currentFrame = (prog.fraction * totalFrames).toInt().coerceIn(0, totalFrames - 1) + 1
                        _state.value = TransmitState.Transmitting(
                            progress = prog.fraction,
                            elapsedSeconds = prog.elapsedSeconds,
                            remainingSeconds = prog.remainingSeconds,
                            currentFrame = currentFrame,
                            totalFrames = totalFrames
                        )
                    }
                }
                activePlaybackJob = playJob
                playJob.join()
            }

            val reason = cancellationReason
            val finalState = if (reason != null) {
                if (reason == "CancelledByUser") TransmitState.Cancelled else TransmitState.Failed(reason)
            } else {
                when (val res = transmitResult) {
                    is TransmitResult.Success -> TransmitState.Completed(res.durationMs)
                    is TransmitResult.Cancelled -> TransmitState.Cancelled
                    is TransmitResult.Failure -> TransmitState.Failed(res.error.message ?: "PlaybackError")
                    null -> TransmitState.Cancelled
                }
            }
            _state.value = finalState
            return@withContext finalState

        } catch (e: CancellationException) {
            val reason = cancellationReason
            val finalState = if (reason != null && reason != "CancelledByUser") {
                TransmitState.Failed(reason)
            } else {
                TransmitState.Cancelled
            }
            _state.value = finalState
            return@withContext finalState
        } catch (e: Throwable) {
            val failed = TransmitState.Failed(e.message ?: "UnknownTransmissionError")
            _state.value = failed
            return@withContext failed
        } finally {
            activePlaybackJob = null
            focusController.abandonFocus()
            coordinator.releaseTransmitLock()
        }
    }

    override fun cancel(reason: String?) {
        val actualReason = reason ?: "CancelledByUser"
        cancellationReason = actualReason
        activePlaybackJob?.cancel(CancellationException(actualReason))
        activePlaybackJob = null
        _state.value = if (actualReason != "CancelledByUser") {
            TransmitState.Failed(actualReason)
        } else {
            TransmitState.Cancelled
        }
    }
}
