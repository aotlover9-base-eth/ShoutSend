package app.shoutsend.modem.profile

import app.shoutsend.modem.AudioSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class TimingTest {

    @Test
    fun sampleCounts48kHz() {
        val spec = AudioSpec.RATE_48K

        // Robust: 60 ms, 6 ms ramp
        assertEquals(2880, Timing.symbolSamples(Speed.ROBUST, spec))
        assertEquals(288, Timing.rampSamples(Speed.ROBUST, spec))

        // Normal: 30 ms, 4 ms ramp
        assertEquals(1440, Timing.symbolSamples(Speed.NORMAL, spec))
        assertEquals(192, Timing.rampSamples(Speed.NORMAL, spec))

        // Fast: 20 ms, 3 ms ramp
        assertEquals(960, Timing.symbolSamples(Speed.FAST, spec))
        assertEquals(144, Timing.rampSamples(Speed.FAST, spec))
    }

    @Test
    fun sampleCounts44_1kHz() {
        val spec = AudioSpec.RATE_44K

        // Robust: 60 ms -> 2646 samples, 6 ms -> 265 samples
        assertEquals(2646, Timing.symbolSamples(Speed.ROBUST, spec))
        assertEquals(265, Timing.rampSamples(Speed.ROBUST, spec))

        // Normal: 30 ms -> 1323 samples, 4 ms -> 176 samples
        assertEquals(1323, Timing.symbolSamples(Speed.NORMAL, spec))
        assertEquals(176, Timing.rampSamples(Speed.NORMAL, spec))

        // Fast: 20 ms -> 882 samples, 3 ms -> 132 samples
        assertEquals(882, Timing.symbolSamples(Speed.FAST, spec))
        assertEquals(132, Timing.rampSamples(Speed.FAST, spec))
    }
}
