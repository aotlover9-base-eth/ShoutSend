package app.shoutsend.modem.packet

import app.shoutsend.modem.profile.RsProfile
import app.shoutsend.modem.protocol.Crc16

/**
 * Builds unencoded [Frame] objects into [k]-byte blocks ready for systematic Reed-Solomon FEC encoding.
 */
public object FrameEncoder {

    /**
     * Encodes [frame] into a [profile.k]-byte array with header fields, zero padding, and CRC-16.
     *
     * @param frame The unencoded frame metadata and payload.
     * @param profile The active Reed-Solomon profile defining data size k.
     * @return [ByteArray] of exact size [profile.k].
     */
    public fun encode(frame: Frame, profile: RsProfile): ByteArray {
        val k = profile.k
        val maxPayload = k - 6
        require(frame.payload.size <= maxPayload) {
            "Payload size (${frame.payload.size}) exceeds max capacity ($maxPayload) for profile $profile"
        }

        val out = ByteArray(k)
        val header = frame.header

        // Byte 0: version (2 bits), type (3 bits), flags (3 bits)
        val b0 = ((header.version and 0x03) shl 6) or
            ((header.type.id and 0x07) shl 3) or
            (header.flags.toByteValue() and 0x07)
        out[0] = b0.toByte()

        // Byte 1: messageId (8 bits)
        out[1] = (header.messageId and 0xFF).toByte()

        // Byte 2: fragmentIndex (4 bits), fragmentCount - 1 (4 bits)
        val b2 = ((header.fragmentIndex and 0x0F) shl 4) or
            ((header.fragmentCount - 1) and 0x0F)
        out[2] = b2.toByte()

        // Byte 3: payloadLength (8 bits)
        out[3] = (header.payloadLength and 0xFF).toByte()

        // Bytes 4 until 4 + payloadLength: payload
        System.arraycopy(frame.payload, 0, out, 4, header.payloadLength)

        // Zero-padding is guaranteed as ByteArray is initialized to 0s.

        // CRC-16 over bytes 0 until 4 + payloadLength (header + payload)
        val crc = Crc16.calculate(out, offset = 0, length = 4 + header.payloadLength)
        out[k - 2] = ((crc ushr 8) and 0xFF).toByte()
        out[k - 1] = (crc and 0xFF).toByte()

        return out
    }
}
