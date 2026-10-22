package app.shoutsend.modem.protocol

import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitenerTest {

    private val rng = SeededRandom(202L)

    @Test
    fun whiteningIsItsOwnExactInverse() {
        val payload = rng.nextBytes(60)
        val seed = 7

        val whitened = Whitener.whiten(payload, seed)
        assertFalse("Whitened bytes should differ from original", payload.contentEquals(whitened))

        val recovered = Whitener.whiten(whitened, seed)
        assertArrayEquals("Double whitening must recover original bytes", payload, recovered)
    }

    @Test
    fun inPlaceMatchesCopying() {
        val payload = rng.nextBytes(48)
        val seed = 3

        val copyResult = Whitener.whiten(payload, seed)
        val inPlaceTarget = payload.copyOf()
        Whitener.whitenInPlace(inPlaceTarget, seed)

        assertArrayEquals(copyResult, inPlaceTarget)
    }

    @Test
    fun statisticalTestZeroBufferRunsAndUniformity() {
        var overallMaxRun = 0
        val chiSquareResults = mutableListOf<Double>()

        for (seed in 0..15) {
            val allZeros = ByteArray(60)
            val whitened = Whitener.whiten(allZeros, seed)

            // Map each byte to two 4-bit symbols (120 symbols total)
            val symbols = IntArray(120)
            for (i in 0 until 60) {
                val b = whitened[i].toInt() and 0xFF
                symbols[i * 2] = (b ushr 4) and 0x0F
                symbols[i * 2 + 1] = b and 0x0F
            }

            // Check max run of consecutive identical symbols
            var currRun = 1
            var maxRun = 1
            for (i in 1 until symbols.size) {
                if (symbols[i] == symbols[i - 1]) {
                    currRun++
                    if (currRun > maxRun) maxRun = currRun
                } else {
                    currRun = 1
                }
            }

            if (maxRun > overallMaxRun) overallMaxRun = maxRun

            // Task requirement: never produces a run of more than 6 identical symbols
            assertTrue("Seed $seed produced run of $maxRun identical symbols (> 6)", maxRun <= 6)

            // Chi-square test against uniform distribution (16 buckets, 120 / 16 = 7.5 expected per bucket)
            val counts = IntArray(16)
            for (sym in symbols) {
                counts[sym]++
            }
            val expected = 120.0 / 16.0
            var chi2 = 0.0
            for (c in counts) {
                val diff = c.toDouble() - expected
                chi2 += (diff * diff) / expected
            }
            chiSquareResults.add(chi2)
            // Generous bound for 15 degrees of freedom at p=0.01 is ~30.58
            assertTrue("Chi-square statistic ($chi2) exceeds generous threshold for seed $seed", chi2 < 30.0)
        }

        println("PN9 Whitener Statistical Results:")
        println("  Overall measured maximum identical run: $overallMaxRun symbols (limit <= 6)")
        println("  Chi-square stats: ${chiSquareResults.map { "%.2f".format(it) }}")
    }
}
