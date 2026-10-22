package app.shoutsend.modem.packet

/**
 * High-level message types supported by the ShoutSend Protocol v1.
 *
 * Encoded as a 3-bit field (bits 5..3 of Frame Header Byte 0).
 * Values 4 through 7 are reserved for future protocol revisions.
 */
public enum class MessageType(public val id: Int) {
    TEXT(0),
    URL(1),
    WIFI(2),
    CONTACT(3);

    public companion object {
        /**
         * Resolves a [MessageType] by its 3-bit protocol identifier.
         * Returns null if [id] is unassigned or reserved (4..7).
         */
        public fun fromId(id: Int): MessageType? = entries.firstOrNull { it.id == id }
    }
}
