package app.shoutsend.core.session

import app.shoutsend.core.audio.receiver.AcousticReceiver
import app.shoutsend.core.audio.receiver.ReceiverDiagnostics
import app.shoutsend.modem.crypto.SecureBox
import app.shoutsend.modem.crypto.SecureOpenResult
import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.PayloadCompactor
import app.shoutsend.modem.payload.PayloadDecodeResult
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-level session contract managing acoustic reception, demodulator state mapping,
 * and live waterfall spectrum feeds.
 */
interface ReceiveSession {
    /** Current reception state. */
    val state: StateFlow<ReceiveState>

    /** Real-time microphone capture diagnostics. */
    val diagnostics: StateFlow<ReceiverDiagnostics>

    /** 60 fps normalized waterfall spectrum rows. */
    val spectrumRows: ReceiveChannel<FloatArray>

    /** True when background media is playing on device, suggesting interference. */
    val isMusicActiveWarning: StateFlow<Boolean>

    /**
     * Starts listening on [band].
     *
     * @param passphraseProvider Optional asynchronous resolver for decrypting secure messages.
     */
    suspend fun start(
        band: Band = Band.RETRO,
        passphraseProvider: (suspend (message: ReceivedMessage) -> String?)? = null
    )

    /**
     * Unlocks an encrypted message if the session is currently in [ReceiveState.NeedsPassphrase].
     *
     * @param passphrase Passphrase provided by user.
     * @return True if message was authenticated and decrypted successfully.
     */
    fun submitPassphrase(passphrase: String): Boolean

    /**
     * Stops acoustic listening cleanly. Idempotent.
     */
    fun stop()
}

/**
 * Production implementation of [ReceiveSession].
 */
class DefaultReceiveSession(
    private val coordinator: AudioCoordinator,
    private val focusController: AudioFocusController,
    private val receiver: AcousticReceiver,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) : ReceiveSession {

    private val _state = MutableStateFlow<ReceiveState>(ReceiveState.Idle)
    override val state: StateFlow<ReceiveState> = _state.asStateFlow()

    override val diagnostics: StateFlow<ReceiverDiagnostics> = receiver.diagnostics
    override val spectrumRows: ReceiveChannel<FloatArray> = receiver.spectrumRows

    private val _isMusicActiveWarning = MutableStateFlow(false)
    override val isMusicActiveWarning: StateFlow<Boolean> = _isMusicActiveWarning.asStateFlow()

    private var eventCollectionJob: Job? = null
    private var activeProfile: ModemProfile = ModemProfile.DEFAULT_RETRO
    private var lastSnrDb: Double = 0.0
    private var receptionStartTime: Long = 0L

    override suspend fun start(
        band: Band,
        passphraseProvider: (suspend (message: ReceivedMessage) -> String?)?
    ) = withContext(Dispatchers.Default) {
        if (_state.value != ReceiveState.Idle && receiver.isRunning) {
            return@withContext // Idempotent start
        }

        val acquired = coordinator.acquireReceiveLock()
        if (!acquired) {
            _state.value = ReceiveState.Failed("CoordinatorBusy")
            return@withContext
        }

        _state.value = ReceiveState.Starting
        _isMusicActiveWarning.value = focusController.isMusicActive
        activeProfile = if (band == Band.STEALTH) ModemProfile.DEFAULT_STEALTH else ModemProfile.DEFAULT_RETRO

        eventCollectionJob?.cancel()
        eventCollectionJob = scope.launch(Dispatchers.Default) {
            receiver.events.collect { event ->
                handleReceiveEvent(event, passphraseProvider)
            }
        }

        try {
            receiver.start(band = band)
            _state.value = ReceiveState.Listening(diagnostics.value.ambientLevelDbfs.toDouble())
        } catch (e: Exception) {
            _state.value = ReceiveState.Failed(e.message ?: "InitFailed")
            stop()
        }
    }

    private suspend fun handleReceiveEvent(
        event: ReceiveEvent,
        passphraseProvider: (suspend (message: ReceivedMessage) -> String?)?
    ) {
        when (event) {
            is ReceiveEvent.Listening -> {
                if (_state.value is ReceiveState.Starting || _state.value is ReceiveState.Idle) {
                    _state.value = ReceiveState.Listening(diagnostics.value.ambientLevelDbfs.toDouble())
                }
            }

            is ReceiveEvent.PreambleDetected -> {
                receptionStartTime = System.currentTimeMillis()
                _state.value = ReceiveState.PreambleDetected
            }

            is ReceiveEvent.Locked -> {
                lastSnrDb = event.snrDb
                _state.value = ReceiveState.Locked(event.snrDb)
            }

            is ReceiveEvent.ModeDecoded -> {
                activeProfile = event.profile
            }

            is ReceiveEvent.FrameDecoded -> {
                _state.value = ReceiveState.Receiving(
                    receivedFragments = event.fragmentIndex + 1,
                    totalFragments = event.fragmentCount,
                    snrDb = lastSnrDb
                )
            }

            is ReceiveEvent.FrameFailed -> {
                val current = _state.value
                if (current !is ReceiveState.Success && current !is ReceiveState.NeedsPassphrase) {
                    _state.value = ReceiveState.Failed(event.reason)
                }
            }

            is ReceiveEvent.Lost -> {
                val current = _state.value
                if (current !is ReceiveState.Success && current !is ReceiveState.NeedsPassphrase) {
                    _state.value = ReceiveState.Failed(event.reason)
                }
            }

            is ReceiveEvent.MessageComplete -> {
                val durationMs = if (receptionStartTime > 0L) {
                    System.currentTimeMillis() - receptionStartTime
                } else 0L

                val msg = event.message
                val decodedPayload = event.payload
                if (decodedPayload != null) {
                    // Plaintext message successfully parsed
                    _state.value = ReceiveState.Success(
                        payload = decodedPayload,
                        metadata = ReceiveMetadata(
                            durationMs = durationMs,
                            snrDb = lastSnrDb,
                            profile = activeProfile,
                            isEncrypted = msg.flags.isEncrypted
                        )
                    )
                } else if (msg.flags.isEncrypted) {
                    // Encrypted message: query passphraseProvider if available
                    if (passphraseProvider != null) {
                        val passphrase = passphraseProvider(msg)
                        if (!passphrase.isNullOrBlank()) {
                            val openResult = SecureBox.open(
                                sealedBytes = msg.payload,
                                type = msg.type,
                                passphrase = passphrase.toCharArray()
                            )
                            when (openResult) {
                                is SecureOpenResult.Opened -> {
                                    val expandResult = PayloadCompactor.expand(
                                        bytes = openResult.plainBytes,
                                        isCompact = msg.flags.isCompact,
                                        type = msg.type
                                    )
                                    when (expandResult) {
                                        is PayloadDecodeResult.Success -> {
                                            _state.value = ReceiveState.Success(
                                                payload = expandResult.payload,
                                                metadata = ReceiveMetadata(
                                                    durationMs = durationMs,
                                                    snrDb = lastSnrDb,
                                                    profile = activeProfile,
                                                    isEncrypted = true
                                                )
                                            )
                                        }
                                        else -> {
                                            _state.value = ReceiveState.Failed("PayloadCorrupt")
                                        }
                                    }
                                }
                                is SecureOpenResult.WrongPassphraseOrTampered -> {
                                    _state.value = ReceiveState.Failed("WrongPassphrase")
                                }
                                is SecureOpenResult.Malformed -> {
                                    _state.value = ReceiveState.Failed("MalformedCiphertext")
                                }
                            }
                        } else {
                            _state.value = ReceiveState.NeedsPassphrase(msg)
                        }
                    } else {
                        _state.value = ReceiveState.NeedsPassphrase(msg)
                    }
                } else {
                    _state.value = ReceiveState.Failed("PayloadCorrupt")
                }
            }

            is ReceiveEvent.Cooldown -> {
                // Post-message cooldown: do not overwrite terminal or interactive states
                val current = _state.value
                if (current !is ReceiveState.Success &&
                    current !is ReceiveState.NeedsPassphrase &&
                    current !is ReceiveState.Failed
                ) {
                    _state.value = ReceiveState.Listening(diagnostics.value.ambientLevelDbfs.toDouble())
                }
            }

            else -> {}
        }
    }

    override fun submitPassphrase(passphrase: String): Boolean {
        val currentState = _state.value
        if (currentState !is ReceiveState.NeedsPassphrase) return false
        val msg = currentState.message
        val openResult = SecureBox.open(
            sealedBytes = msg.payload,
            type = msg.type,
            passphrase = passphrase.toCharArray()
        )
        return when (openResult) {
            is SecureOpenResult.Opened -> {
                val expandResult = PayloadCompactor.expand(
                    bytes = openResult.plainBytes,
                    isCompact = msg.flags.isCompact,
                    type = msg.type
                )
                if (expandResult is PayloadDecodeResult.Success) {
                    val durationMs = if (receptionStartTime > 0L) {
                        System.currentTimeMillis() - receptionStartTime
                    } else 0L
                    _state.value = ReceiveState.Success(
                        payload = expandResult.payload,
                        metadata = ReceiveMetadata(
                            durationMs = durationMs,
                            snrDb = lastSnrDb,
                            profile = activeProfile,
                            isEncrypted = true
                        )
                    )
                    true
                } else {
                    _state.value = ReceiveState.Failed("PayloadCorrupt")
                    false
                }
            }
            is SecureOpenResult.WrongPassphraseOrTampered -> {
                _state.value = ReceiveState.Failed("WrongPassphrase")
                false
            }
            is SecureOpenResult.Malformed -> {
                _state.value = ReceiveState.Failed("MalformedCiphertext")
                false
            }
        }
    }

    override fun stop() {
        eventCollectionJob?.cancel()
        eventCollectionJob = null
        receiver.stop()
        coordinator.releaseReceiveLock()
        _state.value = ReceiveState.Idle
    }
}
