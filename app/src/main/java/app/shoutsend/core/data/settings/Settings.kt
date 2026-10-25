package app.shoutsend.core.data.settings

import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed

/**
 * Immutable user settings model holding all preferences for ShoutSend.
 *
 * Contains zero developer panel keys or developer overrides (Rule 15).
 * Valid ranges:
 * - [repeats]: 1..3
 * - [historyMaxEntries]: 50..2000
 * - [backgroundGraceSeconds]: 0..30
 */
data class Settings(
    val band: Band = DEFAULT_BAND,
    val speed: Speed = DEFAULT_SPEED,
    val repeats: Int = DEFAULT_REPEATS,
    val themeMode: ThemeMode = DEFAULT_THEME_MODE,
    val dynamicColor: Boolean = DEFAULT_DYNAMIC_COLOR,
    val hapticsEnabled: Boolean = DEFAULT_HAPTICS_ENABLED,
    val soundEffectsEnabled: Boolean = DEFAULT_SOUND_EFFECTS_ENABLED,
    val secureModeDefault: Boolean = DEFAULT_SECURE_MODE_DEFAULT,
    val historyEnabled: Boolean = DEFAULT_HISTORY_ENABLED,
    val historyMaxEntries: Int = DEFAULT_HISTORY_MAX_ENTRIES,
    val keepScreenOnDuringSessions: Boolean = DEFAULT_KEEP_SCREEN_ON,
    val lastCalibration: String? = DEFAULT_LAST_CALIBRATION,
    val onboardingCompleted: Boolean = DEFAULT_ONBOARDING_COMPLETED,
    val micPermissionRequested: Boolean = DEFAULT_MIC_PERMISSION_REQUESTED,
    val stealthWarningAccepted: Boolean = DEFAULT_STEALTH_WARNING_ACCEPTED,
    val backgroundGraceSeconds: Int = DEFAULT_BACKGROUND_GRACE_SECONDS,
    val lastTab: String = DEFAULT_LAST_TAB
) {
    init {
        require(repeats in REPEATS_MIN..REPEATS_MAX) {
            "repeats ($repeats) must be in $REPEATS_MIN..$REPEATS_MAX"
        }
        require(historyMaxEntries in HISTORY_MIN..HISTORY_MAX) {
            "historyMaxEntries ($historyMaxEntries) must be in $HISTORY_MIN..$HISTORY_MAX"
        }
        require(backgroundGraceSeconds in GRACE_MIN..GRACE_MAX) {
            "backgroundGraceSeconds ($backgroundGraceSeconds) must be in $GRACE_MIN..$GRACE_MAX"
        }
    }

    companion object {
        val DEFAULT_BAND: Band = Band.RETRO
        val DEFAULT_SPEED: Speed = Speed.NORMAL
        const val DEFAULT_REPEATS: Int = 2
        val DEFAULT_THEME_MODE: ThemeMode = ThemeMode.DARK
        const val DEFAULT_DYNAMIC_COLOR: Boolean = false
        const val DEFAULT_HAPTICS_ENABLED: Boolean = true
        const val DEFAULT_SOUND_EFFECTS_ENABLED: Boolean = true
        const val DEFAULT_SECURE_MODE_DEFAULT: Boolean = false
        const val DEFAULT_HISTORY_ENABLED: Boolean = true
        const val DEFAULT_HISTORY_MAX_ENTRIES: Int = 500
        const val DEFAULT_KEEP_SCREEN_ON: Boolean = true
        val DEFAULT_LAST_CALIBRATION: String? = null
        const val DEFAULT_ONBOARDING_COMPLETED: Boolean = false
        const val DEFAULT_MIC_PERMISSION_REQUESTED: Boolean = false
        const val DEFAULT_STEALTH_WARNING_ACCEPTED: Boolean = false
        const val DEFAULT_BACKGROUND_GRACE_SECONDS: Int = 5
        const val DEFAULT_LAST_TAB: String = "receive"

        const val REPEATS_MIN: Int = 1
        const val REPEATS_MAX: Int = 3
        const val HISTORY_MIN: Int = 50
        const val HISTORY_MAX: Int = 2000
        const val GRACE_MIN: Int = 0
        const val GRACE_MAX: Int = 30
    }
}
