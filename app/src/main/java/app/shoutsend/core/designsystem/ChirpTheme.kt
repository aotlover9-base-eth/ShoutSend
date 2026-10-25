package app.shoutsend.core.designsystem

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalDesignTokens = staticCompositionLocalOf {
    DesignTokens.defaultDark()
}

val LocalReduceMotion = staticCompositionLocalOf {
    false
}

/**
 * Main application theme wrapper converting design tokens to Material 3 and exposing
 * token accessors through LocalDesignTokens.
 */
@Composable
fun ChirpTheme(
    tokens: DesignTokens = DesignTokens.defaultDark(),
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

    val effectiveColors = if (tokens.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicM3: ColorScheme = if (isDark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        tokens.colors.copy(
            primary = dynamicM3.primary,
            onPrimary = dynamicM3.onPrimary,
            primaryContainer = dynamicM3.primaryContainer,
            onPrimaryContainer = dynamicM3.onPrimaryContainer,
            secondary = dynamicM3.secondary,
            onSecondary = dynamicM3.onSecondary,
            secondaryContainer = dynamicM3.secondaryContainer,
            onSecondaryContainer = dynamicM3.onSecondaryContainer,
            tertiary = dynamicM3.tertiary,
            onTertiary = dynamicM3.onTertiary,
            tertiaryContainer = dynamicM3.tertiaryContainer,
            onTertiaryContainer = dynamicM3.onTertiaryContainer,
            background = dynamicM3.background,
            onBackground = dynamicM3.onBackground,
            surface = dynamicM3.surface,
            onSurface = dynamicM3.onSurface,
            surfaceVariant = dynamicM3.surfaceVariant,
            onSurfaceVariant = dynamicM3.onSurfaceVariant,
            outline = dynamicM3.outline,
            outlineVariant = dynamicM3.outlineVariant,
            glowPrimary = dynamicM3.primary,
            glowSecondary = dynamicM3.secondary,
            success = dynamicM3.secondary,
            onSuccess = dynamicM3.onSecondary,
            warning = dynamicM3.tertiary,
            onWarning = dynamicM3.onTertiary,
            signalHigh = dynamicM3.primary,
            signalPeak = dynamicM3.secondary,
            signalLow = dynamicM3.surfaceVariant,
            signalMid = dynamicM3.primaryContainer,
            noiseFloorLine = dynamicM3.tertiary,
            syncMarker = dynamicM3.tertiary,
            toneGuide = dynamicM3.outlineVariant
        )
    } else {
        if (isDark) DefaultTokens.CyberRadioDark else DefaultTokens.CyberRadioLight
    }

    val activeTokens = tokens.copy(colors = effectiveColors)

    CompositionLocalProvider(
        LocalDesignTokens provides activeTokens,
        LocalReduceMotion provides isReduceMotion
    ) {
        MaterialTheme(
            colorScheme = effectiveColors.toMaterialColorScheme(isDark),
            shapes = activeTokens.shapes.toMaterialShapes(),
            typography = activeTokens.typography.toMaterialTypography(),
            content = content
        )
    }
}

/**
 * Convenience accessor for design tokens throughout Compose UI code.
 * Example: ChirpTheme.spacing.md, ChirpTheme.colors.primary
 */
object ChirpTheme {
    val tokens: DesignTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalDesignTokens.current

    val isReducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReduceMotion.current

    val colors: ColorTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.colors

    val shapes: ShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.shapes

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

    val typography: TypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.typography

    val radioVisuals: RadioVisualTokens
        @Composable
        @ReadOnlyComposable
        get() = tokens.radioVisuals
}
