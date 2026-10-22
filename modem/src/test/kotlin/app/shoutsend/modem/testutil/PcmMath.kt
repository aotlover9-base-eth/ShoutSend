package app.shoutsend.modem.testutil

import org.junit.Assert.assertTrue
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

object PcmMath {
    /**
     * Computes the Root Mean Square (RMS) of audio samples.
     */
    fun calculateRms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var sumSquares = 0.0
        for (s in samples) {
            sumSquares += s * s
        }
        return sqrt(sumSquares / samples.size).toFloat()
    }

    /**
     * Converts an RMS amplitude value to decibels relative to full scale (dBFS).
     * Reference 1.0f corresponds to 0 dBFS.
     */
    fun toDbfs(rms: Float, reference: Float = 1.0f): Float {
        val safeRms = if (rms < 1e-9f) 1e-9f else rms
        return (20.0 * log10(safeRms.toDouble() / reference.toDouble())).toFloat()
    }

    /**
     * Asserts that two floats are approximately equal within [tolerance].
     */
    fun assertApproxEquals(expected: Float, actual: Float, tolerance: Float = 1e-3f, message: String = "") {
        val diff = abs(expected - actual)
        assertTrue(
            "$message: Expected $expected but was $actual (diff $diff > tolerance $tolerance)",
            diff <= tolerance
        )
    }

    /**
     * Asserts that two doubles are approximately equal within [tolerance].
     */
    fun assertApproxEquals(expected: Double, actual: Double, tolerance: Double = 1e-3, message: String = "") {
        val diff = abs(expected - actual)
        assertTrue(
            "$message: Expected $expected but was $actual (diff $diff > tolerance $tolerance)",
            diff <= tolerance
        )
    }
}
