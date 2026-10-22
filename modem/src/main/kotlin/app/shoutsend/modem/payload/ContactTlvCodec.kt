package app.shoutsend.modem.payload

import java.io.ByteArrayOutputStream

/**
 * Compact binary Tag-Length-Value (TLV) codec for contact exchange payloads.
 *
 * Structure:
 * Each field is encoded as:
 * - Tag: 1 byte
 * - Length: 1 byte (unsigned 0..255)
 * - Value: [Length] UTF-8 bytes
 *
 * Tags:
 * - 0x01: Display Name
 * - 0x02: Phone (up to 2 entries)
 * - 0x03: Email (up to 1 entry)
 */
public object ContactTlvCodec {

    public const val TAG_NAME: Byte = 0x01
    public const val TAG_PHONE: Byte = 0x02
    public const val TAG_EMAIL: Byte = 0x03

    /**
     * Serializes a [Payload.Contact] into compact TLV binary bytes.
     */
    public fun encode(contact: Payload.Contact): ByteArray {
        val out = ByteArrayOutputStream()

        // Name
        val nameBytes = contact.displayName.toByteArray(Charsets.UTF_8)
        out.write(TAG_NAME.toInt())
        out.write(nameBytes.size)
        out.write(nameBytes)

        // Phones (up to 2)
        for (phone in contact.phones.take(2)) {
            val phoneBytes = phone.toByteArray(Charsets.UTF_8)
            out.write(TAG_PHONE.toInt())
            out.write(phoneBytes.size)
            out.write(phoneBytes)
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
     * Parses binary TLV bytes into a validated [Payload.Contact], or returns null if malformed.
     */
    public fun decode(bytes: ByteArray): Payload.Contact? {
        if (bytes.isEmpty()) return null

        var offset = 0
        var name: String? = null
        val phones = mutableListOf<String>()
        var email: String? = null

        while (offset < bytes.size) {
            if (offset + 2 > bytes.size) {
                return null // Incomplete TLV header
            }
            val tag = bytes[offset++]
            val len = bytes[offset++].toInt() and 0xFF

            if (offset + len > bytes.size) {
                return null // Payload truncated
            }

            val str = String(bytes, offset, len, Charsets.UTF_8)
            offset += len

            when (tag) {
                TAG_NAME -> name = str
                TAG_PHONE -> {
                    if (phones.size < 2) {
                        phones.add(str)
                    } else {
                        return null // Too many phones
                    }
                }
                TAG_EMAIL -> {
                    if (email == null) {
                        email = str
                    } else {
                        return null // Too many emails
                    }
                }
                else -> return null // Unknown tag rejected in strict mode
            }
        }

        if (name == null) return null

        val contact = Payload.Contact(displayName = name, phones = phones, email = email)
        return if (contact.validate() is PayloadValidationResult.Valid) contact else null
    }
}
