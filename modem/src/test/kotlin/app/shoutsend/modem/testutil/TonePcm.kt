package app.shoutsend.modem.testutil

import kotlin.math.PI
import kotlin.math.sin

/**
 * Independent reference sine wave and silence generator for modem test verification.
 * Not used by production code.
 */
object TonePcm {

    fun generateSine(
        frequencyHz: Double,
        durationSeconds: Double,
        sampleRateHz: Int,
        amplitude: Float = 1.0f,
        initialPhase: Double = 0.0
    ): FloatArray {
        val sampleCount = (durationSeconds * sampleRateHz).toInt()
        return generateSineSamples(frequencyHz, sampleCount, sampleRateHz, amplitude, initialPhase)
    }

    fun generateSineSamples(
        frequencyHz: Double,
        sampleCount: Int,
        sampleRateHz: Int,
        amplitude: Float = 1.0f,
        initialPhase: Double = 0.0
    ): FloatArray {
        val output = FloatArray(sampleCount)
        val step = 2.0 * PI * frequencyHz / sampleRateHz
        var phase = initialPhase
        for (i in 0 until sampleCount) {
            output[i] = (amplitude * sin(phase)).toFloat()
            phase += step
            if (phase >= 2.0 * PI) {
                phase -= 2.0 * PI
            }
        }
        return output
    }

    fun generateSilence(durationSeconds: Double, sampleRateHz: Int): FloatArray {
        val sampleCount = (durationSeconds * sampleRateHz).toInt()
        return FloatArray(sampleCount)
    }

    fun generateSilenceSamples(sampleCount: Int): FloatArray {
        return FloatArray(sampleCount)
    }
}
