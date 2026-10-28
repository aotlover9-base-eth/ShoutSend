package app.shoutsend.core.designsystem

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

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
    val extendedColors: ExtendedColors,
    val shapes: ShapeTokens = DefaultTokens.Shapes,
    val typography: TypographyTokens = DefaultTokens.Typography,
    val spacing: SpacingTokens = DefaultTokens.Spacing,
    val strokes: StrokeTokens = DefaultTokens.Strokes,
    val sizes: SizeTokens = DefaultTokens.Sizes,
    val elevation: ElevationTokens = DefaultTokens.Elevations,
    val opacity: OpacityTokens = DefaultTokens.Opacities,
    val motion: MotionTokens = DefaultTokens.Motions,
    val acousticVisuals: AcousticVisualTokens = DefaultTokens.AcousticVisuals,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val seedColor: Long = DefaultTokens.SEED_COLOR_LONG,
    val contrast: Float = 0f,
    val heroShape: String = "Cookie9Sided"
) {
    val radioVisuals: AcousticVisualTokens get() = acousticVisuals

    companion object {
        fun default(): DesignTokens {
            val scheme = dynamicColorScheme(
                seedColor = DefaultTokens.SeedColor,
                isDark = true,
                isAmoled = false,
                style = PaletteStyle.TonalSpot
            )
            val ext = ExtendedColors.build(scheme, true)
            return DesignTokens(
                colors = ColorTokens(scheme, ext),
                extendedColors = ext,
                themeMode = ThemeMode.SYSTEM,
                dynamicColor = true,
                seedColor = DefaultTokens.SEED_COLOR_LONG,
                contrast = 0f,
                heroShape = "Cookie9Sided"
            )
        }

        fun defaultDark(): DesignTokens {
            val scheme = dynamicColorScheme(
                seedColor = DefaultTokens.SeedColor,
                isDark = true,
                isAmoled = false,
                style = PaletteStyle.TonalSpot
            )
            val ext = ExtendedColors.build(scheme, true)
            return DesignTokens(
                colors = ColorTokens(scheme, ext),
                extendedColors = ext,
                themeMode = ThemeMode.DARK,
                dynamicColor = false,
                seedColor = DefaultTokens.SEED_COLOR_LONG,
                contrast = 0f,
                heroShape = "Cookie9Sided"
            )
        }

        fun defaultLight(): DesignTokens {
            val scheme = dynamicColorScheme(
                seedColor = DefaultTokens.SeedColor,
                isDark = false,
                isAmoled = false,
                style = PaletteStyle.TonalSpot
            )
            val ext = ExtendedColors.build(scheme, false)
            return DesignTokens(
                colors = ColorTokens(scheme, ext),
                extendedColors = ext,
                themeMode = ThemeMode.LIGHT,
                dynamicColor = false,
                seedColor = DefaultTokens.SEED_COLOR_LONG,
                contrast = 0f,
                heroShape = "Cookie9Sided"
            )
        }
    }
}
