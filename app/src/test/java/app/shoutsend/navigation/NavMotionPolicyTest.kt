package app.shoutsend.navigation

import app.shoutsend.core.designsystem.DefaultTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying navigation motion policies, reduced-motion bypass logic,
 * and design system motion tokens (Step 48).
 */
class NavMotionPolicyTest {

    @Test
    fun shouldBypassAnimationsWhenReducedMotionIsTrue() {
        assertTrue(NavMotionPolicy.shouldBypassAnimations(isReducedMotion = true))
    }

    @Test
    fun shouldNotBypassAnimationsWhenReducedMotionIsFalse() {
        assertFalse(NavMotionPolicy.shouldBypassAnimations(isReducedMotion = false))
    }

    @Test
    fun resolveTransitionDurationBypassesToZeroWhenReducedMotionActive() {
        val resolved = NavMotionPolicy.resolveTransitionDuration(
            isReducedMotion = true,
            durationMs = 300
        )
        assertEquals(0, resolved)
    }

    @Test
    fun resolveTransitionDurationPreservesDurationWhenReducedMotionInactive() {
        val resolved = NavMotionPolicy.resolveTransitionDuration(
            isReducedMotion = false,
            durationMs = 300
        )
        assertEquals(300, resolved)
    }

    @Test
    fun defaultMotionTokensProvideValidDurationsAndCurves() {
        val motions = DefaultTokens.Motions
        assertTrue(motions.durationShort1 > 0)
        assertTrue(motions.durationShort2 > motions.durationShort1)
        assertTrue(motions.durationMedium1 > motions.durationShort2)
        assertTrue(motions.durationMedium2 > motions.durationMedium1)
        assertTrue(motions.durationLong1 > motions.durationMedium2)
        assertTrue(motions.durationLong2 > motions.durationLong1)

        assertNotNull(motions.easingStandard)
        assertNotNull(motions.easingStandardDecelerate)
        assertNotNull(motions.easingStandardAccelerate)
        assertNotNull(motions.springPress)
        assertNotNull(motions.springEnter)
        assertNotNull(motions.springExit)
    }
}
