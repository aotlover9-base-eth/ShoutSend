package app.shoutsend.core.modem

import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.tuning.ModemTuning

/**
 * Aggregated runtime configuration for acoustic transceivers.
 *
 * Combines user preferences (Band, Speed, Repeats) with physical layer DSP tuning ([ModemTuning])
 * and an optional hardware audio source override (developer tuning only, null in User APK).
 */
data class ModemConfig(
    val band: Band = Band.RETRO,
    val speed: Speed = Speed.NORMAL,
    val repeats: Int = 2,
    val scheme: Scheme = Scheme.MFSK16,
    val rsProfileId: Int = 1,
    val tuning: ModemTuning = ModemTuning.DEFAULT,
    val audioSourceOverride: Int? = null
) {
    /**
     * Resolves the corresponding low-level [ModemProfile] for physical modulation.
     */
    val profile: ModemProfile
        get() = ModemProfile(
            band = band,
            scheme = scheme,
            speed = speed,
            repeats = repeats,
            rsProfileId = rsProfileId
        )
}
