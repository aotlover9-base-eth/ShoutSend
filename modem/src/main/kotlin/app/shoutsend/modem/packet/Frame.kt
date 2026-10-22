package app.shoutsend.modem.packet

/**
 * Represents a single transmission frame before FEC encoding or after FEC decoding.
 *
 * @property header The parsed 4-byte frame header metadata.
 * @property payload The payload slice carried by this frame (size matches header.payloadLength).
 */
public data class Frame(
    public val header: FrameHeader,
    public val payload: ByteArray
) {
    init {
        require(payload.size == header.payloadLength) {
            "Payload size (${payload.size}) must match header.payloadLength (${header.payloadLength})"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Frame) return false
        if (header != other.header) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
