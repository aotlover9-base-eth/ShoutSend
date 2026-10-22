package app.shoutsend.modem.protocol

/**
 * Typed exception thrown when attempting to read past the end of a bit stream.
 */
public class BitStreamException(message: String) : Exception(message)

/**
 * Sequential bit-level deserializer reading bits MSB first.
 */
public class BitReader(
    private val buffer: ByteArray,
    public val totalBits: Int = buffer.size * 8
) {
    private var bitPosition: Int = 0

    init {
        require(totalBits in 0..(buffer.size * 8)) {
            "totalBits ($totalBits) exceeds buffer capacity (${buffer.size * 8})"
        }
    }

    public val remainingBits: Int
        get() = (totalBits - bitPosition).coerceAtLeast(0)

    public val hasRemaining: Boolean
        get() = remainingBits > 0

    /**
     * Reads [numBits] (0..32), MSB first.
     */
    public fun readBits(numBits: Int): Int {
        require(numBits in 0..32) { "numBits must be between 0 and 32, got $numBits" }
        if (numBits == 0) return 0
        if (remainingBits < numBits) {
            throw BitStreamException("Underflow: requested $numBits bits, but only $remainingBits remaining")
        }

        var result = 0
        for (i in 0 until numBits) {
            val byteIndex = bitPosition ushr 3
            val bitOffset = 7 - (bitPosition and 7)
            val bit = (buffer[byteIndex].toInt() ushr bitOffset) and 1
            result = (result shl 1) or bit
            bitPosition++
        }
        return result
    }

    public fun readBit(): Int = readBits(1)

    public fun readByte(): Int = readBits(8)

    public fun readBytes(count: Int): ByteArray {
        val out = ByteArray(count)
        for (i in 0 until count) {
            out[i] = readByte().toByte()
        }
        return out
    }
}
