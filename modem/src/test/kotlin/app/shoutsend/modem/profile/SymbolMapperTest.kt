package app.shoutsend.modem.profile

import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SymbolMapperTest {

    private val rng = SeededRandom(42L)

    @Test
    fun mfsk16RoundTripRandomPayloads() {
        val testSizes = listOf(0, 1, 2, 10, 48, 60, 320)
        for (size in testSizes) {
            val original = rng.nextBytes(size)
            val symbols = SymbolMapper.bytesToSymbols(original, Scheme.MFSK16)
            assertEquals(size * 2, symbols.size)

            val reconstructed = SymbolMapper.symbolsToBytes(symbols, Scheme.MFSK16, size)
            assertArrayEquals("MFSK16 roundtrip failed for size $size", original, reconstructed)
        }
    }

    @Test
    fun dtfskRoundTripRandomPayloads() {
        val testSizes = listOf(0, 1, 2, 3, 4, 10, 48, 60, 320)
        for (size in testSizes) {
            val original = rng.nextBytes(size)
            val symbols = SymbolMapper.bytesToSymbols(original, Scheme.DTFSK)

            val expectedSymbols = if (size == 0) 0 else (size * 8 + 5) / 6
            assertEquals(expectedSymbols, symbols.size)

            val reconstructed = SymbolMapper.symbolsToBytes(symbols, Scheme.DTFSK, size)
            assertArrayEquals("DTFSK roundtrip failed for size $size", original, reconstructed)
        }
    }

    @Test
    fun dtfskToneSplittingAndCombining() {
        for (sym in 0..63) {
            val (lowTone, highTone) = SymbolMapper.splitDtfskTones(sym)
            assert(lowTone in 0..7) { "Low tone out of range: $lowTone" }
            assert(highTone in 8..15) { "High tone out of range: $highTone" }

            val combined = SymbolMapper.combineDtfskTones(lowTone, highTone)
            assertEquals("DTFSK tone combination mismatch for symbol $sym", sym, combined)
        }
    }
}
