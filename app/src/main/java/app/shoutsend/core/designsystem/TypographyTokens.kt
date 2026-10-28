package app.shoutsend.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/**
 * Typography tokens mapping strictly to the Material 3 type scale.
 * All styles use the device default font family. Monospace is prohibited in user UI (Rule 32b).
 */
data class TypographyTokens(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
) {
    // Migration helpers delegating to standard M3 styles (no monospace)
    val radioReadoutLarge: TextStyle get() = titleMedium
    val radioReadoutMedium: TextStyle get() = labelLarge
    val radioReadoutSmall: TextStyle get() = labelSmall

    fun toMaterialTypography(): Typography {
        return Typography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall
        )
    }
}
