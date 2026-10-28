package app.shoutsend.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Design System token resolution and invariants in Material 3 Expressive.
 */
class DesignSystemTest {

    @Test
    fun defaultSeedColorIsMaterial3Purple() {
        assertEquals(0xFF6750A4L, DefaultTokens.SEED_COLOR_LONG)
    }

    @Test
    fun componentShapeTokensReferenceExpressiveRadiusTokens() {
        val shapes = DefaultTokens.Shapes
        assertEquals("button must reference full pill shape", shapes.full, shapes.button)
        assertEquals("card must reference large shape", shapes.large, shapes.card)
        assertEquals("dialog must reference extraLarge shape", shapes.extraLarge, shapes.dialog)
        assertEquals("textField must reference large shape", shapes.large, shapes.textField)
        assertEquals("chip must reference medium shape", shapes.medium, shapes.chip)
        assertEquals("fab must reference large shape", shapes.large, shapes.fab)
    }

    @Test
    fun everyColorRoleHasSpecifiedValue() {
        listOf(DesignTokens.defaultDark().colors, DesignTokens.defaultLight().colors).forEach { palette ->
            assertTrue(palette.primary.isSpecified)
            assertTrue(palette.onPrimary.isSpecified)
            assertTrue(palette.primaryContainer.isSpecified)
            assertTrue(palette.onPrimaryContainer.isSpecified)
            assertTrue(palette.secondary.isSpecified)
            assertTrue(palette.onSecondary.isSpecified)
            assertTrue(palette.secondaryContainer.isSpecified)
            assertTrue(palette.onSecondaryContainer.isSpecified)
            assertTrue(palette.tertiary.isSpecified)
            assertTrue(palette.onTertiary.isSpecified)
            assertTrue(palette.tertiaryContainer.isSpecified)
            assertTrue(palette.onTertiaryContainer.isSpecified)
            assertTrue(palette.error.isSpecified)
            assertTrue(palette.onError.isSpecified)
            assertTrue(palette.errorContainer.isSpecified)
            assertTrue(palette.onErrorContainer.isSpecified)
            assertTrue(palette.background.isSpecified)
            assertTrue(palette.onBackground.isSpecified)
            assertTrue(palette.surface.isSpecified)
            assertTrue(palette.onSurface.isSpecified)
            assertTrue(palette.surfaceVariant.isSpecified)
            assertTrue(palette.onSurfaceVariant.isSpecified)
            assertTrue(palette.outline.isSpecified)
            assertTrue(palette.outlineVariant.isSpecified)
            assertTrue(palette.scrim.isSpecified)
            // Acoustic slots
            assertTrue(palette.signalActive.isSpecified)
            assertTrue(palette.signalIdle.isSpecified)
            assertTrue(palette.signalError.isSpecified)
            // Extended semantic slots
            assertTrue(palette.success.isSpecified)
            assertTrue(palette.onSuccess.isSpecified)
            assertTrue(palette.successContainer.isSpecified)
            assertTrue(palette.onSuccessContainer.isSpecified)
            assertTrue(palette.warning.isSpecified)
            assertTrue(palette.onWarning.isSpecified)
            assertTrue(palette.warningContainer.isSpecified)
            assertTrue(palette.onWarningContainer.isSpecified)
        }
    }

    @Test
    fun defaultPalettesMeetWcagContrastAA() {
        fun contrastRatio(c1: Color, c2: Color): Float {
            val l1 = c1.luminance()
            val l2 = c2.luminance()
            val lighter = maxOf(l1, l2)
            val darker = minOf(l1, l2)
            return (lighter + 0.05f) / (darker + 0.05f)
        }

        val dark = DesignTokens.defaultDark().colors
        assertTrue("Dark onPrimary contrast >= 4.5", contrastRatio(dark.primary, dark.onPrimary) >= 4.5f)
        assertTrue("Dark onSecondary contrast >= 4.5", contrastRatio(dark.secondary, dark.onSecondary) >= 4.5f)
        assertTrue("Dark onTertiary contrast >= 4.5", contrastRatio(dark.tertiary, dark.onTertiary) >= 4.5f)
        assertTrue("Dark onError contrast >= 4.5", contrastRatio(dark.error, dark.onError) >= 4.5f)
        assertTrue("Dark onBackground contrast >= 4.5", contrastRatio(dark.background, dark.onBackground) >= 4.5f)
        assertTrue("Dark onSurface contrast >= 4.5", contrastRatio(dark.surface, dark.onSurface) >= 4.5f)

        val light = DesignTokens.defaultLight().colors
        assertTrue("Light onPrimary contrast >= 4.5", contrastRatio(light.primary, light.onPrimary) >= 4.5f)
        assertTrue("Light onSecondary contrast >= 4.5", contrastRatio(light.secondary, light.onSecondary) >= 4.5f)
        assertTrue("Light onBackground contrast >= 4.5", contrastRatio(light.background, light.onBackground) >= 4.5f)
        assertTrue("Light onSurface contrast >= 4.5", contrastRatio(light.surface, light.onSurface) >= 4.5f)
    }

    @Test
    fun tokenSetDefaultsIntegrity() {
        val darkTokens = DesignTokens.defaultDark()
        assertEquals(ThemeMode.DARK, darkTokens.themeMode)
        assertFalse(darkTokens.dynamicColor)
        assertEquals(0xFF6750A4L, darkTokens.seedColor)
        assertEquals("Cookie9Sided", darkTokens.heroShape)

        val lightTokens = DesignTokens.defaultLight()
        assertEquals(ThemeMode.LIGHT, lightTokens.themeMode)
        assertFalse(lightTokens.dynamicColor)
        assertEquals(0xFF6750A4L, lightTokens.seedColor)
        assertEquals("Cookie9Sided", lightTokens.heroShape)
    }

    @Test
    fun spacingAndSizesHierarchy() {
        val spacing = DefaultTokens.Spacing
        assertTrue(spacing.xxs < spacing.xs)
        assertTrue(spacing.xs < spacing.sm)
        assertTrue(spacing.sm < spacing.md)
        assertTrue(spacing.md < spacing.lg)
        assertTrue(spacing.lg < spacing.xl)
        assertTrue(spacing.xl < spacing.xxl)

        val sizes = DefaultTokens.Sizes
        assertTrue(sizes.iconXs < sizes.iconSm)
        assertTrue(sizes.iconSm < sizes.iconMd)
        assertTrue(sizes.iconMd < sizes.iconLg)
        assertTrue(sizes.iconLg < sizes.iconXl)
        assertTrue(sizes.touchTargetMin >= 48.dp)
    }
}
