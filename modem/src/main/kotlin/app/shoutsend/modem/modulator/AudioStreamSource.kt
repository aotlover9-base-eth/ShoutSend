package app.shoutsend.modem.modulator

import app.shoutsend.modem.AudioSpec

/**
 * Common streaming audio abstraction for signal modulators and synthetic test tone sequences.
 */
public interface AudioStreamSource {
    /**
     * Target audio specification (sample rate and channels).
     */
    public val audioSpec: AudioSpec

    /**
     * Total stream duration in audio samples.
     */
    public val totalSamples: Long

    /**
     * Total stream duration in fractional seconds.
     */
    public val durationSeconds: Double

    /**
     * Renders normalized float audio samples in range `[-1.0f, 1.0f]` for
     * `[startSample, startSample + count)` directly into [out].
     *
     * Streaming capable, thread-safe, and zero-allocation.
     */
    public fun render(
        startSample: Long,
        out: FloatArray,
        outOffset: Int = 0,
        count: Int = out.size - outOffset
    )
}
