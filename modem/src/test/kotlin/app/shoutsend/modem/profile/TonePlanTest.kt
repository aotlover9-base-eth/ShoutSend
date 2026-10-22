package app.shoutsend.modem.profile

import app.shoutsend.modem.AudioSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonePlanTest {

    @Test
    fun stealthTonePlanFrequencies48k() {
        val plan = TonePlan(Band.STEALTH, AudioSpec.RATE_48K)
        assertEquals(17450.0, plan.syncTone0, 0.001)
        assertEquals(20000.0, plan.syncTone1, 0.001)
        assertEquals(16, plan.dataTones.size)

        for (i in 0 until 16) {
            val expected = 17600.0 + (i * 150.0)
            assertEquals(expected, plan.tone(i), 0.001)
        }

        assertEquals(8, plan.lowerTones.size)
        assertEquals(8, plan.upperTones.size)
        assertEquals(17600.0, plan.lowerTones[0], 0.001)
        assertEquals(18650.0, plan.lowerTones[7], 0.001)
        assertEquals(18800.0, plan.upperTones[0], 0.001)
        assertEquals(19850.0, plan.upperTones[7], 0.001)
    }

    @Test
    fun stealthTonePlanFrequencies44k() {
        val plan = TonePlan(Band.STEALTH, AudioSpec.RATE_44K)
        assertEquals(17450.0, plan.syncTone0, 0.001)
        assertEquals(20000.0, plan.syncTone1, 0.001)
        assertEquals(19850.0, plan.tone(15), 0.001)
        // 44.1 kHz Nyquist is 22050 Hz, 22050 - 1500 = 20550 Hz.
        // 20000 Hz is below 20550 Hz, so it is valid.
    }

    @Test
    fun retroTonePlanFrequencies() {
        val plan = TonePlan(Band.RETRO, AudioSpec.RATE_48K)
        assertEquals(1375.0, plan.syncTone0, 0.001)
        assertEquals(3500.0, plan.syncTone1, 0.001)
        assertEquals(16, plan.dataTones.size)

        for (i in 0 until 16) {
            val expected = 1500.0 + (i * 125.0)
            assertEquals(expected, plan.tone(i), 0.001)
        }

        assertEquals(1500.0, plan.lowerTones[0], 0.001)
        assertEquals(2375.0, plan.lowerTones[7], 0.001)
        assertEquals(2500.0, plan.upperTones[0], 0.001)
        assertEquals(3375.0, plan.upperTones[7], 0.001)
    }

    @Test
    fun minimumToneSpacingExceeds100Hz() {
        for (band in Band.entries) {
            for (spec in listOf(AudioSpec.RATE_48K, AudioSpec.RATE_44K)) {
                val plan = TonePlan(band, spec)
                val allSorted = (plan.dataTones + listOf(plan.syncTone0, plan.syncTone1)).sorted()
                for (i in 0 until allSorted.size - 1) {
                    val diff = allSorted[i + 1] - allSorted[i]
                    assertTrue(
                        "Tone spacing in $band must be >= 100 Hz, got $diff between ${allSorted[i]} and ${allSorted[i + 1]}",
                        diff >= 100.0
                    )
                }
            }
        }
    }
}
