package app.shoutsend.core.session

import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.ModemProfile

/**
 * Metadata recorded upon successful message demodulation.
 */
data class ReceiveMetadata(
    val durationMs: Long,
    val snrDb: Double,
    val profile: ModemProfile,
    val isEncrypted: Boolean
)

/**
 * State machine hierarchy for acoustic reception sessions.
 */
sealed class ReceiveState {
    /** Receiver is idle / stopped. */
    object Idle : ReceiveState()

    /** Initializing microphone hardware and starting DSP pipelines. */
    object Starting : ReceiveState()

    /**
     * Actively listening for acoustic preamble.
     *
     * @param ambientDbfs Current background noise floor level in dBFS.
     */
    data class Listening(val ambientDbfs: Double) : ReceiveState()

    /** Preamble chirp energy detected in candidate band. */
    object PreambleDetected : ReceiveState()

    /**
     * Sync word locked and carrier frequency calibrated.
     *
     * @param snrDb Estimated signal-to-noise ratio in decibels.
     */
    data class Locked(val snrDb: Double) : ReceiveState()

    /**
     * Ingestion and decoding of physical data frames.
     *
     * @param receivedFragments Total successfully decoded fragments so far.
     * @param totalFragments Total expected fragments in message.
     * @param snrDb Current signal-to-noise ratio in decibels.
     */
    data class Receiving(
        val receivedFragments: Int,
        val totalFragments: Int,
        val snrDb: Double
    ) : ReceiveState()

    /**
     * Complete message assembled, decrypted, and unpacked.
     *
     * @param payload Typed domain payload ([Payload.Text], [Payload.Url], etc.).
     * @param metadata Physical link duration, SNR, profile, and encryption status.
     */
    data class Success(
        val payload: Payload,
        val metadata: ReceiveMetadata
    ) : ReceiveState()

    /**
     * Received an encrypted message requiring user passphrase entry.
     *
     * @param message Raw received ciphertext message.
     */
    data class NeedsPassphrase(val message: ReceivedMessage) : ReceiveState()

    /**
     * Demodulation failure or hardware error.
     *
     * @param reason Diagnostic reason (e.g., "PermissionMissing", "MicrophoneSilent", "InputTooLoud", "WrongPassphrase", "Lost", "Timeout").
     */
    data class Failed(val reason: String) : ReceiveState()
}
