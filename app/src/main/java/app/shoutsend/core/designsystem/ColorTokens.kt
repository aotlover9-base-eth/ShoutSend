package app.shoutsend.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Waterfall/spectrogram palette derived strictly from the active [ColorScheme]
 * (surfaceContainerLowest -> primaryContainer -> primary -> tertiary).
 */
fun waterfallPalette(colorScheme: ColorScheme): List<Color> = listOf(
    colorScheme.surfaceContainerLowest,
    colorScheme.primaryContainer,
    colorScheme.primary,
    colorScheme.tertiary
)

/**
 * Complete color tokens wrapping Material 3 color roles and extended semantic roles.
 */
data class ColorTokens(
    val colorScheme: ColorScheme,
    val extended: ExtendedColors
) {
    val primary: Color get() = colorScheme.primary
    val onPrimary: Color get() = colorScheme.onPrimary
    val primaryContainer: Color get() = colorScheme.primaryContainer
    val onPrimaryContainer: Color get() = colorScheme.onPrimaryContainer
    val secondary: Color get() = colorScheme.secondary
    val onSecondary: Color get() = colorScheme.onSecondary
    val secondaryContainer: Color get() = colorScheme.secondaryContainer
    val onSecondaryContainer: Color get() = colorScheme.onSecondaryContainer
    val tertiary: Color get() = colorScheme.tertiary
    val onTertiary: Color get() = colorScheme.onTertiary
    val tertiaryContainer: Color get() = colorScheme.tertiaryContainer
    val onTertiaryContainer: Color get() = colorScheme.onTertiaryContainer
    val error: Color get() = colorScheme.error
    val onError: Color get() = colorScheme.onError
    val errorContainer: Color get() = colorScheme.errorContainer
    val onErrorContainer: Color get() = colorScheme.onErrorContainer
    val background: Color get() = colorScheme.background
    val onBackground: Color get() = colorScheme.onBackground
    val surface: Color get() = colorScheme.surface
    val onSurface: Color get() = colorScheme.onSurface
    val surfaceVariant: Color get() = colorScheme.surfaceVariant
    val onSurfaceVariant: Color get() = colorScheme.onSurfaceVariant
    val surfaceContainerLowest: Color get() = colorScheme.surfaceContainerLowest
    val surfaceContainerLow: Color get() = colorScheme.surfaceContainerLow
    val surfaceContainer: Color get() = colorScheme.surfaceContainer
    val surfaceContainerHigh: Color get() = colorScheme.surfaceContainerHigh
    val surfaceContainerHighest: Color get() = colorScheme.surfaceContainerHighest
    val outline: Color get() = colorScheme.outline
    val outlineVariant: Color get() = colorScheme.outlineVariant
    val scrim: Color get() = colorScheme.scrim
    val inverseSurface: Color get() = colorScheme.inverseSurface
    val inverseOnSurface: Color get() = colorScheme.inverseOnSurface
    val inversePrimary: Color get() = colorScheme.inversePrimary
    val surfaceTint: Color get() = colorScheme.surfaceTint

    // Extended semantic roles
    val success: Color get() = extended.success
    val onSuccess: Color get() = extended.onSuccess
    val successContainer: Color get() = extended.successContainer
    val onSuccessContainer: Color get() = extended.onSuccessContainer
    val warning: Color get() = extended.warning
    val onWarning: Color get() = extended.onWarning
    val warningContainer: Color get() = extended.warningContainer
    val onWarningContainer: Color get() = extended.onWarningContainer

    // Migration accessors mapped to M3 roles (to be cleaned up in R3-R6 screens)
    val signalActive: Color get() = primary
    val signalIdle: Color get() = outline
    val signalError: Color get() = error
    val signalLow: Color get() = surfaceVariant
    val signalMid: Color get() = primaryContainer
    val signalHigh: Color get() = primary
    val signalPeak: Color get() = secondary
    val syncMarker: Color get() = tertiary
    val toneGuide: Color get() = outlineVariant
    val noiseFloorLine: Color get() = tertiary
    val glowPrimary: Color get() = primary
    val glowSecondary: Color get() = secondary
    val spectrumGlow: Color get() = primary
    val scanline: Color get() = Color.Transparent

    fun toMaterialColorScheme(): ColorScheme = colorScheme
    fun toMaterialColorScheme(isDark: Boolean): ColorScheme = colorScheme
}
