package app.shoutsend.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.harmonizeWithPrimary

/**
 * Extended semantic colors for success and warning states harmonized with the active primary.
 */
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
) {
    companion object {
        fun build(colorScheme: ColorScheme, isDark: Boolean): ExtendedColors {
            val harmonizedSuccess = colorScheme.harmonizeWithPrimary(DefaultTokens.SuccessSeed)
            val harmonizedWarning = colorScheme.harmonizeWithPrimary(DefaultTokens.WarningSeed)

            return if (isDark) {
                ExtendedColors(
                    success = harmonizedSuccess,
                    onSuccess = DefaultTokens.SuccessDarkOn,
                    successContainer = DefaultTokens.SuccessDarkContainer,
                    onSuccessContainer = DefaultTokens.SuccessDarkOnContainer,
                    warning = harmonizedWarning,
                    onWarning = DefaultTokens.WarningDarkOn,
                    warningContainer = DefaultTokens.WarningDarkContainer,
                    onWarningContainer = DefaultTokens.WarningDarkOnContainer
                )
            } else {
                ExtendedColors(
                    success = harmonizedSuccess,
                    onSuccess = DefaultTokens.SuccessLightOn,
                    successContainer = DefaultTokens.SuccessLightContainer,
                    onSuccessContainer = DefaultTokens.SuccessLightOnContainer,
                    warning = harmonizedWarning,
                    onWarning = DefaultTokens.WarningLightOn,
                    warningContainer = DefaultTokens.WarningLightContainer,
                    onWarningContainer = DefaultTokens.WarningLightOnContainer
                )
            }
        }
    }
}
