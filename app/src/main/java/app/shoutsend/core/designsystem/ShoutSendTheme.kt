package app.shoutsend.core.designsystem

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

val LocalDesignTokens = staticCompositionLocalOf {
    DesignTokens.default()
}

val LocalReduceMotion = staticCompositionLocalOf {
    false
}

val LocalHeroShapeName = staticCompositionLocalOf {
    "Cookie9Sided"
}

/**
 * Main application theme wrapper converting design tokens to Material 3 Expressive and exposing
 * token accessors through LocalDesignTokens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShoutSendTheme(
    tokens: DesignTokens = DesignTokens.default(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (tokens.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val isReduceMotion = try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
    }

    // Dynamic color (Android 12+) or MaterialKolor scheme from seed
    val m3Scheme: ColorScheme = if (tokens.dynamicColor) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        dynamicColorScheme(
            seedColor = Color(tokens.seedColor),
            isDark = isDark,
            isAmoled = false,
            style = PaletteStyle.TonalSpot,
            contrastLevel = tokens.contrast.toDouble()
        )
    }

    val extendedColors = ExtendedColors.build(m3Scheme, isDark)
    val activeColors = ColorTokens(m3Scheme, extendedColors)
    val activeTokens = tokens.copy(
        colors = activeColors,
        extendedColors = extendedColors
    )

    CompositionLocalProvider(
        LocalDesignTokens provides activeTokens,
        LocalReduceMotion provides isReduceMotion,
        LocalHeroShapeName provides tokens.heroShape
    ) {
        MaterialExpressiveTheme(
            colorScheme = m3Scheme,
            motionScheme = MotionScheme.Companion.expressive(),
            shapes = activeTokens.shapes.toMaterialShapes(),
            typography = activeTokens.typography.toMaterialTypography(),
            content = content
        )
    }
}

/**
 * Convenience accessor for design tokens throughout Compose UI code.
 */
object ShoutSendTheme {
    val tokens: DesignTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalDesignTokens.current

    val isReducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReduceMotion.current

    val heroShapeName: String
        @Composable
        @ReadOnlyComposable
        get() = LocalHeroShapeName.current

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val colors: ColorTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.colors

    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = tokens.extendedColors

    val shapes: ShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.shapes

    val typography: TypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.typography

    val spacing: SpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.spacing

    val strokes: StrokeTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.strokes

    val sizes: SizeTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.sizes

    val elevation: ElevationTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.elevation

    val opacity: OpacityTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.opacity

    val motion: MotionTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.motion

    val acousticVisuals: AcousticVisualTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.acousticVisuals

    val radioVisuals: AcousticVisualTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.acousticVisuals
}
