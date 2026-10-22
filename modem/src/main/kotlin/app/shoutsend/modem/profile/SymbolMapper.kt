package app.shoutsend.modem.profile

/**
 * Maps raw byte arrays to acoustic transmission symbols, and vice-versa.
 *
 * - [Scheme.MFSK16]: 4 bits per symbol (high nibble first, 0..15).
 * - [Scheme.DTFSK]: 6 bits per symbol (0..63), mapped to two simultaneous tones:
 *   one in lower group (tones 0..7, bits 5..3) and one in upper group (tones 8..15, bits 2..0).
 */
public object SymbolMapper {

    public fun symbolBits(scheme: Scheme): Int = scheme.bitsPerSymbol

    /**
     * Converts a byte array to an array of symbol integers.
     */
    public fun bytesToSymbols(bytes: ByteArray, scheme: Scheme): IntArray {
        return when (scheme) {
            Scheme.MFSK16 -> {
                val symbols = IntArray(bytes.size * 2)
                for (i in bytes.indices) {
                    val b = bytes[i].toInt() and 0xFF
                    symbols[i * 2] = (b ushr 4) and 0x0F
                    symbols[i * 2 + 1] = b and 0x0F
                }
                symbols
            }
            Scheme.DTFSK -> {
                if (bytes.isEmpty()) return IntArray(0)
                val totalBits = bytes.size * 8
                val symbolCount = (totalBits + 5) / 6
                val symbols = IntArray(symbolCount)

                var bitBuffer = 0
                var bitsBuffered = 0
                var symIdx = 0

                for (b in bytes) {
                    bitBuffer = (bitBuffer shl 8) or (b.toInt() and 0xFF)
                    bitsBuffered += 8
                    while (bitsBuffered >= 6) {
                        val shift = bitsBuffered - 6
                        symbols[symIdx++] = (bitBuffer ushr shift) and 0x3F
                        bitsBuffered -= 6
                        bitBuffer = bitBuffer and ((1 shl bitsBuffered) - 1)
                    }
                }
                if (bitsBuffered > 0) {
                    symbols[symIdx] = (bitBuffer shl (6 - bitsBuffered)) and 0x3F
                }
                symbols
            }
        }
    }

    /**
     * Reconstructs the original byte array from symbol integers.
     *
     * @param symbols The decoded symbol array.
     * @param scheme The modulation scheme used.
     * @param byteCount The exact number of expected payload bytes.
     */
    public fun symbolsToBytes(symbols: IntArray, scheme: Scheme, byteCount: Int): ByteArray {
        require(byteCount >= 0) { "byteCount cannot be negative: $byteCount" }
        val output = ByteArray(byteCount)
        if (byteCount == 0) return output

        when (scheme) {
            Scheme.MFSK16 -> {
                for (i in 0 until byteCount) {
                    val hiIdx = i * 2
                    val loIdx = i * 2 + 1
                    val hi = if (hiIdx < symbols.size) symbols[hiIdx] and 0x0F else 0
                    val lo = if (loIdx < symbols.size) symbols[loIdx] and 0x0F else 0
                    output[i] = ((hi shl 4) or lo).toByte()
                }
            }
            Scheme.DTFSK -> {
                var bitBuffer = 0
                var bitsBuffered = 0
                var byteIdx = 0

                for (sym in symbols) {
                    bitBuffer = (bitBuffer shl 6) or (sym and 0x3F)
                    bitsBuffered += 6
                    while (bitsBuffered >= 8 && byteIdx < byteCount) {
                        val shift = bitsBuffered - 8
                        output[byteIdx++] = ((bitBuffer ushr shift) and 0xFF).toByte()
                        bitsBuffered -= 8
                        bitBuffer = bitBuffer and ((1 shl bitsBuffered) - 1)
                    }
                    if (byteIdx >= byteCount) break
                }
            }
        }
        return output
    }

    /**
     * Splits a 6-bit DTFSK symbol into (lowerToneIndex 0..7, upperToneIndex 8..15).
     */
    public fun splitDtfskTones(symbol: Int): Pair<Int, Int> {
        require(symbol in 0..63) { "DTFSK symbol must be in range 0..63, got $symbol" }
        val low = (symbol ushr 3) and 0x07
        val high = (symbol and 0x07) + 8
        return Pair(low, high)
    }

    /**
     * Combines a lower tone index (0..7) and upper tone index (8..15) into a 6-bit DTFSK symbol.
     */
    public fun combineDtfskTones(lowIndex: Int, highIndex: Int): Int {
        require(lowIndex in 0..7) { "lowIndex must be 0..7, got $lowIndex" }
        require(highIndex in 8..15) { "highIndex must be 8..15, got $highIndex" }
        return ((lowIndex and 0x07) shl 3) or ((highIndex - 8) and 0x07)
    }
}
