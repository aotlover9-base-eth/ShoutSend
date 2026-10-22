package app.shoutsend.modem.fec

import app.shoutsend.modem.profile.RsProfile
import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RsEncoderTest {

    private val rng = SeededRandom(505L)

    @Test
    fun generatorPolynomialHasExpectedDegreeAndRoots() {
        for (profile in listOf(RsProfile.LIGHT, RsProfile.STANDARD, RsProfile.STRONG)) {
            val g = RsEncoder.getGenerator(profile)
            assertEquals("Generator degree must equal nsym", profile.nsym + 1, g.size)
            assertEquals("Leading coefficient of generator must be 1", 1, g[0])

            // Each root alpha^0 ... alpha^(nsym-1) must evaluate to 0
            for (i in 0 until profile.nsym) {
                val root = Gf256.exp[i]
                val value = Gf256.polyEvaluate(g, root)
                assertEquals(
                    "Generator for ${profile.name} does not have root alpha^$i",
                    0,
                    value
                )
            }
        }
    }

    @Test
    fun thousandRandomMessagesPerProfileProduceZeroSyndromes() {
        for (profile in listOf(RsProfile.LIGHT, RsProfile.STANDARD, RsProfile.STRONG)) {
            val k = profile.k
            val nsym = profile.nsym

            for (m in 0 until 1000) {
                val data = rng.nextBytes(k)
                val codeword = RsEncoder.encode(data, profile)
                assertEquals(profile.n, codeword.size)

                // Convert codeword to int array for polynomial evaluation
                val poly = IntArray(codeword.size) { i -> codeword[i].toInt() and 0xFF }

                // Check all nsym syndromes
                for (i in 0 until nsym) {
                    val root = Gf256.exp[i]
                    val syndrome = Gf256.polyEvaluate(poly, root)
                    assertEquals(
                        "Syndrome S_$i was non-zero ($syndrome) for message #$m in ${profile.name}",
                        0,
                        syndrome
                    )
                }
            }
        }
    }

    @Test
    fun encodingIsLinear() {
        val profile = RsProfile.STANDARD
        val a = rng.nextBytes(profile.k)
        val b = rng.nextBytes(profile.k)

        val aXorB = ByteArray(profile.k) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }

        val cwA = RsEncoder.encode(a, profile)
        val cwB = RsEncoder.encode(b, profile)
        val cwA_xor_cwB = ByteArray(profile.n) { i -> (cwA[i].toInt() xor cwB[i].toInt()).toByte() }

        val cwLinear = RsEncoder.encode(aXorB, profile)

        assertArrayEquals("encode(a xor b) != encode(a) xor encode(b)", cwA_xor_cwB, cwLinear)
    }

    @Test
    fun shortenedCodewordsSupport() {
        val profile = RsProfile.STANDARD
        val shortLengths = listOf(1, 5, 10, 20, 35)

        for (len in shortLengths) {
            val shortData = rng.nextBytes(len)
            val shortenedCodeword = RsEncoder.encode(shortData, profile)
            assertEquals(len + profile.nsym, shortenedCodeword.size)

            // Verify with padded equivalent
            val paddedData = ByteArray(profile.k)
            System.arraycopy(shortData, 0, paddedData, profile.k - len, len)
            val fullCodeword = RsEncoder.encode(paddedData, profile)

            // Parity should match identically
            val shortParity = shortenedCodeword.copyOfRange(len, shortenedCodeword.size)
            val fullParity = fullCodeword.copyOfRange(profile.k, fullCodeword.size)
            assertArrayEquals("Shortened parity must equal zero-padded parity", fullParity, shortParity)

            // Syndromes must all be zero
            val poly = IntArray(shortenedCodeword.size) { i -> shortenedCodeword[i].toInt() and 0xFF }
            for (i in 0 until profile.nsym) {
                val root = Gf256.exp[i]
                val syndrome = Gf256.polyEvaluate(poly, root)
                assertEquals("Syndrome S_$i for shortened codeword (len=$len) was non-zero", 0, syndrome)
            }
        }
    }

    @Test
    fun benchmarkTenThousandCodewords() {
        val profile = RsProfile.STANDARD
        val iterations = 10000
        val data = rng.nextBytes(profile.k)
        val outBuffer = ByteArray(profile.n)

        // Warm up
        for (i in 0 until 1000) {
            RsEncoder.encode(data, profile, outBuffer, 0)
        }

        val startTime = System.nanoTime()
        for (i in 0 until iterations) {
            RsEncoder.encode(data, profile, outBuffer, 0)
        }
        val elapsedNanos = System.nanoTime() - startTime
        val elapsedMs = elapsedNanos / 1_000_000.0

        val totalBytes = iterations * profile.n
        val throughputMbSec = (totalBytes.toDouble() / (1024.0 * 1024.0)) / (elapsedMs / 1000.0)

        println("Reed-Solomon RS(60,48) Benchmark:")
        println("  Encoded $iterations codewords in %.2f ms".format(elapsedMs))
        println("  Speed: %.2f codewords/sec (%.2f MB/s)".format(iterations / (elapsedMs / 1000.0), throughputMbSec))
    }
}
