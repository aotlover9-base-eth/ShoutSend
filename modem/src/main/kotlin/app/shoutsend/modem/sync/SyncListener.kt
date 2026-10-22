package app.shoutsend.modem.sync

/**
 * Callbacks emitted by [SyncDetector] during transmission acquisition and tracking.
 * Zero-allocation interface.
 */
public interface SyncListener {

    /**
     * Emitted when a candidate alternating preamble sequence is detected.
     */
    public fun onPreambleCandidate(sampleIndex: Long, confidence: Float) {}

    /**
     * Emitted when the 8-symbol sync word is verified and exact symbol timing is locked.
     *
     * @param symbolStartSample Absolute sample index corresponding to the start of the mode block.
     * @param estimatedSnrDb Estimated signal-to-noise ratio in decibels.
     */
    public fun onSyncLocked(symbolStartSample: Long, estimatedSnrDb: Float) {}

    /**
     * Emitted when synchronization is lost.
     */
    public fun onSyncLost(sampleIndex: Long, reason: String) {}
}
