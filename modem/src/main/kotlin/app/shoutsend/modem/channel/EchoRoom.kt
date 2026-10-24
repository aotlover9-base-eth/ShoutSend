package app.shoutsend.modem.channel

/**
 * A multipath echo reflection tap specifying delay and attenuation.
 *
 * @property delayMs Delay in milliseconds relative to direct path.
 * @property gainDb Gain in decibels (typically negative, e.g. -6.0 dB).
 */
public data class EchoTap(
    public val delayMs: Double,
    public val gainDb: Double
)

/**
 * Preset multipath echo environments modeled by discrete reflection taps.
 */
public enum class RoomPreset {
    NONE,
    BATHROOM,
    LIVING_ROOM,
    HALL;

    /**
     * Returns the list of multipath reflection taps for this room preset.
     */
    public fun taps(): List<EchoTap> = when (this) {
        NONE -> emptyList()
        // Bathroom: short distance, hard reflective tiles (strong, short delays)
        BATHROOM -> listOf(
            EchoTap(delayMs = 4.0, gainDb = -5.0),
            EchoTap(delayMs = 9.0, gainDb = -8.0),
            EchoTap(delayMs = 15.0, gainDb = -12.0)
        )
        // Living room: medium distance, soft furnishings, carpets (medium delay, moderate attenuation)
        LIVING_ROOM -> listOf(
            EchoTap(delayMs = 12.0, gainDb = -10.0),
            EchoTap(delayMs = 25.0, gainDb = -15.0),
            EchoTap(delayMs = 45.0, gainDb = -20.0)
        )
        // Hall: large volume, distant walls (long delay, highly dispersed/weak reflections)
        HALL -> listOf(
            EchoTap(delayMs = 35.0, gainDb = -14.0),
            EchoTap(delayMs = 70.0, gainDb = -18.0),
            EchoTap(delayMs = 120.0, gainDb = -24.0)
        )
    }
}
