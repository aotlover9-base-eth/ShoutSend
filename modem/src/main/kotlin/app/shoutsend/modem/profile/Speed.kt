package app.shoutsend.modem.profile

public enum class Speed(
    public val id: Int,
    public val durationMs: Int,
    public val rampMs: Int
) {
    ROBUST(id = 0, durationMs = 60, rampMs = 6),
    NORMAL(id = 1, durationMs = 30, rampMs = 4),
    FAST(id = 2, durationMs = 20, rampMs = 3);

    public companion object {
        public fun fromId(id: Int): Speed? = entries.find { it.id == id }
    }
}
