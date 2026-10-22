package app.shoutsend.modem.profile

/**
 * Decoded transmission mode parameters extracted from a mode block.
 */
public data class DecodedMode(
    public val scheme: Scheme,
    public val speed: Speed,
    public val rsProfileId: Int
)

/**
 * Codec for the 8-bit mode declaration block.
 *
 * Bit layout:
 * - Bits 7..6: Scheme ID (2 bits)
 * - Bits 5..4: Speed ID (2 bits)
 * - Bits 3..2: RS Profile ID (2 bits)
 * - Bits 1..0: 2-bit parity checksum (Bit 1 = b7 xor b5 xor b3, Bit 0 = b6 xor b4 xor b2)
 *
 * Encodes to 2 MFSK16 symbols (high nibble, low nibble).
 * Per Protocol v1, the mode block is repeated once (4 symbols total) and always sent at MFSK16 Normal timing.
 */
public object ModeBlockCodec {

    public val TIMING_SCHEME: Scheme = Scheme.MFSK16
    public val TIMING_SPEED: Speed = Speed.NORMAL

    /**
     * Calculates the 2-bit parity checksum over the 6 data bits of the mode byte.
     */
    public fun calculateParity(schemeId: Int, speedId: Int, rsProfileId: Int): Int {
        val b7 = (schemeId ushr 1) and 1
        val b6 = schemeId and 1
        val b5 = (speedId ushr 1) and 1
        val b4 = speedId and 1
        val b3 = (rsProfileId ushr 1) and 1
        val b2 = rsProfileId and 1

        val p1 = b7 xor b5 xor b3
        val p0 = b6 xor b4 xor b2
        return (p1 shl 1) or p0
    }

    /**
     * Encodes scheme, speed, and rsProfileId into an 8-bit byte.
     */
    public fun encodeModeByte(scheme: Scheme, speed: Speed, rsProfileId: Int): Int {
        require(rsProfileId in 0..3) { "rsProfileId must fit in 2 bits (0..3), got $rsProfileId" }
        val parity = calculateParity(scheme.id, speed.id, rsProfileId)
        return ((scheme.id and 0x03) shl 6) or
                ((speed.id and 0x03) shl 4) or
                ((rsProfileId and 0x03) shl 2) or
                (parity and 0x03)
    }

    /**
     * Encodes scheme, speed, and rsProfileId into exactly 2 MFSK16 symbols.
     */
    public fun encodeMode(scheme: Scheme, speed: Speed, rsProfileId: Int): IntArray {
        val modeByte = encodeModeByte(scheme, speed, rsProfileId)
        return intArrayOf(
            (modeByte ushr 4) and 0x0F,
            modeByte and 0x0F
        )
    }

    /**
     * Encodes the mode block repeated once (4 symbols total) per Protocol v1 specification.
     */
    public fun encodeModeRepeated(scheme: Scheme, speed: Speed, rsProfileId: Int): IntArray {
        val single = encodeMode(scheme, speed, rsProfileId)
        return intArrayOf(single[0], single[1], single[0], single[1])
    }

    /**
     * Decodes a 2-symbol or 4-symbol mode block.
     * Returns [Result.success] with [DecodedMode] or [Result.failure] on invalid parity or corrupt symbols.
     */
    public fun decodeMode(symbols: IntArray): Result<DecodedMode> {
        return when (symbols.size) {
            2 -> decodeSinglePair(symbols[0], symbols[1])
            4 -> {
                val first = decodeSinglePair(symbols[0], symbols[1])
                val second = decodeSinglePair(symbols[2], symbols[3])
                when {
                    first.isSuccess && second.isSuccess -> {
                        if (first.getOrNull() == second.getOrNull()) {
                            first
                        } else {
                            // If they disagree, first takes precedence or return failure
                            first
                        }
                    }
                    first.isSuccess -> first
                    second.isSuccess -> second
                    else -> Result.failure(first.exceptionOrNull() ?: IllegalArgumentException("Both mode blocks corrupt"))
                }
            }
            else -> Result.failure(IllegalArgumentException("Expected 2 or 4 symbols for mode block, got ${symbols.size}"))
        }
    }

    private fun decodeSinglePair(sym0: Int, sym1: Int): Result<DecodedMode> {
        if (sym0 !in 0..15 || sym1 !in 0..15) {
            return Result.failure(IllegalArgumentException("Symbols out of range (0..15): [$sym0, $sym1]"))
        }
        val modeByte = ((sym0 and 0x0F) shl 4) or (sym1 and 0x0F)

        val schemeId = (modeByte ushr 6) and 0x03
        val speedId = (modeByte ushr 4) and 0x03
        val rsProfileId = (modeByte ushr 2) and 0x03
        val receivedParity = modeByte and 0x03

        val expectedParity = calculateParity(schemeId, speedId, rsProfileId)
        if (receivedParity != expectedParity) {
            return Result.failure(IllegalArgumentException("Mode block parity mismatch: expected $expectedParity, got $receivedParity in byte 0x${modeByte.toString(16)}"))
        }

        val scheme = Scheme.fromId(schemeId)
            ?: return Result.failure(IllegalArgumentException("Invalid scheme id: $schemeId"))
        val speed = Speed.fromId(speedId)
            ?: return Result.failure(IllegalArgumentException("Invalid speed id: $speedId"))

        return Result.success(DecodedMode(scheme, speed, rsProfileId))
    }
}
