package app.shoutsend.modem.profile

public enum class Band(public val id: Int, public val title: String) {
    STEALTH(id = 0, title = "Stealth (17.6 - 20.0 kHz)"),
    RETRO(id = 1, title = "Retro (1.5 - 3.5 kHz)");

    public companion object {
        public fun fromId(id: Int): Band? = entries.find { it.id == id }
    }
}
