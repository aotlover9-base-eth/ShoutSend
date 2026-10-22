package app.shoutsend.modem.packet

import app.shoutsend.modem.profile.RsProfile
import app.shoutsend.modem.protocol.Crc16

/**
 * Diagnostic error reasons for frame decoding failures.
 */
public sealed interface FrameDecodeError {
    public data class VersionMismatch(public val version: Int) : FrameDecodeError
    public data class BadCrc(public val expected: Int, public val actual: Int) : FrameDecodeError
    public data class BadLength(public val length: Int, public val maxAllowed: Int) : FrameDecodeError
    public data class BadFragmentField(public val index: Int, public val count: Int) : FrameDecodeError
    public data class InvalidSize(public val actual: Int, public val expected: Int) : FrameDecodeError
    public data class UnknownMessageType(public val typeId: Int) : FrameDecodeError
}

/**
 * Result of attempting to decode a frame from k bytes after Reed-Solomon error correction.
 */
public sealed interface FrameDecodeResult {
    public data class Success(public val frame: Frame) : FrameDecodeResult
    public data class Failure(public val error: FrameDecodeError) : FrameDecodeResult
}

/**
 * Validates and parses k-byte Reed-Solomon payload data into a verified [Frame].
 */
public object FrameDecoder {

    /**
     * Decodes and validates [bytes] against [profile].
     *
     * @param bytes Codeword data bytes of length [profile.k].
     * @param profile The active Reed-Solomon profile.
     * @return [FrameDecodeResult.Success] on valid frame, or [FrameDecodeResult.Failure] with the exact reason.
     */
    public fun decode(bytes: ByteArray, profile: RsProfile): FrameDecodeResult {
        val k = profile.k
        if (bytes.size != k) {
            return FrameDecodeResult.Failure(FrameDecodeError.InvalidSize(bytes.size, k))
        }

        // 1. Version check (bits 7..6 of byte 0 must be 1)
        val version = (bytes[0].toInt() ushr 6) and 0x03
        if (version != 1) {
            return FrameDecodeResult.Failure(FrameDecodeError.VersionMismatch(version))
        }

        // 2. Type check (bits 5..3 of byte 0)
        val typeId = (bytes[0].toInt() ushr 3) and 0x07
        val type = MessageType.fromId(typeId)
            ?: return FrameDecodeResult.Failure(FrameDecodeError.UnknownMessageType(typeId))

        // 3. Length bounds check (byte 3)
        val payloadLength = bytes[3].toInt() and 0xFF
        val maxPayload = k - 6
        if (payloadLength > maxPayload) {
            return FrameDecodeResult.Failure(FrameDecodeError.BadLength(payloadLength, maxPayload))
        }

        // 4. CRC verification over header (4 bytes) + payload (payloadLength bytes)
        val expectedCrc = ((bytes[k - 2].toInt() and 0xFF) shl 8) or (bytes[k - 1].toInt() and 0xFF)
        val actualCrc = Crc16.calculate(bytes, offset = 0, length = 4 + payloadLength)
        if (expectedCrc != actualCrc) {
            return FrameDecodeResult.Failure(FrameDecodeError.BadCrc(expected = expectedCrc, actual = actualCrc))
        }

        // 5. Fragment index and count verification (byte 2)
        val fragmentIndex = (bytes[2].toInt() ushr 4) and 0x0F
        val fragmentCount = (bytes[2].toInt() and 0x0F) + 1
        if (fragmentCount !in 1..16 || fragmentIndex >= fragmentCount) {
            return FrameDecodeResult.Failure(FrameDecodeError.BadFragmentField(fragmentIndex, fragmentCount))
        }

        // 6. Flags (bits 2..0 of byte 0) and messageId (byte 1)
        val flags = MessageFlags.fromByteValue(bytes[0].toInt() and 0x07)
        val messageId = bytes[1].toInt() and 0xFF

        val payload = bytes.copyOfRange(4, 4 + payloadLength)
        val header = FrameHeader(
            version = version,
            type = type,
            flags = flags,
            messageId = messageId,
            fragmentIndex = fragmentIndex,
            fragmentCount = fragmentCount,
            payloadLength = payloadLength
        )

        return FrameDecodeResult.Success(Frame(header, payload))
    }
}
