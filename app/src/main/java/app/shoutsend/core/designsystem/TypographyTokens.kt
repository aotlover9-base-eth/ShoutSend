package app.shoutsend.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/**
 * Typography tokens mapping to Material 3 type scale plus radio readout monospace styles.
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
    val labelSmall: TextStyle,
    // Radio readout monospace styles for frequencies, SNR, bytes
    val radioReadoutLarge: TextStyle,
    val radioReadoutMedium: TextStyle,
    val radioReadoutSmall: TextStyle
) {
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
