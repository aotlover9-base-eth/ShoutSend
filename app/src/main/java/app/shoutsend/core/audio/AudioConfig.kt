package app.shoutsend.core.audio

import android.media.AudioFormat

/**
 * Audio hardware and buffer configuration parameters.
 */
object AudioConfig {
    /**
     * Preferred audio sample rates in priority order.
     * 48 kHz is the Android primary native rate; 44.1 kHz is secondary fallback.
     */
    val PREFERRED_SAMPLE_RATES: IntArray = intArrayOf(48000, 44100)

    /** Input channel configuration (Mono). */
    const val CHANNEL_CONFIG_IN: Int = AudioFormat.CHANNEL_IN_MONO

    /** Output channel configuration (Mono). */
    const val CHANNEL_CONFIG_OUT: Int = AudioFormat.CHANNEL_OUT_MONO

    /** Audio sample encoding (16-bit linear PCM). */
    const val ENCODING_PCM: Int = AudioFormat.ENCODING_PCM_16BIT

    /** Duration of an audio processing chunk in milliseconds. */
    const val CHUNK_DURATION_MS: Int = 20

    /** Duration of ring buffers in seconds. */
    const val RING_BUFFER_DURATION_SEC: Int = 2

    /**
     * Calculates the number of audio samples in a single processing chunk for the given sample rate.
     * E.g., 960 samples for 48,000 Hz, 882 samples for 44,100 Hz.
     */
    fun chunkSamples(sampleRateHz: Int): Int {
        return (sampleRateHz * CHUNK_DURATION_MS) / 1000
    }

    /**
     * Calculates the total sample capacity of a ring buffer for the given sample rate.
     */
    fun ringBufferSamples(sampleRateHz: Int): Int {
        return sampleRateHz * RING_BUFFER_DURATION_SEC
    }
}
