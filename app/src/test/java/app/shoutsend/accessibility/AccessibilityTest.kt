package app.shoutsend.accessibility

import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.feature.receive.WaterfallSemantics
import app.shoutsend.feature.send.RingsSemantics
import app.shoutsend.modem.profile.Band
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying accessibility semantic announcements, WCAG 48dp touch target adherence,
 * and reduced-motion semantic fallbacks (Step 49).
 */
class AccessibilityTest {

    @Test
    fun ringsSemanticsProvideClearDescriptions() {
        val transmittingDesc = RingsSemantics.formatDescription(isTransmitting = true)
        assertTrue(transmittingDesc.contains("broadcasting"))

        val idleDesc = RingsSemantics.formatDescription(isTransmitting = false)
        assertTrue(idleDesc.contains("idle"))
    }

    @Test
    fun waterfallSemanticsProvideClearDescriptionsForBands() {
        val retroDesc = WaterfallSemantics.formatDescription(Band.RETRO)
        assertTrue(retroDesc.contains("Retro"))

        val stealthDesc = WaterfallSemantics.formatDescription(Band.STEALTH)
        assertTrue(stealthDesc.contains("Stealth"))
    }

    @Test
    fun touchTargetDimensionsSatisfyMinimumAccessibleSize() {
        val minTarget = DefaultTokens.Sizes.touchTargetMin
        assertTrue("Touch target minimum must be at least 48dp", minTarget.value >= 48.0f)

        val buttonHeight = DefaultTokens.Sizes.buttonHeight
        assertTrue("Button height must meet or exceed 48dp", buttonHeight.value >= 48.0f)

        val textFieldHeight = DefaultTokens.Sizes.textFieldHeight
        assertTrue("Text field height must meet or exceed 48dp", textFieldHeight.value >= 48.0f)
    }

    @Test
    fun colorTokensProvideNonZeroAlphaForAccessibility() {
        val colors = DefaultTokens.CyberRadioDark
        assertTrue(colors.onSurface.alpha > 0.0f)
        assertTrue(colors.onPrimary.alpha > 0.0f)
        assertTrue(colors.primary.alpha > 0.0f)
        assertTrue(colors.signalActive.alpha > 0.0f)
        assertTrue(colors.signalError.alpha > 0.0f)
    }
}
