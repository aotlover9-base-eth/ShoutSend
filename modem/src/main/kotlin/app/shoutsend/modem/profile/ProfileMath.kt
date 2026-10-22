package app.shoutsend.modem.profile

/**
 * Mathematical calculations for acoustic transmission speeds, bit rates, and symbol throughput.
 */
public object ProfileMath {

    /**
     * Computes the raw transmission bit rate (bps) on air for the given [profile].
     */
    public fun bitsPerSecond(profile: ModemProfile): Double {
        val symbolDurationSec = profile.speed.durationMs.toDouble() / 1000.0
        return profile.scheme.bitsPerSymbol.toDouble() / symbolDurationSec
    }

    /**
     * Computes the raw transmission byte rate (B/s) on air for the given [profile].
     */
    public fun rawBytesPerSecond(profile: ModemProfile): Double {
        return bitsPerSecond(profile) / 8.0
    }
}
