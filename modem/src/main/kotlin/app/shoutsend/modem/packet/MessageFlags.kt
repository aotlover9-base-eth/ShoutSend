package app.shoutsend.modem.packet

/**
 * Message-level flags encoded in the lower 3 bits (bits 2..0) of Frame Header Byte 0.
 *
 * @property isEncrypted Indicates payload is encrypted via AES-GCM (Step 15).
 * @property isCompact Indicates payload is compacted using Brotli/Huffman (Step 14).
 * @property isReserved Reserved for future protocol extension (bit 0).
 */
public data class MessageFlags(
    public val isEncrypted: Boolean = false,
    public val isCompact: Boolean = false,
    public val isReserved: Boolean = false
) {
    /**
     * Serializes flags into a 3-bit integer representation (0..7).
     */
    public fun toByteValue(): Int {
        var v = 0
        if (isEncrypted) v = v or 0x04
        if (isCompact) v = v or 0x02
        if (isReserved) v = v or 0x01
        return v
    }

    public companion object {
        public val NONE: MessageFlags = MessageFlags()

        /**
         * Deserializes a 3-bit flag field.
         */
        public fun fromByteValue(byteValue: Int): MessageFlags = MessageFlags(
            isEncrypted = (byteValue and 0x04) != 0,
            isCompact = (byteValue and 0x02) != 0,
            isReserved = (byteValue and 0x01) != 0
        )
    }
}
