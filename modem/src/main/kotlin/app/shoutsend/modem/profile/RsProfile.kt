package app.shoutsend.modem.profile

public enum class RsProfile(
    public val id: Int,
    public val codewordBytes: Int,
    public val dataBytes: Int,
    public val parityBytes: Int
) {
    RS_60_48(id = 0, codewordBytes = 60, dataBytes = 48, parityBytes = 12),
    RS_40_28(id = 1, codewordBytes = 40, dataBytes = 28, parityBytes = 12),
    RS_24_16(id = 2, codewordBytes = 24, dataBytes = 16, parityBytes = 8),
    RS_RESERVED(id = 3, codewordBytes = 0, dataBytes = 0, parityBytes = 0);

    public companion object {
        public fun fromId(id: Int): RsProfile? = entries.find { it.id == id }
    }
}
