package app.shoutsend.core.audio.receiver

/**
 * Real-time diagnostic metrics reported by the acoustic receiver.
 *
 * @property ambientLevelDbfs Filtered ambient background noise level in dBFS [-90.0 .. 0.0].
 * @property peakLevelDbfs Instantaneous peak amplitude level in dBFS [-90.0 .. 0.0].
 * @property isClipping True if >0.1% samples in the last 500ms reached within 1% of full-scale clipping.
 * @property isSilent True if digital zero silence has persisted for 2.0+ seconds.
 * @property overrunCount Cumulative audio frames dropped by the ring buffer.
 * @property sourceName Active hardware audio capture source (e.g. UNPROCESSED, VOICE_RECOGNITION, MIC).
 * @property sampleRateHz Actual active capture sample rate (48000 or 44100).
 * @property effects Hardware acoustic echo canceler, noise suppressor, and AGC status.
 */
data class ReceiverDiagnostics(
    val ambientLevelDbfs: Float = -90.0f,
    val peakLevelDbfs: Float = -90.0f,
    val isClipping: Boolean = false,
    val isSilent: Boolean = false,
    val overrunCount: Long = 0L,
    val sourceName: String = "UNKNOWN",
    val sampleRateHz: Int = 48000,
    val effects: AudioEffectsState = AudioEffectsState()
)
