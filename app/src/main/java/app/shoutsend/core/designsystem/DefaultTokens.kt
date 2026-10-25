package app.shoutsend.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SINGLE EXCEPTION FILE:
 * Per Rule 6 and Rule 7, this is the only file in app/src where raw hex colors,
 * numeric dp, sp, and millisecond literals may exist.
 */
object DefaultTokens {

    // --- CYBER RADIO DARK PALETTE (Default) ---
    val CyberRadioDark = ColorTokens(
        primary = Color(0xFF00F0FF),
        onPrimary = Color(0xFF00363D),
        primaryContainer = Color(0xFF004F58),
        onPrimaryContainer = Color(0xFF97F0FF),
        secondary = Color(0xFF00FF66),
        onSecondary = Color(0xFF003912),
        secondaryContainer = Color(0xFF00531D),
        onSecondaryContainer = Color(0xFF73FF8B),
        tertiary = Color(0xFFFFB300),
        onTertiary = Color(0xFF412D00),
        tertiaryContainer = Color(0xFF5D4200),
        onTertiaryContainer = Color(0xFFFFDF9E),
        error = Color(0xFFFF5449),
        onError = Color(0xFF410002),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0B0E14),
        onBackground = Color(0xFFE1E7F0),
        surface = Color(0xFF0B0E14),
        onSurface = Color(0xFFE1E7F0),
        surfaceVariant = Color(0xFF171C26),
        onSurfaceVariant = Color(0xFFA5B0C2),
        surfaceContainerLowest = Color(0xFF06080C),
        surfaceContainerLow = Color(0xFF10141D),
        surfaceContainer = Color(0xFF171C26),
        surfaceContainerHigh = Color(0xFF1E2430),
        surfaceContainerHighest = Color(0xFF252C3A),
        outline = Color(0xFF525E75),
        outlineVariant = Color(0xFF343D4F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE1E7F0),
        inverseOnSurface = Color(0xFF1E2430),
        inversePrimary = Color(0xFF006874),
        surfaceTint = Color(0xFF00F0FF),
        spectrumGlow = Color(0xFF00F0FF),
        signalActive = Color(0xFF00FF66),
        signalIdle = Color(0xFF525E75),
        signalError = Color(0xFFFF5449),
        signalLow = Color(0xFF0D1B2A),
        signalMid = Color(0xFF0077B6),
        signalHigh = Color(0xFF00F0FF),
        signalPeak = Color(0xFFFFFFFF),
        noiseFloorLine = Color(0xFFFFB300),
        syncMarker = Color(0xFFFF007F),
        toneGuide = Color(0xFF1E2D3D),
        glowPrimary = Color(0xFF00F0FF),
        glowSecondary = Color(0xFF00FF66),
        success = Color(0xFF00FF66),
        onSuccess = Color(0xFF003912),
        warning = Color(0xFFFFB300),
        onWarning = Color(0xFF412D00),
        scanline = Color(0x0A00F0FF)
    )

    // --- CYBER RADIO LIGHT PALETTE ---
    val CyberRadioLight = ColorTokens(
        primary = Color(0xFF006874),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF97F0FF),
        onPrimaryContainer = Color(0xFF001F24),
        secondary = Color(0xFF006E26),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF73FF8B),
        onSecondaryContainer = Color(0xFF002207),
        tertiary = Color(0xFF7A5900),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDF9E),
        onTertiaryContainer = Color(0xFF261A00),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF7F9FE),
        onBackground = Color(0xFF171C26),
        surface = Color(0xFFF7F9FE),
        onSurface = Color(0xFF171C26),
        surfaceVariant = Color(0xFFDCE2EE),
        onSurfaceVariant = Color(0xFF404753),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF1F4FA),
        surfaceContainer = Color(0xFFEAEFF6),
        surfaceContainerHigh = Color(0xFFE4E9F0),
        surfaceContainerHighest = Color(0xFFDEE3EA),
        outline = Color(0xFF707885),
        outlineVariant = Color(0xFFBFC7D5),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2C313C),
        inverseOnSurface = Color(0xFFEFF0F8),
        inversePrimary = Color(0xFF00F0FF),
        surfaceTint = Color(0xFF006874),
        spectrumGlow = Color(0xFF006874),
        signalActive = Color(0xFF006E26),
        signalIdle = Color(0xFF707885),
        signalError = Color(0xFFBA1A1A),
        signalLow = Color(0xFFE0E8F0),
        signalMid = Color(0xFF0288D1),
        signalHigh = Color(0xFF006874),
        signalPeak = Color(0xFF004D40),
        noiseFloorLine = Color(0xFFE65100),
        syncMarker = Color(0xFFC2185B),
        toneGuide = Color(0xFFCFD8DC),
        glowPrimary = Color(0xFF006874),
        glowSecondary = Color(0xFF006E26),
        success = Color(0xFF006E26),
        onSuccess = Color(0xFFFFFFFF),
        warning = Color(0xFF7A5900),
        onWarning = Color(0xFFFFFFFF),
        scanline = Color(0x05000000)
    )

    // --- BASE SHAPES ---
    val ShapeExtraSmall = RoundedCornerShape(4.dp)
    val ShapeSmall = RoundedCornerShape(8.dp)
    val ShapeMedium = RoundedCornerShape(12.dp)
    val ShapeLarge = RoundedCornerShape(16.dp)
    val ShapeExtraLarge = RoundedCornerShape(28.dp)

    val Shapes = ShapeTokens(
        extraSmall = ShapeExtraSmall,
        small = ShapeSmall,
        medium = ShapeMedium,
        large = ShapeLarge,
        extraLarge = ShapeExtraLarge,
        button = ShapeSmall,
        card = ShapeMedium,
        dialog = ShapeExtraLarge,
        bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        textField = ShapeSmall,
        chip = ShapeSmall,
        fab = ShapeLarge
    )

    // --- SPACING TOKENS ---
    val Spacing = SpacingTokens(
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = 16.dp,
        lg = 24.dp,
        xl = 32.dp,
        xxl = 48.dp
    )

    // --- STROKE TOKENS ---
    val Strokes = StrokeTokens(
        hairline = 0.5.dp,
        thin = 1.dp,
        regular = 1.5.dp,
        thick = 2.dp
    )

    // --- SIZE TOKENS ---
    val Sizes = SizeTokens(
        iconXs = 16.dp,
        iconSm = 20.dp,
        iconMd = 24.dp,
        iconLg = 32.dp,
        iconXl = 48.dp,
        touchTargetMin = 48.dp,
        buttonHeight = 48.dp,
        textFieldHeight = 56.dp,
        topBarHeight = 64.dp,
        navBarHeight = 80.dp
    )

    // --- ELEVATION TOKENS ---
    val Elevations = ElevationTokens(
        level0 = 0.dp,
        level1 = 1.dp,
        level2 = 3.dp,
        level3 = 6.dp,
        level4 = 8.dp,
        level5 = 12.dp
    )

    // --- OPACITY TOKENS ---
    val Opacities = OpacityTokens(
        disabled = 0.38f,
        scrim = 0.60f,
        glow = 0.75f,
        muted = 0.60f,
        subtle = 0.12f
    )

    // --- MOTION TOKENS ---
    val Motions = MotionTokens(
        durationShort1 = 50,
        durationShort2 = 100,
        durationMedium1 = 200,
        durationMedium2 = 300,
        durationLong1 = 400,
        durationLong2 = 500,
        durationExtraLong1 = 700,
        durationExtraLong2 = 1000,
        easingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
        easingStandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f),
        easingStandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f),
        springPress = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        springEnter = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        springExit = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    )

    // --- RADIO VISUAL TOKENS ---
    val RadioVisuals = RadioVisualTokens(
        glowRadius = 12.dp,
        glowAlpha = 0.35f,
        ringStrokeWidths = 1.5.dp,
        ringCount = 3,
        ringSpawnIntervalMs = 800,
        waterfallRowsPerSecond = 30,
        waterfallHistorySeconds = 4,
        colormapResolution = 256,
        meterSegmentCount = 12
    )

    // --- TYPOGRAPHY TOKENS ---
    val Typography = TypographyTokens(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp
        ),
        radioReadoutLarge = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        radioReadoutMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        radioReadoutSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )
}
