package app.shoutsend.modem.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeBlockCodecTest {

    @Test
    fun roundTripAllValidCombinations() {
        for (scheme in Scheme.entries) {
            for (speed in Speed.entries) {
                for (rsId in 0..3) {
                    val symbols = ModeBlockCodec.encodeMode(scheme, speed, rsId)
                    assertEquals(2, symbols.size)

                    val decodeResult = ModeBlockCodec.decodeMode(symbols)
                    assertTrue("Failed to decode valid mode: $scheme, $speed, $rsId", decodeResult.isSuccess)

                    val decoded = decodeResult.getOrThrow()
                    assertEquals(scheme, decoded.scheme)
                    assertEquals(speed, decoded.speed)
                    assertEquals(rsId, decoded.rsProfileId)
                }
            }
        }
    }

    @Test
    fun detectsEverySingleBitErrorIn8Bits() {
        var totalTested = 0
        var totalDetected = 0

        for (scheme in Scheme.entries) {
            for (speed in Speed.entries) {
                for (rsId in 0..3) {
                    val originalByte = ModeBlockCodec.encodeModeByte(scheme, speed, rsId)

                    // Flip each of the 8 bits
                    for (bit in 0 until 8) {
                        val corruptedByte = originalByte xor (1 shl bit)
                        val corruptedSymbols = intArrayOf(
                            (corruptedByte ushr 4) and 0x0F,
                            corruptedByte and 0x0F
                        )

                        val result = ModeBlockCodec.decodeMode(corruptedSymbols)
                        totalTested++
                        if (result.isFailure) {
                            totalDetected++
                        }
                    }
                }
            }
        }

        assertEquals("Must test 24 combinations * 8 bits = 192 cases", 192, totalTested)
        assertEquals("Must detect 100% of single-bit errors in mode block", totalTested, totalDetected)
    }

    @Test
    fun repeatedModeBlockDecodingAndRepair() {
        val original = ModeBlockCodec.encodeModeRepeated(Scheme.DTFSK, Speed.FAST, 1)
        assertEquals(4, original.size)

        // Perfect reception
        val cleanResult = ModeBlockCodec.decodeMode(original)
        assertTrue(cleanResult.isSuccess)
        assertEquals(Scheme.DTFSK, cleanResult.getOrThrow().scheme)

        // Corrupted first pair, clean second pair
        val corruptedFirst = original.clone()
        corruptedFirst[0] = corruptedFirst[0] xor 1 // flip a bit
        val repairedResult = ModeBlockCodec.decodeMode(corruptedFirst)
        assertTrue(repairedResult.isSuccess)
        assertEquals(Scheme.DTFSK, repairedResult.getOrThrow().scheme)

        // Both corrupted
        val bothCorrupted = original.clone()
        bothCorrupted[0] = bothCorrupted[0] xor 1
        bothCorrupted[2] = bothCorrupted[2] xor 1
        val failedResult = ModeBlockCodec.decodeMode(bothCorrupted)
        assertTrue(failedResult.isFailure)
    }
}
