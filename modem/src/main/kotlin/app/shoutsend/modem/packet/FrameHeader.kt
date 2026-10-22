package app.shoutsend.modem.packet

/**
 * 4-byte header for ShoutSend Protocol v1 transmission frames.
 *
 * Header Layout:
 * - Byte 0: version (bits 7..6 = 1), type (bits 5..3), flags (bits 2..0).
 * - Byte 1: messageId (0..255).
 * - Byte 2: fragmentIndex (bits 7..4 = 0..15), fragmentCount (bits 3..0 = (count - 1)).
 * - Byte 3: payloadLength (0..k-6).
 */
public data class FrameHeader(
    public val version: Int = 1,
    public val type: MessageType,
    public val flags: MessageFlags = MessageFlags.NONE,
    public val messageId: Int,
    public val fragmentIndex: Int,
    public val fragmentCount: Int,
    public val payloadLength: Int
) {
    init {
        require(version == 1) { "Protocol version must be 1: $version" }
        require(messageId in 0..255) { "messageId must be in 0..255: $messageId" }
        require(fragmentCount in 1..16) { "fragmentCount must be in 1..16: $fragmentCount" }
        require(fragmentIndex in 0 until fragmentCount) {
            "fragmentIndex must be in 0 until fragmentCount ($fragmentCount): $fragmentIndex"
        }
        require(payloadLength >= 0) { "payloadLength cannot be negative: $payloadLength" }
    }
}
