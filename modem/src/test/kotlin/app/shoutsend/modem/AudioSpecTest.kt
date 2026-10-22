package app.shoutsend.modem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioSpecTest {

    @Test
    fun acceptsStandardSampleRates() {
        val spec48k = AudioSpec(48000)
        assertEquals(48000, spec48k.sampleRateHz)
        assertEquals(AudioSpec.RATE_48K, spec48k)

        val spec44k = AudioSpec(44100)
        assertEquals(44100, spec44k.sampleRateHz)
        assertEquals(AudioSpec.RATE_44K, spec44k)
    }

    @Test
    fun rejectsUnsupportedSampleRates() {
        val invalidRates = listOf(8000, 16000, 22050, 32000, 96000, 192000, 0, -48000)
        for (rate in invalidRates) {
            val exception = assertThrows(IllegalArgumentException::class.java) {
                AudioSpec(rate)
            }
            assert(exception.message?.contains("Unsupported sample rate") == true)
        }
    }
}
