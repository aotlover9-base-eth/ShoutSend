package app.shoutsend.modem.protocol

/**
 * High-performance bit-level serializer appending bits MSB first.
 * Does not allocate per bit or per byte write.
 */
public class BitWriter(initialByteCapacity: Int = 64) {

    private var buffer: ByteArray = ByteArray(initialByteCapacity.coerceAtLeast(8))
    private var bitCount: Int = 0

    public val totalBits: Int
        get() = bitCount

    public val byteLength: Int
        get() = (bitCount + 7) / 8

    /**
     * Writes [numBits] (0..32) from [value], MSB first.
     */
    public fun writeBits(value: Int, numBits: Int): BitWriter {
        require(numBits in 0..32) { "numBits must be between 0 and 32, got $numBits" }
        if (numBits == 0) return this

        ensureCapacity(bitCount + numBits)

        for (i in numBits - 1 downTo 0) {
            val bit = (value ushr i) and 1
            val byteIndex = bitCount ushr 3
            val bitOffset = 7 - (bitCount and 7)

            if (bit != 0) {
                buffer[byteIndex] = (buffer[byteIndex].toInt() or (1 shl bitOffset)).toByte()
            }
            bitCount++
        }
        return this
    }

    public fun writeBit(bit: Int): BitWriter = writeBits(bit and 1, 1)

    public fun writeBit(bit: Boolean): BitWriter = writeBits(if (bit) 1 else 0, 1)

    public fun writeByte(b: Int): BitWriter = writeBits(b and 0xFF, 8)

    public fun writeBytes(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): BitWriter {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) { "Invalid offset/length" }
        for (i in 0 until length) {
            writeByte(bytes[offset + i].toInt() and 0xFF)
        }
        return this
    }

    /**
     * Returns a copy of the written bytes. Trailing bits in the final byte are zero-padded.
     */
    public fun toByteArray(): ByteArray {
        val result = ByteArray(byteLength)
        System.arraycopy(buffer, 0, result, 0, byteLength)
        return result
    }

    private fun ensureCapacity(requiredBits: Int) {
        val requiredBytes = (requiredBits + 7) / 8
        if (requiredBytes > buffer.size) {
            var newCap = buffer.size * 2
            while (newCap < requiredBytes) {
                newCap *= 2
            }
            val newBuf = ByteArray(newCap)
            System.arraycopy(buffer, 0, newBuf, 0, buffer.size)
            buffer = newBuf
        }
    }
}
