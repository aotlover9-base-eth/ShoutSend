package app.shoutsend.core.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec

/**
 * Motion tokens for animations, durations, easing curves, and spring physics.
 */
data class MotionTokens(
    val durationShort1: Int,
    val durationShort2: Int,
    val durationMedium1: Int,
    val durationMedium2: Int,
    val durationLong1: Int,
    val durationLong2: Int,
    val durationExtraLong1: Int,
    val durationExtraLong2: Int,
    val easingStandard: Easing,
    val easingStandardDecelerate: Easing,
    val easingStandardAccelerate: Easing,
    val springPress: SpringSpec<Float>,
    val springEnter: SpringSpec<Float>,
    val springExit: SpringSpec<Float>
)
