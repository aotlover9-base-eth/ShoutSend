package app.shoutsend.core.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioConfigTest {

    @Test
    fun preferredSampleRatesOrderAndContent() {
        val expected = intArrayOf(48000, 44100)
        assertArrayEquals(expected, AudioConfig.PREFERRED_SAMPLE_RATES)
        assertEquals(2, AudioConfig.PREFERRED_SAMPLE_RATES.size)
        assertEquals(48000, AudioConfig.PREFERRED_SAMPLE_RATES[0])
        assertEquals(44100, AudioConfig.PREFERRED_SAMPLE_RATES[1])
    }

    @Test
    fun chunkSamplesCalculations() {
        // At 48 kHz, 20 ms -> 48000 * 20 / 1000 = 960 samples
        assertEquals(960, AudioConfig.chunkSamples(48000))

        // At 44.1 kHz, 20 ms -> 44100 * 20 / 1000 = 882 samples
        assertEquals(882, AudioConfig.chunkSamples(44100))
    }

    @Test
    fun ringBufferSamplesCalculations() {
        // At 48 kHz, 2s -> 96,000 samples
        assertEquals(96000, AudioConfig.ringBufferSamples(48000))

        // At 44.1 kHz, 2s -> 88,200 samples
        assertEquals(88200, AudioConfig.ringBufferSamples(44100))
    }

    @Test
    fun configurationConstants() {
        assertEquals(20, AudioConfig.CHUNK_DURATION_MS)
        assertEquals(2, AudioConfig.RING_BUFFER_DURATION_SEC)
        assertTrue(AudioConfig.CHANNEL_CONFIG_IN > 0)
        assertTrue(AudioConfig.CHANNEL_CONFIG_OUT > 0)
        assertTrue(AudioConfig.ENCODING_PCM > 0)
    }
}
