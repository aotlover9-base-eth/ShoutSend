package app.shoutsend.modem.protocol

/**
 * Whitening pseudo-random scrambler using a PN9 LFSR (x^9 + x^5 + 1).
 *
 * Seeded by an integer (typically frame index), producing a keystream that is XORed into bytes.
 * Since whitening is an XOR operation, [whiten] is its own exact inverse.
 */
public object Whitener {

    public const val LFSR_MASK: Int = 0x1FF

    /**
     * Derives a non-zero 9-bit initial LFSR state from [seed].
     */
    public fun deriveInitialState(seed: Int): Int {
        val nonZeroSeed = ((seed and 0x0F) shl 5) xor LFSR_MASK
        return if (nonZeroSeed == 0) LFSR_MASK else nonZeroSeed
    }

    /**
     * Whitens or de-whitens [bytes] in-place using the PN9 sequence keyed by [seed].
     * Zero allocations in this loop.
     */
    public fun whitenInPlace(bytes: ByteArray, seed: Int, offset: Int = 0, length: Int = bytes.size) {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) { "Invalid bounds" }
        var state = deriveInitialState(seed)

        for (i in 0 until length) {
            var keystreamByte = 0
            for (b in 0 until 8) {
                val outBit = (state ushr 8) and 1
                val feedback = ((state ushr 8) xor (state ushr 4)) and 1
                state = ((state shl 1) or feedback) and LFSR_MASK
                keystreamByte = (keystreamByte shl 1) or outBit
            }
            val idx = offset + i
            bytes[idx] = (bytes[idx].toInt() xor keystreamByte).toByte()
        }
    }

    /**
     * Returns a whitened or de-whitened copy of [bytes].
     */
    public fun whiten(bytes: ByteArray, seed: Int): ByteArray {
        val copy = bytes.copyOf()
        whitenInPlace(copy, seed)
        return copy
    }
}
