package app.shoutsend.modem.packet

import app.shoutsend.modem.profile.RsProfile
import kotlin.random.Random

/**
 * Splits arbitrary on-air message payloads into framed fragments.
 */
public object Fragmenter {

    /**
     * Absolute upper limit on unencoded payload bytes transmitted over the air in Protocol v1.
     */
    public const val MAX_AIR_BYTES: Int = 320

    /**
     * Maximum fragments allowed per message (4-bit representation limit).
     */
    public const val MAX_FRAGMENTS: Int = 16

    /**
     * Splits [payload] into a sequence of [Frame] objects with a specified [messageId].
     *
     * @param payload The complete payload bytes to transmit. Must be 1..[MAX_AIR_BYTES].
     * @param type Message payload type.
     * @param flags Message flags (encryption, compaction).
     * @param profile The active Reed-Solomon profile.
     * @param messageId Single byte identifier (0..255).
     * @return Ordered list of [Frame] instances ready for encoding.
     */
    public fun fragment(
        payload: ByteArray,
        type: MessageType,
        flags: MessageFlags = MessageFlags.NONE,
        profile: RsProfile = RsProfile.STANDARD,
        messageId: Int
    ): List<Frame> {
        require(payload.isNotEmpty()) { "Payload cannot be empty" }
        require(payload.size <= MAX_AIR_BYTES) {
            "Payload size (${payload.size}) exceeds MAX_AIR_BYTES ($MAX_AIR_BYTES)"
        }
        require(messageId in 0..255) { "messageId must be in 0..255: $messageId" }

        val maxPayloadPerFrame = profile.k - 6
        val fragmentCount = (payload.size + maxPayloadPerFrame - 1) / maxPayloadPerFrame
        require(fragmentCount in 1..MAX_FRAGMENTS) {
            "Computed fragmentCount ($fragmentCount) exceeds MAX_FRAGMENTS ($MAX_FRAGMENTS)"
        }

        val frames = ArrayList<Frame>(fragmentCount)
        for (i in 0 until fragmentCount) {
            val start = i * maxPayloadPerFrame
            val end = minOf(start + maxPayloadPerFrame, payload.size)
            val slice = payload.copyOfRange(start, end)
            val header = FrameHeader(
                version = 1,
                type = type,
                flags = flags,
                messageId = messageId,
                fragmentIndex = i,
                fragmentCount = fragmentCount,
                payloadLength = slice.size
            )
            frames.add(Frame(header, slice))
        }
        return frames
    }

    /**
     * Splits [payload] using an injected random source for generating [messageId].
     */
    public fun fragment(
        payload: ByteArray,
        type: MessageType,
        flags: MessageFlags = MessageFlags.NONE,
        profile: RsProfile = RsProfile.STANDARD,
        randomSource: () -> Int = { Random.nextInt(0, 256) }
    ): List<Frame> = fragment(
        payload = payload,
        type = type,
        flags = flags,
        profile = profile,
        messageId = randomSource() and 0xFF
    )
}
