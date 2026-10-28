package app.shoutsend.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
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

    // --- SEED COLOR & SEMANTIC FIXED HUES ---
    const val SEED_COLOR_LONG = 0xFF6750A4L
    val SeedColor = Color(SEED_COLOR_LONG)

    val SeedPresets: List<Pair<String, Long>> = listOf(
        "Violet" to SEED_COLOR_LONG,
        "Coral" to 0xFFBA1A1AL,
        "Forest" to 0xFF386A20L,
        "Ocean" to 0xFF006874L,
        "Amber" to 0xFF825500L
    )

    // Fixed semantic seeds for harmonization with primary
    val SuccessSeed = Color(0xFF388E3C)
    val WarningSeed = Color(0xFFF57C00)

    // Semantic container and on-container roles (fallback and dark/light pairings)
    val SuccessDarkOn = Color(0xFF003912)
    val SuccessDarkContainer = Color(0xFF00531D)
    val SuccessDarkOnContainer = Color(0xFF73FF8B)

    val SuccessLightOn = Color(0xFFFFFFFF)
    val SuccessLightContainer = Color(0xFF73FF8B)
    val SuccessLightOnContainer = Color(0xFF002207)

    val WarningDarkOn = Color(0xFF412D00)
    val WarningDarkContainer = Color(0xFF5D4200)
    val WarningDarkOnContainer = Color(0xFFFFDF9E)

    val WarningLightOn = Color(0xFFFFFFFF)
    val WarningLightContainer = Color(0xFFFFDF9E)
    val WarningLightOnContainer = Color(0xFF261A00)

    // --- BASE SHAPES (M3 Expressive scale) ---
    val ShapeExtraSmall = RoundedCornerShape(4.dp)
    val ShapeSmall = RoundedCornerShape(8.dp)
    val ShapeMedium = RoundedCornerShape(16.dp)
    val ShapeLarge = RoundedCornerShape(24.dp)
    val ShapeLargeIncreased = RoundedCornerShape(28.dp)
    val ShapeExtraLarge = RoundedCornerShape(32.dp)
    val ShapeExtraLargeIncreased = RoundedCornerShape(40.dp)
    val ShapeFull = CircleShape

    val Shapes = ShapeTokens(
        extraSmall = ShapeExtraSmall,
        small = ShapeSmall,
        medium = ShapeMedium,
        large = ShapeLarge,
        largeIncreased = ShapeLargeIncreased,
        extraLarge = ShapeExtraLarge,
        extraLargeIncreased = ShapeExtraLargeIncreased,
        full = ShapeFull,
        button = ShapeFull,
        card = ShapeLarge,
        dialog = ShapeExtraLarge,
        bottomSheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        textField = ShapeLarge,
        chip = ShapeMedium,
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

    // --- ACOUSTIC VISUAL TOKENS ---
    val AcousticVisuals = AcousticVisualTokens(
        ringStrokeWidths = 1.5.dp,
        ringCount = 3,
        ringSpawnIntervalMs = 800,
        waterfallRowsPerSecond = 30,
        waterfallHistorySeconds = 4,
        colormapResolution = 256,
        meterSegmentCount = 12
    )

    val RadioVisuals = AcousticVisuals

    // --- TYPOGRAPHY TOKENS (Device Default Family, No Monospace) ---
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
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
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
        )
    )
}
