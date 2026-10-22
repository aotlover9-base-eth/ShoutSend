package app.shoutsend.modem.protocol

import app.shoutsend.modem.fec.RsDecodeResult
import app.shoutsend.modem.fec.RsDecoder
import app.shoutsend.modem.fec.RsEncoder
import app.shoutsend.modem.profile.RsProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.testutil.SeededRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class CodewordSymbolizerTest {

    private val rng = SeededRandom(707L)

    @Test
    fun symbolCountExactFor60ByteCodewords() {
        val codeword = rng.nextBytes(60)

        val mfskSymbols = CodewordSymbolizer.encode(codeword, Scheme.MFSK16, frameSeed = 1)
        assertEquals("MFSK16 must produce 120 symbols for 60 bytes", 120, mfskSymbols.size)

        val dtfskSymbols = CodewordSymbolizer.encode(codeword, Scheme.DTFSK, frameSeed = 1)
        assertEquals("DTFSK must produce 80 symbols for 60 bytes", 80, dtfskSymbols.size)
    }

    @Test
    fun cleanRoundTripProducesZeroErasures() {
        val original = rng.nextBytes(60)

        for (scheme in Scheme.entries) {
            val symbols = CodewordSymbolizer.encode(original, scheme, frameSeed = 5)
            val confidences = FloatArray(symbols.size) { 1.0f }

            val decoded = CodewordSymbolizer.decode(symbols, confidences, scheme, frameSeed = 5, byteCount = 60)
            assertArrayEquals("Roundtrip failed for $scheme", original, decoded.bytes)
            assertEquals(0, decoded.erasurePositions.size)
        }
    }

    @Test
    fun tableDrivenDtfskSixSymbolWindowMapping() {
        // Table of expected byte contributions for the first 6 DTFSK symbols (36 bits -> 4.5 bytes, indices 0..4)
        val expectedTouchedBytes = mapOf(
            0 to listOf(0),
            1 to listOf(0, 1),
            2 to listOf(1, 2),
            3 to listOf(2),
            4 to listOf(3),
            5 to listOf(3, 4)
        )

        for (symIdx in 0..5) {
            val bitStart = symIdx * 6
            val bitEnd = symIdx * 6 + 5
            val bStart = bitStart / 8
            val bEnd = bitEnd / 8

            val actualTouched = (bStart..bEnd).toList()
            assertEquals("DTFSK symbol $symIdx touched byte mismatch", expectedTouchedBytes[symIdx], actualTouched)
        }
    }

    @Test
    fun burstDamageAnalysisTable() {
        println("### Symbol Burst Impact Table")
        println("| Burst Length (L) | MFSK16 Max Bytes | MFSK16 Bound | DTFSK Max Bytes | DTFSK Bound |")
        println("| :--- | :--- | :--- | :--- | :--- |")

        for (l in 1..12) {
            // MFSK16
            var mfskMax = 0
            for (offset in 0..(120 - l)) {
                val affected = mutableSetOf<Int>()
                for (s in offset until offset + l) {
                    affected.add(s / 2)
                }
                if (affected.size > mfskMax) mfskMax = affected.size
            }
            val mfskBound = ceil(l * 4.0 / 8.0).toInt() + 1
            assertTrue(mfskMax <= mfskBound)

            // DTFSK
            var dtfskMax = 0
            for (offset in 0..(80 - l)) {
                val affected = mutableSetOf<Int>()
                for (s in offset until offset + l) {
                    val bStart = (s * 6) / 8
                    val bEnd = (s * 6 + 5) / 8
                    for (b in bStart..bEnd) {
                        affected.add(b)
                    }
                }
                if (affected.size > dtfskMax) dtfskMax = affected.size
            }
            val dtfskBound = ceil(l * 6.0 / 8.0).toInt() + 1
            assertTrue(dtfskMax <= dtfskBound)

            println("| %2d symbols       | %2d bytes         | %2d bytes      | %2d bytes        | %2d bytes     |".format(
                l, mfskMax, mfskBound, dtfskMax, dtfskBound
            ))
        }
    }

    @Test
    fun endToEndRecoveryRateEvaluation() {
        val serRates = listOf(0.0, 0.02, 0.05, 0.10)
        val numTrials = 500
        val profile = RsProfile.STANDARD // RS(60,48)

        println("### Recovery Rate Results over $numTrials messages per SER:")
        println("| Symbol Error Rate (SER) | Without Erasures | With Erasures | Improvement |")
        println("| :--- | :--- | :--- | :--- |")

        for (ser in serRates) {
            var succNoErasure = 0
            var succWithErasure = 0

            for (trial in 0 until numTrials) {
                val originalMessage = rng.nextBytes(profile.k)
                val codeword = RsEncoder.encode(originalMessage, profile)
                val seed = trial % 16

                val symbols = CodewordSymbolizer.encode(codeword, Scheme.MFSK16, seed)
                val corruptedSymbols = symbols.clone()
                val confidences = FloatArray(symbols.size) { 1.0f }

                for (i in symbols.indices) {
                    if (rng.nextFloat() < ser) {
                        corruptedSymbols[i] = (corruptedSymbols[i] + rng.nextInt(1, 16)) % 16
                        confidences[i] = 0.1f // Demodulator low confidence
                    }
                }

                // 1. Without Erasures (confidences ignored)
                val decodedNoErasure = CodewordSymbolizer.decode(
                    corruptedSymbols,
                    FloatArray(symbols.size) { 1.0f },
                    Scheme.MFSK16,
                    seed,
                    byteCount = profile.n
                )
                val rsRes1 = RsDecoder.decode(decodedNoErasure.bytes, IntArray(0), profile)
                if (rsRes1 is RsDecodeResult.Corrected && originalMessage.contentEquals(rsRes1.data)) {
                    succNoErasure++
                }

                // 2. With Erasures (low confidences mapped to byte erasures)
                val decodedWithErasure = CodewordSymbolizer.decode(
                    corruptedSymbols,
                    confidences,
                    Scheme.MFSK16,
                    seed,
                    byteCount = profile.n
                )
                val rsRes2 = RsDecoder.decode(decodedWithErasure.bytes, decodedWithErasure.erasurePositions, profile)
                if (rsRes2 is RsDecodeResult.Corrected && originalMessage.contentEquals(rsRes2.data)) {
                    succWithErasure++
                }
            }

            val rateNoEr = (succNoErasure.toDouble() / numTrials) * 100.0
            val rateEr = (succWithErasure.toDouble() / numTrials) * 100.0
            val imp = rateEr - rateNoEr

            println("| %2d%% SER                  | %5.1f%%           | %5.1f%%         | +%4.1f%%       |".format(
                (ser * 100).toInt(), rateNoEr, rateEr, imp
            ))

            if (ser > 0.02) {
                assertTrue("Erasure marking must improve recovery at SER=$ser", rateEr > rateNoEr)
            }
        }
    }
}
