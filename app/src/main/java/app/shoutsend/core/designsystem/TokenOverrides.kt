package app.shoutsend.core.designsystem

import kotlinx.serialization.Serializable

/**
 * Sparse token overrides model for runtime developer customizations.
 * Only modified values are stored.
 */
@Serializable
data class TokenOverrides(
    val colors: Map<String, Long> = emptyMap(),
    val radii: Map<String, Float> = emptyMap(),
    val extra: Map<String, String> = emptyMap()
) {
    fun isEmpty(): Boolean = colors.isEmpty() && radii.isEmpty() && extra.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    companion object {
        val ALLOWED_COLOR_ROLES = setOf(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground", "surface", "onSurface",
            "surfaceVariant", "onSurfaceVariant",
            "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
            "surfaceContainerHigh", "surfaceContainerHighest",
            "outline", "outlineVariant", "scrim",
            "inverseSurface", "inverseOnSurface", "inversePrimary",
            "surfaceTint", "spectrumGlow", "signalActive", "signalIdle", "signalError",
            "signalLow", "signalMid", "signalHigh", "signalPeak",
            "noiseFloorLine", "syncMarker", "toneGuide",
            "glowPrimary", "glowSecondary", "success", "onSuccess",
            "warning", "onWarning", "scanline"
        )

        val ALLOWED_RADIUS_NAMES = setOf(
            "extraSmall", "small", "medium", "large", "extraLarge"
        )

        const val MIN_RADIUS_DP = 0f
        const val MAX_RADIUS_DP = 100f

        /**
         * Validates a TokenOverrides instance, returning failure if unknown keys or out-of-range values exist.
         */
        fun validate(overrides: TokenOverrides): Result<TokenOverrides> {
            for (role in overrides.colors.keys) {
                if (role !in ALLOWED_COLOR_ROLES) {
                    return Result.failure(IllegalArgumentException("Unknown color role: $role"))
                }
            }
            for ((name, radius) in overrides.radii) {
                if (name !in ALLOWED_RADIUS_NAMES) {
                    return Result.failure(IllegalArgumentException("Unknown radius name: $name"))
                }
                if (radius < MIN_RADIUS_DP || radius > MAX_RADIUS_DP) {
                    return Result.failure(IllegalArgumentException("Radius $name value $radius out of range [$MIN_RADIUS_DP, $MAX_RADIUS_DP]"))
                }
            }
            return Result.success(overrides)
        }
    }
}
