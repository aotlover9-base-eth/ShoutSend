package app.shoutsend.modem.protocol

import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.SymbolMapper

/**
 * Result of de-symbolization holding reconstructed whitened bytes and identified erasure positions.
 */
public data class DecodedCodeword(
    public val bytes: ByteArray,
    public val erasurePositions: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DecodedCodeword
        return bytes.contentEquals(other.bytes) && erasurePositions.contentEquals(other.erasurePositions)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + erasurePositions.contentHashCode()
        return result
    }
}

/**
 * Converts Reed-Solomon codewords to transmission symbols with PN9 whitening,
 * and maps demodulator confidence values into byte-level erasure indicators.
 */
public object CodewordSymbolizer {

    public const val DEFAULT_CONFIDENCE_THRESHOLD: Float = 0.5f

    /**
     * Converts a Reed-Solomon [codeword] into transmission symbols:
     * 1. Whitens with PN9 keystream derived from [frameSeed].
     * 2. Maps whitened bytes to symbols via [SymbolMapper].
     */
    public fun encode(
        codeword: ByteArray,
        scheme: Scheme,
        frameSeed: Int
    ): IntArray {
        val whitened = Whitener.whiten(codeword, frameSeed)
        return SymbolMapper.bytesToSymbols(whitened, scheme)
    }

    /**
     * Reconstructs byte data from received [symbols], removes PN9 whitening, and flags
     * byte erasure positions where contributing symbol [confidences] fall below [confidenceThreshold].
     */
    public fun decode(
        symbols: IntArray,
        confidences: FloatArray,
        scheme: Scheme,
        frameSeed: Int,
        byteCount: Int = 60,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): DecodedCodeword {
        val rawBytes = SymbolMapper.symbolsToBytes(symbols, scheme, byteCount)
        Whitener.whitenInPlace(rawBytes, frameSeed)

        val erasedBytes = mutableListOf<Int>()

        when (scheme) {
            Scheme.MFSK16 -> {
                for (b in 0 until byteCount) {
                    val s0 = b * 2
                    val s1 = b * 2 + 1
                    val c0 = if (s0 < confidences.size) confidences[s0] else 1.0f
                    val c1 = if (s1 < confidences.size) confidences[s1] else 1.0f
                    if (c0 < confidenceThreshold || c1 < confidenceThreshold) {
                        erasedBytes.add(b)
                    }
                }
            }
            Scheme.DTFSK -> {
                for (b in 0 until byteCount) {
                    val startSym = (8 * b) / 6
                    val endSym = (8 * b + 7) / 6
                    var isErased = false
                    for (s in startSym..endSym) {
                        val conf = if (s < confidences.size) confidences[s] else 1.0f
                        if (conf < confidenceThreshold) {
                            isErased = true
                            break
                        }
                    }
                    if (isErased) {
                        erasedBytes.add(b)
                    }
                }
            }
        }

        return DecodedCodeword(
            bytes = rawBytes,
            erasurePositions = erasedBytes.toIntArray()
        )
    }
}
