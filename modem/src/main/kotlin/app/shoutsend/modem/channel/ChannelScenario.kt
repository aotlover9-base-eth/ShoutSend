package app.shoutsend.modem.channel

/**
 * Deterministic acoustic channel impairment scenario.
 *
 * All parameters model physical acoustic propagation, ambient environment,
 * and transducer hardware behavior without depending on native platform audio.
 *
 * @property name Human-readable identifier for the scenario.
 * @property gainDb Overall signal attenuation or boost in decibels.
 * @property targetSnrDb Additive White Gaussian Noise target SNR in signal band (null for no AWGN).
 * @property ambientNoiseDbfs Ambient pink noise and low-frequency hum floor in dBFS (null for none).
 * @property room Multipath acoustic reflection room model.
 * @property phone Transducer speaker/microphone frequency response model.
 * @property clockDriftPpm Transceiver hardware clock drift in parts-per-million.
 * @property leadInSilenceRangeMs Random prepended silence interval in milliseconds.
 * @property slowFading Enables slow path-loss fading between 0.5 and 1.0 amplitude.
 * @property dropoutsAndClipping Enables intermittent acoustic dropouts (20-80 ms) and clipping bursts.
 */
public data class ChannelScenario(
    public val name: String,
    public val gainDb: Double = 0.0,
    public val targetSnrDb: Double? = null,
    public val ambientNoiseDbfs: Double? = null,
    public val room: RoomPreset = RoomPreset.NONE,
    public val phone: PhoneHardwarePreset = PhoneHardwarePreset.FLAT,
    public val clockDriftPpm: Double = 0.0,
    public val leadInSilenceRangeMs: IntRange = 0..0,
    public val slowFading: Boolean = false,
    public val dropoutsAndClipping: Boolean = false
) {
    public companion object {
        /** Ideal noiseless channel. */
        public val CLEAN: ChannelScenario = ChannelScenario(
            name = "CLEAN",
            gainDb = 0.0
        )

        /** Typical domestic room with low background noise. */
        public val QUIET_ROOM: ChannelScenario = ChannelScenario(
            name = "QUIET_ROOM",
            gainDb = -3.0,
            targetSnrDb = 25.0,
            ambientNoiseDbfs = -60.0,
            room = RoomPreset.LIVING_ROOM,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = 15.0
        )

        /** Living room with furniture, television/appliance hum, and conversational acoustics. */
        public val LIVING_ROOM: ChannelScenario = ChannelScenario(
            name = "LIVING_ROOM",
            gainDb = -6.0,
            targetSnrDb = 18.0,
            ambientNoiseDbfs = -45.0,
            room = RoomPreset.LIVING_ROOM,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = -25.0,
            slowFading = true
        )

        /** Public indoor venue with loud chatter, clatter, and reverberation. */
        public val CAFE: ChannelScenario = ChannelScenario(
            name = "CAFE",
            gainDb = -10.0,
            targetSnrDb = 12.0,
            ambientNoiseDbfs = -32.0,
            room = RoomPreset.HALL,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = 40.0,
            dropoutsAndClipping = true
        )

        /** High-noise outdoor urban environment with low-frequency vehicle rumble. */
        public val NOISY_STREET: ChannelScenario = ChannelScenario(
            name = "NOISY_STREET",
            gainDb = -14.0,
            targetSnrDb = 8.0,
            ambientNoiseDbfs = -25.0,
            room = RoomPreset.NONE,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = -50.0,
            dropoutsAndClipping = true
        )

        /** Device with speaker/microphone roll-off above 17 kHz (tests ultrasound degradation). */
        public val WEAK_ULTRASOUND_PHONE: ChannelScenario = ChannelScenario(
            name = "WEAK_ULTRASOUND_PHONE",
            gainDb = -6.0,
            targetSnrDb = 20.0,
            room = RoomPreset.LIVING_ROOM,
            phone = PhoneHardwarePreset.WEAK_ULTRASOUND_PHONE,
            clockDriftPpm = 10.0
        )

        /** Phones placed adjacent to each other on a desk (high SNR, minimal path loss). */
        public val CLOSE_RANGE: ChannelScenario = ChannelScenario(
            name = "CLOSE_RANGE",
            gainDb = 2.0,
            targetSnrDb = 32.0,
            ambientNoiseDbfs = -65.0,
            room = RoomPreset.NONE,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = 0.0
        )

        /** Large distance separation with heavy path loss and weak acoustic signal. */
        public val FAR_RANGE: ChannelScenario = ChannelScenario(
            name = "FAR_RANGE",
            gainDb = -18.0,
            targetSnrDb = 10.0,
            ambientNoiseDbfs = -50.0,
            room = RoomPreset.HALL,
            phone = PhoneHardwarePreset.GOOD_PHONE,
            clockDriftPpm = 30.0,
            slowFading = true
        )
    }
}
