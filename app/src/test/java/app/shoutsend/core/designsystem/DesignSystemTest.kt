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
 * Unit tests for Design System token resolution and invariants.
 */
class DesignSystemTest {

    @Test
    fun componentShapeTokensReferenceRadiusTokens() {
        val shapes = DefaultTokens.Shapes
        assertEquals(shapes.small, shapes.button)
        assertEquals(shapes.medium, shapes.card)
        assertEquals(shapes.extraLarge, shapes.dialog)
        assertEquals(shapes.small, shapes.textField)
        assertEquals(shapes.small, shapes.chip)
        assertEquals(shapes.large, shapes.fab)
    }

    @Test
    fun everyColorRoleHasSpecifiedValue() {
        listOf(DefaultTokens.CyberRadioDark, DefaultTokens.CyberRadioLight).forEach { palette ->
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
            // Extended slots
            assertTrue(palette.spectrumGlow.isSpecified)
            assertTrue(palette.signalActive.isSpecified)
            assertTrue(palette.signalIdle.isSpecified)
            assertTrue(palette.signalError.isSpecified)
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

        val dark = DefaultTokens.CyberRadioDark
        assertTrue("Dark onPrimary contrast >= 4.5", contrastRatio(dark.primary, dark.onPrimary) >= 4.5f)
        assertTrue("Dark onSecondary contrast >= 4.5", contrastRatio(dark.secondary, dark.onSecondary) >= 4.5f)
        assertTrue("Dark onTertiary contrast >= 4.5", contrastRatio(dark.tertiary, dark.onTertiary) >= 4.5f)
        assertTrue("Dark onError contrast >= 4.5", contrastRatio(dark.error, dark.onError) >= 4.5f)
        assertTrue("Dark onBackground contrast >= 4.5", contrastRatio(dark.background, dark.onBackground) >= 4.5f)
        assertTrue("Dark onSurface contrast >= 4.5", contrastRatio(dark.surface, dark.onSurface) >= 4.5f)

        val light = DefaultTokens.CyberRadioLight
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
        assertEquals(DefaultTokens.CyberRadioDark, darkTokens.colors)

        val lightTokens = DesignTokens.defaultLight()
        assertEquals(ThemeMode.LIGHT, lightTokens.themeMode)
        assertFalse(lightTokens.dynamicColor)
        assertEquals(DefaultTokens.CyberRadioLight, lightTokens.colors)
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
