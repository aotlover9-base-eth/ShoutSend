package app.shoutsend.core.designsystem

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

/**
 * Root design tokens bundle. Single source of visual values for the entire app.
 */
data class DesignTokens(
    val colors: ColorTokens,
    val shapes: ShapeTokens = DefaultTokens.Shapes,
    val typography: TypographyTokens = DefaultTokens.Typography,
    val spacing: SpacingTokens = DefaultTokens.Spacing,
    val strokes: StrokeTokens = DefaultTokens.Strokes,
    val sizes: SizeTokens = DefaultTokens.Sizes,
    val elevation: ElevationTokens = DefaultTokens.Elevations,
    val opacity: OpacityTokens = DefaultTokens.Opacities,
    val motion: MotionTokens = DefaultTokens.Motions,
    val radioVisuals: RadioVisualTokens = DefaultTokens.RadioVisuals,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false
) {
    companion object {
        fun defaultDark(): DesignTokens = DesignTokens(
            colors = DefaultTokens.CyberRadioDark,
            themeMode = ThemeMode.DARK,
            dynamicColor = false
        )

        fun defaultLight(): DesignTokens = DesignTokens(
            colors = DefaultTokens.CyberRadioLight,
            themeMode = ThemeMode.LIGHT,
            dynamicColor = false
        )
    }
}
