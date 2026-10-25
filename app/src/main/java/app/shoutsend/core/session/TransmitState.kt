package app.shoutsend.core.session

/**
 * State machine hierarchy for acoustic transmission sessions.
 */
sealed class TransmitState {
    /** Transmitter is idle, awaiting payload. */
    object Idle : TransmitState()

    /** Validating, compacting, encrypting, and synthesizing audio plan. */
    object Preparing : TransmitState()

    /**
     * Active acoustic modulation through speaker.
     *
     * @param progress Normalized playback progress [0.0f, 1.0f].
     * @param elapsedSeconds Time elapsed since transmission start.
     * @param remainingSeconds Estimated time remaining to complete transmission.
     * @param currentFrame 1-based index of frame currently being modulated.
     * @param totalFrames Total frames in transmission plan.
     */
    data class Transmitting(
        val progress: Float,
        val elapsedSeconds: Double,
        val remainingSeconds: Double,
        val currentFrame: Int,
        val totalFrames: Int
    ) : TransmitState()

    /** Transmission modulated and played to completion. */
    data class Completed(val durationMs: Long) : TransmitState()

    /** Transmission was cancelled by user or coordinator. */
    object Cancelled : TransmitState()

    /** Transmission failed due to validation, audio track, or focus error. */
    data class Failed(val reason: String) : TransmitState()
}
