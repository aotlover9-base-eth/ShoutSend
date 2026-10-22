package app.shoutsend.modem.payload

import app.shoutsend.modem.protocol.BitReader
import app.shoutsend.modem.protocol.BitWriter

/**
 * Packs and unpacks 7-bit ASCII text strings into dense bitstreams.
 */
public object SevenBitPacker {

    /**
     * Returns true if all characters in [s] fit in 7-bit ASCII (0..127).
     */
    public fun is7BitAscii(s: String): Boolean {
        for (i in 0 until s.length) {
            if (s[i].code > 127) return false
        }
        return true
    }

    /**
     * Packs [s] into a length-prefixed byte array (2 bytes char count + 7-bit packed stream).
     */
    public fun pack(s: String): ByteArray {
        require(is7BitAscii(s)) { "String contains non-7-bit characters" }
        val charCount = s.length

        val writer = BitWriter()
        for (i in 0 until charCount) {
            writer.writeBits(s[i].code and 0x7F, 7)
        }
        val bits = writer.toByteArray()

        val out = ByteArray(2 + bits.size)
        out[0] = ((charCount ushr 8) and 0xFF).toByte()
        out[1] = (charCount and 0xFF).toByte()
        System.arraycopy(bits, 0, out, 2, bits.size)
        return out
    }

    /**
     * Unpacks 7-bit ASCII packed bytes into a String.
     * Returns null if [bytes] is truncated or invalid.
     */
    public fun unpack(bytes: ByteArray, offset: Int = 0): String? {
        if (bytes.size - offset < 2) return null
        val charCount = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
        val neededBits = charCount * 7
        val neededBytes = (neededBits + 7) / 8
        if (bytes.size - offset - 2 < neededBytes) return null

        val slice = bytes.copyOfRange(offset + 2, bytes.size)
        return try {
            val reader = BitReader(slice, totalBits = neededBits)
            val chars = CharArray(charCount)
            for (i in 0 until charCount) {
                chars[i] = (reader.readBits(7) and 0x7F).toChar()
            }
            String(chars)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Raw pack without 2-byte prefix, writing exactly [s.length] 7-bit characters.
     */
    public fun packRaw(s: String): ByteArray {
        val writer = BitWriter()
        for (i in 0 until s.length) {
            writer.writeBits(s[i].code and 0x7F, 7)
        }
        return writer.toByteArray()
    }

    /**
     * Raw unpack without 2-byte prefix, reading exactly [charCount] 7-bit characters.
     */
    public fun unpackRaw(bytes: ByteArray, offset: Int, charCount: Int): String? {
        val neededBits = charCount * 7
        val neededBytes = (neededBits + 7) / 8
        if (bytes.size - offset < neededBytes) return null

        val slice = bytes.copyOfRange(offset, bytes.size)
        return try {
            val reader = BitReader(slice, totalBits = neededBits)
            val chars = CharArray(charCount)
            for (i in 0 until charCount) {
                chars[i] = (reader.readBits(7) and 0x7F).toChar()
            }
            String(chars)
        } catch (_: Exception) {
            null
        }
    }
}
