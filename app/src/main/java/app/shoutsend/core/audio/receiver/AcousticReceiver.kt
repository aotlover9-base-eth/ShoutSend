package app.shoutsend.core.audio.receiver

import app.shoutsend.modem.demod.ReceiveEvent
import app.shoutsend.modem.profile.Band
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level acoustic receiver coordinating hardware microphone capture,
 * lock-free streaming buffering, live spectrum analysis, and the demodulator pipeline.
 */
interface AcousticReceiver {
    /**
     * Emitted modem receive events (PreambleDetected, Locked, ModeDecoded, FrameDecoded, MessageComplete, Lost, Cooldown).
     */
    val events: SharedFlow<ReceiveEvent>

    /**
     * Real-time hardware and signal diagnostics updated continuously.
     */
    val diagnostics: StateFlow<ReceiverDiagnostics>

    /**
     * Fast preallocated channel emitting 60 fps normalized spectrum rows for the waterfall UI.
     */
    val spectrumRows: ReceiveChannel<FloatArray>

    /**
     * Optional raw PCM audio tap invoked for calibration recording and diagnostics.
     */
    var rawAudioTap: ((samples: ShortArray, offset: Int, count: Int) -> Unit)?

    /**
     * Whether audio capture and processing is currently active.
     */
    val isRunning: Boolean

    /**
     * Starts acoustic listening on [band] with optional [passphrase] for encrypted transmissions.
     * Idempotent: if already active with identical configuration, does not restart.
     */
    fun start(band: Band = Band.RETRO, passphrase: String? = null)

    /**
     * Stops audio capture and processing cleanly, releasing all hardware resources.
     * Idempotent.
     */
    fun stop()
}
