package app.shoutsend.core.audio.transmitter

/**
 * Abstraction for 16-bit linear PCM audio output sink.
 * Enables fast and deterministic unit testing with fakes without requiring an Android audio server.
 */
interface PcmSink {
    /**
     * Writes up to [count] 16-bit PCM frames from [buffer] starting at [offset].
     * Returns the number of frames written, or a negative error code.
     */
    fun write(buffer: ShortArray, offset: Int, count: Int): Int

    fun play()
    fun pause()
    fun flush()
    fun stop()
    fun release()

    /**
     * Returns the current playback head position in frames.
     */
    fun getPlaybackHeadPosition(): Long

    /**
     * Returns the cumulative count of audio buffer underruns during playback.
     */
    fun getUnderrunCount(): Int
}
