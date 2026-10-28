package app.shoutsend.core.designsystem

import kotlinx.serialization.Serializable

/**
 * Sparse token overrides model for runtime developer customizations.
 * Stores dynamic color setting, theme mode, seed color, contrast, corner radii, and hero shape.
 */
@Serializable
data class TokenOverrides(
    val dynamicColor: Boolean = true,
    val themeMode: String = "SYSTEM",
    val seedColor: Long = DefaultTokens.SEED_COLOR_LONG,
    val contrast: Float = 0f,
    val radii: Map<String, Float> = emptyMap(),
    val heroShape: String = "Cookie9Sided",
    // Optional properties with defaults to maintain backward-compatibility with previously saved v1 JSON
    val colors: Map<String, Long> = emptyMap(),
    val extra: Map<String, String> = emptyMap()
) {
    fun isEmpty(): Boolean =
        dynamicColor &&
        themeMode.equals("SYSTEM", ignoreCase = true) &&
        seedColor == DefaultTokens.SEED_COLOR_LONG &&
        contrast == 0f &&
        radii.isEmpty() &&
        heroShape == "Cookie9Sided"

    fun isNotEmpty(): Boolean = !isEmpty()

    companion object {
        val ALLOWED_RADIUS_NAMES = setOf(
            "extraSmall", "small", "medium", "large", "largeIncreased", "extraLarge", "extraLargeIncreased"
        )
        const val MIN_RADIUS_DP = 0f
        const val MAX_RADIUS_DP = 100f

        val ALLOWED_HERO_SHAPES = setOf(
            "Cookie9Sided", "Sunny", "Clover4Leaf", "Pill", "SoftBurst", "Heart", "Flower"
        )

        /**
         * Validates a TokenOverrides instance, returning failure if unknown keys or out-of-range values exist.
         */
        fun validate(overrides: TokenOverrides): Result<TokenOverrides> {
            val mode = overrides.themeMode.uppercase()
            if (mode !in setOf("SYSTEM", "LIGHT", "DARK")) {
                return Result.failure(IllegalArgumentException("Invalid themeMode: ${overrides.themeMode}"))
            }
            if (overrides.contrast < -1f || overrides.contrast > 1f) {
                return Result.failure(IllegalArgumentException("Contrast ${overrides.contrast} out of range [-1.0, 1.0]"))
            }
            for ((name, radius) in overrides.radii) {
                if (name !in ALLOWED_RADIUS_NAMES) {
                    return Result.failure(IllegalArgumentException("Unknown radius token name: $name"))
                }
                if (radius < MIN_RADIUS_DP || radius > MAX_RADIUS_DP) {
                    return Result.failure(IllegalArgumentException("Radius $name value $radius out of range [$MIN_RADIUS_DP, $MAX_RADIUS_DP]"))
                }
            }
            if (overrides.heroShape !in ALLOWED_HERO_SHAPES) {
                return Result.failure(IllegalArgumentException("Unknown hero shape: ${overrides.heroShape}"))
            }
            return Result.success(overrides)
        }
    }
}
