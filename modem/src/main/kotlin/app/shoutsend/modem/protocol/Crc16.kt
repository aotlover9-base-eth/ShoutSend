package app.shoutsend.modem.protocol

/**
 * Table-driven CRC-16/CCITT-FALSE implementation.
 *
 * Parameters:
 * - Polynomial: 0x1021 (x^16 + x^12 + x^5 + 1)
 * - Initial value: 0xFFFF
 * - RefIn: false
 * - RefOut: false
 * - Final XOR: 0x0000
 *
 * Known Answer: ASCII "123456789" produces 0x29B1.
 */
public object Crc16 {

    public const val POLYNOMIAL: Int = 0x1021
    public const val INITIAL_VALUE: Int = 0xFFFF

    private val TABLE: IntArray = IntArray(256) { i ->
        var curr = i shl 8
        for (bit in 0 until 8) {
            curr = if ((curr and 0x8000) != 0) {
                ((curr shl 1) xor POLYNOMIAL) and 0xFFFF
            } else {
                (curr shl 1) and 0xFFFF
            }
        }
        curr
    }

    /**
     * Calculates the CRC-16/CCITT-FALSE over [bytes].
     */
    public fun calculate(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
        return update(INITIAL_VALUE, bytes, offset, length)
    }

    /**
     * Updates an existing CRC-16 value with a single [byte].
     */
    public fun update(currentCrc: Int, byte: Byte): Int {
        val tableIndex = ((currentCrc ushr 8) xor (byte.toInt() and 0xFF)) and 0xFF
        return ((currentCrc shl 8) xor TABLE[tableIndex]) and 0xFFFF
    }

    /**
     * Updates an existing CRC-16 value with multiple [bytes].
     */
    public fun update(currentCrc: Int, bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) {
            "Invalid offset/length for CRC update"
        }
        var crc = currentCrc
        for (i in 0 until length) {
            val tableIndex = ((crc ushr 8) xor (bytes[offset + i].toInt() and 0xFF)) and 0xFF
            crc = ((crc shl 8) xor TABLE[tableIndex]) and 0xFFFF
        }
        return crc
    }
}
