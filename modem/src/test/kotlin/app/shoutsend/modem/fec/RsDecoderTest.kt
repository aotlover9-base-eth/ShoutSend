package app.shoutsend.modem.fec

import app.shoutsend.modem.profile.RsProfile
import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RsDecoderTest {

    private val rng = SeededRandom(606L)

    @Test
    fun testEveryProfileRandomErrorsUpToMaxErrors() {
        for (profile in listOf(RsProfile.LIGHT, RsProfile.STANDARD, RsProfile.STRONG)) {
            val totalTrials = 2000
            for (t in 0 until totalTrials) {
                val originalData = rng.nextBytes(profile.k)
                val codeword = RsEncoder.encode(originalData, profile)

                val numErrors = rng.nextInt(0, profile.maxErrors + 1)
                val corrupted = codeword.copyOf()

                // Pick unique random error positions
                val errorPositions = mutableSetOf<Int>()
                while (errorPositions.size < numErrors) {
                    errorPositions.add(rng.nextInt(0, profile.n))
                }

                for (pos in errorPositions) {
                    var delta = rng.nextInt(1, 256)
                    while (delta == 0) delta = rng.nextInt(1, 256)
                    corrupted[pos] = (corrupted[pos].toInt() xor delta).toByte()
                }

                val result = RsDecoder.decode(corrupted, IntArray(0), profile)
                assertTrue(
                    "Failed to correct $numErrors errors in ${profile.name} at trial $t",
                    result is RsDecodeResult.Corrected
                )
                val corrected = result as RsDecodeResult.Corrected
                assertArrayEquals(
                    "Data mismatch in ${profile.name} at trial $t",
                    originalData,
                    corrected.data
                )
                assertEquals(numErrors, corrected.errorCount)
                assertEquals(0, corrected.erasureCount)
            }
        }
    }

    @Test
    fun testErasuresOnlyUpToNsym() {
        for (profile in listOf(RsProfile.LIGHT, RsProfile.STANDARD, RsProfile.STRONG)) {
            for (numErasures in 1..profile.nsym) {
                for (trial in 0 until 50) {
                    val originalData = rng.nextBytes(profile.k)
                    val codeword = RsEncoder.encode(originalData, profile)
                    val corrupted = codeword.copyOf()

                    val erasurePositions = mutableSetOf<Int>()
                    while (erasurePositions.size < numErasures) {
                        erasurePositions.add(rng.nextInt(0, profile.n))
                    }

                    for (pos in erasurePositions) {
                        corrupted[pos] = (corrupted[pos].toInt() xor rng.nextInt(1, 256)).toByte()
                    }

                    val result = RsDecoder.decode(corrupted, erasurePositions.toIntArray(), profile)
                    assertTrue("Erasure test failed for $numErasures in ${profile.name}", result is RsDecodeResult.Corrected)
                    val corrected = result as RsDecodeResult.Corrected
                    assertArrayEquals(originalData, corrected.data)
                    assertEquals(0, corrected.errorCount)
                    assertEquals(numErasures, corrected.erasureCount)
                }
            }
        }
    }

    @Test
    fun testMixedErrorsAndErasuresAtLimit() {
        val profile = RsProfile.STANDARD // nsym = 12
        for (numErrors in 1..5) {
            val numErasures = profile.nsym - 2 * numErrors // e.g. e=3 -> s=6; 2*3 + 6 = 12
            for (trial in 0 until 50) {
                val originalData = rng.nextBytes(profile.k)
                val codeword = RsEncoder.encode(originalData, profile)
                val corrupted = codeword.copyOf()

                val allPositions = mutableSetOf<Int>()
                while (allPositions.size < numErrors + numErasures) {
                    allPositions.add(rng.nextInt(0, profile.n))
                }

                val allPosList = allPositions.toList()
                val erasurePos = allPosList.subList(0, numErasures).toIntArray()
                val errorPos = allPosList.subList(numErasures, allPosList.size)

                for (pos in allPositions) {
                    corrupted[pos] = (corrupted[pos].toInt() xor rng.nextInt(1, 256)).toByte()
                }

                val result = RsDecoder.decode(corrupted, erasurePos, profile)
                assertTrue("Mixed test failed (e=$numErrors, s=$numErasures)", result is RsDecodeResult.Corrected)
                val corrected = result as RsDecodeResult.Corrected
                assertArrayEquals(originalData, corrected.data)
                assertEquals(numErrors, corrected.errorCount)
                assertEquals(numErasures, corrected.erasureCount)
            }
        }
    }

    @Test
    fun testOneBeyondLimitFailureAndMiscorrectionRate() {
        val profile = RsProfile.STANDARD // maxErrors = 6, limit + 1 = 7 errors
        val trials = 10000
        var failureCount = 0
        var miscorrectionCount = 0

        val errorCount = profile.maxErrors + 1 // 7 errors (uncorrectable)

        for (t in 0 until trials) {
            val originalData = rng.nextBytes(profile.k)
            val codeword = RsEncoder.encode(originalData, profile)
            val corrupted = codeword.copyOf()

            val positions = mutableSetOf<Int>()
            while (positions.size < errorCount) {
                positions.add(rng.nextInt(0, profile.n))
            }
            for (pos in positions) {
                corrupted[pos] = (corrupted[pos].toInt() xor rng.nextInt(1, 256)).toByte()
            }

            val result = RsDecoder.decode(corrupted, IntArray(0), profile)
            when (result) {
                is RsDecodeResult.Failure -> failureCount++
                is RsDecodeResult.Corrected -> {
                    // Decoder thought it corrected, but data was actually uncorrectable
                    if (!originalData.contentEquals(result.data)) {
                        miscorrectionCount++
                    }
                }
            }
        }

        val miscorrectionRate = (miscorrectionCount.toDouble() / trials.toDouble()) * 100.0
        println("Beyond-Capacity (7 errors in RS(60,48)) Results over $trials trials:")
        println("  Honest Failures Detected: $failureCount (${(failureCount.toDouble() / trials) * 100.0}%)")
        println("  Miscorrections: $miscorrectionCount (Rate: %.4f%%)".format(miscorrectionRate))

        // Requirement: miscorrection must be below 1 in 100 (1.0%)
        assertTrue(
            "Miscorrection rate ($miscorrectionRate%) must be below 1.0%",
            miscorrectionRate < 1.0
        )
    }

    @Test
    fun testErrorInParityBytesOnly() {
        val profile = RsProfile.STANDARD
        val originalData = rng.nextBytes(profile.k)
        val codeword = RsEncoder.encode(originalData, profile)
        val corrupted = codeword.copyOf()

        // Corrupt only parity bytes (indices k until n)
        for (i in 0 until 4) {
            val pos = profile.k + i
            corrupted[pos] = (corrupted[pos].toInt() xor 0x7F).toByte()
        }

        val result = RsDecoder.decode(corrupted, IntArray(0), profile)
        assertTrue(result is RsDecodeResult.Corrected)
        val corrected = result as RsDecodeResult.Corrected
        assertArrayEquals(originalData, corrected.data)
        assertEquals(4, corrected.errorCount)
    }

    @Test
    fun testAllZeroAndAll0xFFMessages() {
        val profile = RsProfile.STANDARD

        // All zero
        val zeros = ByteArray(profile.k) { 0 }
        val cwZeros = RsEncoder.encode(zeros, profile)
        cwZeros[10] = 0xAA.toByte() // 1 error
        val resZeros = RsDecoder.decode(cwZeros, IntArray(0), profile)
        assertTrue(resZeros is RsDecodeResult.Corrected)
        assertArrayEquals(zeros, (resZeros as RsDecodeResult.Corrected).data)

        // All 0xFF
        val ones = ByteArray(profile.k) { 0xFF.toByte() }
        val cwOnes = RsEncoder.encode(ones, profile)
        cwOnes[20] = 0x11.toByte() // 1 error
        val resOnes = RsDecoder.decode(cwOnes, IntArray(0), profile)
        assertTrue(resOnes is RsDecodeResult.Corrected)
        assertArrayEquals(ones, (resOnes as RsDecodeResult.Corrected).data)
    }

    @Test
    fun testErasuresGivenWhenBytesAreActuallyCorrect() {
        val profile = RsProfile.STANDARD
        val originalData = rng.nextBytes(profile.k)
        val codeword = RsEncoder.encode(originalData, profile)

        // Flag 6 erasure positions but DO NOT distort them
        val erasures = intArrayOf(2, 5, 12, 25, 33, 47)
        val result = RsDecoder.decode(codeword, erasures, profile)
        assertTrue(result is RsDecodeResult.Corrected)
        val corrected = result as RsDecodeResult.Corrected
        assertArrayEquals(originalData, corrected.data)
        assertEquals(0, corrected.errorCount)
        assertEquals(6, corrected.erasureCount)
    }

    @Test
    fun benchmarkDecodeTenThousandCodewords() {
        for (profile in listOf(RsProfile.LIGHT, RsProfile.STANDARD, RsProfile.STRONG)) {
            val iterations = 10000
            val data = rng.nextBytes(profile.k)
            val cleanCodeword = RsEncoder.encode(data, profile)

            // Introduce 2 errors
            val testCodeword = cleanCodeword.copyOf()
            testCodeword[3] = (testCodeword[3].toInt() xor 0x55).toByte()
            testCodeword[7] = (testCodeword[7].toInt() xor 0xAA).toByte()

            // Warm up
            for (i in 0 until 500) {
                RsDecoder.decode(testCodeword, IntArray(0), profile)
            }

            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                RsDecoder.decode(testCodeword, IntArray(0), profile)
            }
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            val speed = iterations / (elapsedMs / 1000.0)

            println("RsDecoder Benchmark [${profile.name}]:")
            println("  Decoded $iterations codewords in %.2f ms (%.2f codewords/sec)".format(elapsedMs, speed))
        }
    }
}
