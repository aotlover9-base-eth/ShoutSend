package app.shoutsend.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import app.shoutsend.core.data.settings.SettingsRepository

/**
 * Interface governing tactile haptic pulses and sensations across the application.
 */
interface TactileFeedbackController {
    /** Short click sensation for button taps and primary user actions. */
    fun click()

    /** Rhythmic tick sensation on frame transmission/reception or repeat passes. */
    fun tick()

    /** Rewarding double burst sensation on successful transfer completion. */
    fun success()

    /** Strong distinctive pulse sensation on decode or transmission error. */
    fun error()
}

/**
 * No-op implementation of [TactileFeedbackController] for headless environments and previews.
 */
object NoOpTactileFeedbackController : TactileFeedbackController {
    override fun click() {}
    override fun tick() {}
    override fun success() {}
    override fun error() {}
}

/**
 * CompositionLocal providing access to the [TactileFeedbackController] within the Compose tree.
 */
val LocalTactileFeedback = staticCompositionLocalOf<TactileFeedbackController> {
    NoOpTactileFeedbackController
}

/**
 * Production Android implementation of [TactileFeedbackController] using [VibratorManager] / [Vibrator].
 */
class DefaultTactileFeedbackController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val vibrator: Vibrator? = resolveVibrator(context)
) : TactileFeedbackController {

    private fun canVibrate(): Boolean {
        if (!settingsRepository.settings.value.hapticsEnabled) {
            return false
        }
        val vib = vibrator ?: return false
        return try {
            vib.hasVibrator()
        } catch (_: Exception) {
            false
        }
    }

    private fun performVibration(effect: VibrationEffect) {
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                vib.vibrate(effect, attributes)
            } else {
                vib.vibrate(effect)
            }
        } catch (_: Exception) {
            // Non-fatal if device hardware denies vibration
        }
    }

    override fun click() {
        if (!canVibrate()) return
        try {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            performVibration(effect)
        } catch (_: Exception) {
            try {
                val fallback = VibrationEffect.createOneShot(15L, 120)
                performVibration(fallback)
            } catch (_: Exception) {}
        }
    }

    override fun tick() {
        if (!canVibrate()) return
        try {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            performVibration(effect)
        } catch (_: Exception) {
            try {
                val fallback = VibrationEffect.createOneShot(8L, 70)
                performVibration(fallback)
            } catch (_: Exception) {}
        }
    }

    override fun success() {
        if (!canVibrate()) return
        try {
            val timings = longArrayOf(0, 35, 50, 50)
            val amplitudes = intArrayOf(0, 160, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            performVibration(effect)
        } catch (_: Exception) {
            try {
                val fallback = VibrationEffect.createOneShot(80L, 200)
                performVibration(fallback)
            } catch (_: Exception) {}
        }
    }

    override fun error() {
        if (!canVibrate()) return
        try {
            val timings = longArrayOf(0, 70, 60, 70)
            val amplitudes = intArrayOf(0, 220, 0, 220)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            performVibration(effect)
        } catch (_: Exception) {
            try {
                val fallback = VibrationEffect.createOneShot(140L, 220)
                performVibration(fallback)
            } catch (_: Exception) {}
        }
    }

    companion object {
        fun resolveVibrator(context: Context): Vibrator? {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
