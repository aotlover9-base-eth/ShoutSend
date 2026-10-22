package app.shoutsend.modem.payload

import java.io.ByteArrayOutputStream

/**
 * Compact binary TLV encoder for contacts featuring 7-bit ASCII and 4-bit BCD phone packing.
 */
public object ContactCompactor {

    public const val TAG_NAME: Byte = 0x01
    public const val TAG_PHONE_RAW: Byte = 0x02
    public const val TAG_PHONE_7BIT: Byte = 0x12
    public const val TAG_PHONE_BCD: Byte = 0x22
    public const val TAG_EMAIL: Byte = 0x03

    /**
     * Compacts [contact] using 4-bit BCD for digit-only phones and 7-bit for ASCII phones.
     */
    public fun compact(contact: Payload.Contact): ByteArray {
        val out = ByteArrayOutputStream()

        // Name
        val nameBytes = contact.displayName.toByteArray(Charsets.UTF_8)
        out.write(TAG_NAME.toInt())
        out.write(nameBytes.size)
        out.write(nameBytes)

        // Phones (up to 2)
        for (phone in contact.phones.take(2)) {
            val isDigitOnly = phone.isNotEmpty() && phone.all { it in '0'..'9' }
            val isAscii = SevenBitPacker.is7BitAscii(phone)

            if (isDigitOnly) {
                // Pack 4 bits per digit
                val bcdBytes = packBcd(phone)
                out.write(TAG_PHONE_BCD.toInt())
                out.write(1 + bcdBytes.size) // length includes digitCount byte
                out.write(phone.length)
                out.write(bcdBytes)
            } else if (isAscii) {
                // Pack 7 bits per character
                val packed7 = SevenBitPacker.packRaw(phone)
                out.write(TAG_PHONE_7BIT.toInt())
                out.write(1 + packed7.size)
                out.write(phone.length)
                out.write(packed7)
            } else {
                val raw = phone.toByteArray(Charsets.UTF_8)
                out.write(TAG_PHONE_RAW.toInt())
                out.write(raw.size)
                out.write(raw)
            }
        }

        // Email (optional)
        if (contact.email != null) {
            val emailBytes = contact.email.toByteArray(Charsets.UTF_8)
            out.write(TAG_EMAIL.toInt())
            out.write(emailBytes.size)
            out.write(emailBytes)
        }

        return out.toByteArray()
    }

    /**
     * Expands compacted contact TLV bytes.
     * Returns null if malformed.
     */
    public fun expand(bytes: ByteArray): Payload.Contact? {
        if (bytes.isEmpty()) return null

        var offset = 0
        var name: String? = null
        val phones = mutableListOf<String>()
        var email: String? = null

        while (offset < bytes.size) {
            if (offset + 2 > bytes.size) return null
            val tag = bytes[offset++]
            val len = bytes[offset++].toInt() and 0xFF

            if (offset + len > bytes.size) return null

            when (tag) {
                TAG_NAME -> {
                    name = String(bytes, offset, len, Charsets.UTF_8)
                    offset += len
                }
                TAG_PHONE_RAW -> {
                    if (phones.size < 2) {
                        phones.add(String(bytes, offset, len, Charsets.UTF_8))
                    }
                    offset += len
                }
                TAG_PHONE_BCD -> {
                    if (len < 1) return null
                    val count = bytes[offset++].toInt() and 0xFF
                    val bcdLen = len - 1
                    val phone = unpackBcd(bytes, offset, bcdLen, count) ?: return null
                    if (phones.size < 2) {
                        phones.add(phone)
                    }
                    offset += bcdLen
                }
                TAG_PHONE_7BIT -> {
                    if (len < 1) return null
                    val count = bytes[offset++].toInt() and 0xFF
                    val packedLen = len - 1
                    val phone = SevenBitPacker.unpackRaw(bytes, offset, count) ?: return null
                    if (phones.size < 2) {
                        phones.add(phone)
                    }
                    offset += packedLen
                }
                TAG_EMAIL -> {
                    if (email == null) {
                        email = String(bytes, offset, len, Charsets.UTF_8)
                    }
                    offset += len
                }
                else -> return null
            }
        }

        if (name == null) return null
        val contact = Payload.Contact(displayName = name, phones = phones, email = email)
        return if (contact.validate() is PayloadValidationResult.Valid) contact else null
    }

    private fun packBcd(digits: String): ByteArray {
        val outLen = (digits.length + 1) / 2
        val out = ByteArray(outLen)
        for (i in 0 until digits.length) {
            val digit = digits[i] - '0'
            val byteIdx = i / 2
            if (i % 2 == 0) {
                out[byteIdx] = (digit shl 4).toByte()
            } else {
                out[byteIdx] = (out[byteIdx].toInt() or (digit and 0x0F)).toByte()
            }
        }
        return out
    }

    private fun unpackBcd(bytes: ByteArray, offset: Int, byteCount: Int, digitCount: Int): String? {
        val expectedBytes = (digitCount + 1) / 2
        if (byteCount < expectedBytes) return null
        val chars = CharArray(digitCount)
        for (i in 0 until digitCount) {
            val byteVal = bytes[offset + (i / 2)].toInt() and 0xFF
            val digit = if (i % 2 == 0) {
                (byteVal ushr 4) and 0x0F
            } else {
                byteVal and 0x0F
            }
            if (digit > 9) return null
            chars[i] = ('0' + digit)
        }
        return String(chars)
    }
}
