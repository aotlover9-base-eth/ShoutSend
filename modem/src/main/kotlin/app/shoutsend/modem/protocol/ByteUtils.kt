package app.shoutsend.modem.protocol

/**
 * Low-level byte utilities for protocol encoding, debugging, and constant-time crypto equality.
 */
public object ByteUtils {

    /**
     * Converts a byte array to a lowercase hex string.
     * FOR TEST AND DEBUG PURPOSES ONLY. Never log payload bytes in production.
     */
    public fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val chars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            chars[i * 2] = hexChars[v ushr 4]
            chars[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(chars)
    }

    /**
     * Constant-time comparison between two byte arrays to prevent timing side channels.
     */
    public fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
