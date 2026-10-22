package app.shoutsend.modem.profile

import app.shoutsend.modem.AudioSpec

/**
 * Defines the acoustic tone frequencies (sync and data tones) for a given [Band] and [AudioSpec].
 *
 * Validates that all frequencies lie safely below the Nyquist limit minus a 1.5 kHz guard margin,
 * and that minimum spacing between adjacent tones is at least 100 Hz.
 */
public class TonePlan(
    public val band: Band,
    public val audioSpec: AudioSpec
) {
    public val syncTone0: Double
    public val syncTone1: Double
    public val dataTones: List<Double>

    init {
        when (band) {
            Band.STEALTH -> {
                syncTone0 = 17450.0
                syncTone1 = 20000.0
                dataTones = List(16) { i -> 17600.0 + (i * 150.0) }
            }
            Band.RETRO -> {
                syncTone0 = 1375.0
                syncTone1 = 3500.0
                dataTones = List(16) { i -> 1500.0 + (i * 125.0) }
            }
        }

        // Validation against Nyquist minus 1.5 kHz margin
        val nyquistHz = audioSpec.sampleRateHz / 2.0
        val maxAllowedHz = nyquistHz - 1500.0

        val allTones = (dataTones + listOf(syncTone0, syncTone1)).sorted()

        for (tone in allTones) {
            require(tone <= maxAllowedHz) {
                "Tone frequency $tone Hz exceeds Nyquist guard limit ($maxAllowedHz Hz) for sample rate ${audioSpec.sampleRateHz} Hz."
            }
            require(tone > 0.0) {
                "Tone frequency must be positive, got $tone Hz."
            }
        }

        // Validate minimum 100 Hz tone spacing
        for (i in 0 until allTones.size - 1) {
            val spacing = allTones[i + 1] - allTones[i]
            require(spacing >= 100.0) {
                "Tone spacing ($spacing Hz) between ${allTones[i]} Hz and ${allTones[i + 1]} Hz is below minimum 100 Hz."
            }
        }
    }

    /**
     * Tone index 0..7 (lower group for DTFSK).
     */
    public val lowerTones: List<Double> = dataTones.subList(0, 8)

    /**
     * Tone index 8..15 (upper group for DTFSK).
     */
    public val upperTones: List<Double> = dataTones.subList(8, 16)

    /**
     * Retrieves the tone frequency for a data symbol (0..15).
     */
    public fun tone(index: Int): Double {
        require(index in dataTones.indices) { "Data tone index out of range: $index (expected 0..15)" }
        return dataTones[index]
    }
}
