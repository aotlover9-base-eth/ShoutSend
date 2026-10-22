package app.shoutsend.modem.profile

public data class ModemProfile(
    public val band: Band,
    public val scheme: Scheme,
    public val speed: Speed,
    public val repeats: Int = 2,
    public val rsProfileId: Int = 0
) {
    init {
        require(repeats in 1..3) { "Repeats must be between 1 and 3, got $repeats" }
        require(rsProfileId in 0..3) { "RS profile ID must fit in 2 bits (0..3), got $rsProfileId" }
    }

    public val rsProfile: RsProfile
        get() = RsProfile.fromId(rsProfileId) ?: RsProfile.STANDARD

    public companion object {
        public val DEFAULT_STEALTH: ModemProfile = ModemProfile(
            band = Band.STEALTH,
            scheme = Scheme.MFSK16,
            speed = Speed.NORMAL,
            repeats = 2,
            rsProfileId = 0
        )

        public val DEFAULT_RETRO: ModemProfile = ModemProfile(
            band = Band.RETRO,
            scheme = Scheme.MFSK16,
            speed = Speed.NORMAL,
            repeats = 2,
            rsProfileId = 0
        )
    }
}
