package app.shoutsend.modem.demod

import app.shoutsend.modem.packet.ReceivedMessage
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.profile.ModemProfile

/**
 * Strongly typed events emitted by [ReceiverPipeline] during acoustic reception.
 */
public sealed interface ReceiveEvent {

    /**
     * Emitted periodically in the HUNTING state reporting current background ambient noise level in dBFS.
     */
    public data class Listening(public val ambientDbfs: Double) : ReceiveEvent

    /**
     * Emitted when a candidate alternating sync preamble pattern is detected.
     */
    public object PreambleDetected : ReceiveEvent

    /**
     * Emitted when the 8-symbol sync word is verified and fine symbol boundary timing is locked.
     */
    public data class Locked(public val snrDb: Double) : ReceiveEvent

    /**
     * Emitted when the transmission mode block is successfully decoded.
     */
    public data class ModeDecoded(public val profile: ModemProfile) : ReceiveEvent

    /**
     * Emitted when an individual physical frame is Reed-Solomon corrected and CRC-verified.
     */
    public data class FrameDecoded(
        public val fragmentIndex: Int,
        public val fragmentCount: Int,
        public val correctedBytes: Int
    ) : ReceiveEvent

    /**
     * Emitted when a frame fails Reed-Solomon decoding or CRC verification.
     */
    public data class FrameFailed(public val reason: String) : ReceiveEvent

    /**
     * Emitted as distinct fragments of a multi-fragment message are received.
     */
    public data class Progress(public val receivedFragments: Int, public val totalFragments: Int) : ReceiveEvent

    /**
     * Emitted upon successful reassembly and optional payload decoding of a complete transmission.
     */
    public data class MessageComplete(
        public val message: ReceivedMessage,
        public val payload: Payload? = null
    ) : ReceiveEvent

    /**
     * Emitted when synchronization or signal tracking is lost.
     */
    public data class Lost(public val reason: String) : ReceiveEvent

    /**
     * Emitted when the receiver enters the post-message refractory cooldown period (300 ms).
     */
    public object Cooldown : ReceiveEvent
}

/**
 * Preallocated listener callback receiving pipeline events with zero heap allocations on the audio path.
 */
public fun interface ReceiveEventListener {
    public fun onReceiveEvent(event: ReceiveEvent)
}
