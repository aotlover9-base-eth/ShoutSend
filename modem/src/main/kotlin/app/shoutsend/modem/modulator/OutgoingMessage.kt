package app.shoutsend.modem.modulator

import app.shoutsend.modem.packet.MessageFlags
import app.shoutsend.modem.packet.MessageType
import kotlin.random.Random

/**
 * High-level message container prepared for physical acoustic modulation.
 *
 * @property type Application payload type indicator (Text, Url, Wi-Fi, Contact).
 * @property flags Modifiers including encryption and compaction state.
 * @property payload Raw or encoded byte array to transmit over the air (1..320 bytes).
 * @property messageId Random 8-bit identifier (0..255) binding fragments belonging to this message.
 */
public data class OutgoingMessage(
    public val type: MessageType,
    public val flags: MessageFlags = MessageFlags.NONE,
    public val payload: ByteArray,
    public val messageId: Int = Random.nextInt(0, 256)
) {
    init {
        require(payload.isNotEmpty()) { "Payload cannot be empty" }
        require(payload.size <= 320) { "Payload size (${payload.size}) exceeds 320 bytes limit" }
        require(messageId in 0..255) { "messageId must be in 0..255, got $messageId" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutgoingMessage) return false
        if (type != other.type) return false
        if (flags != other.flags) return false
        if (messageId != other.messageId) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + flags.hashCode()
        result = 31 * result + messageId
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
