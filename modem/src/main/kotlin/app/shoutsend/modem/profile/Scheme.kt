package app.shoutsend.modem.profile

public enum class Scheme(
    public val id: Int,
    public val bitsPerSymbol: Int,
    public val tonesPerSymbol: Int
) {
    MFSK16(id = 0, bitsPerSymbol = 4, tonesPerSymbol = 1),
    DTFSK(id = 1, bitsPerSymbol = 6, tonesPerSymbol = 2);

    public companion object {
        public fun fromId(id: Int): Scheme? = entries.find { it.id == id }
    }
}
