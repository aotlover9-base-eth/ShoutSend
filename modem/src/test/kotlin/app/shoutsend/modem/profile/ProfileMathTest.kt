package app.shoutsend.modem.profile

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.round

class ProfileMathTest {

    @Test
    fun reproducesProtocolV1RawRates() {
        val cases = listOf(
            Triple(Scheme.MFSK16, Speed.ROBUST, 8.3),
            Triple(Scheme.MFSK16, Speed.NORMAL, 16.7),
            Triple(Scheme.MFSK16, Speed.FAST, 25.0),
            Triple(Scheme.DTFSK, Speed.ROBUST, 12.5),
            Triple(Scheme.DTFSK, Speed.NORMAL, 25.0),
            Triple(Scheme.DTFSK, Speed.FAST, 37.5)
        )

        for ((scheme, speed, expectedBytesSec) in cases) {
            val profile = ModemProfile(Band.STEALTH, scheme, speed)
            val rawBps = ProfileMath.rawBytesPerSecond(profile)
            val roundedBps = round(rawBps * 10.0) / 10.0
            assertEquals(
                "Raw rate mismatch for $scheme at $speed",
                expectedBytesSec,
                roundedBps,
                0.05
            )
        }
    }
}
