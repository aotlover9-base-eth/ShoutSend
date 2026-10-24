package app.shoutsend.modem.channel

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Filter type for acoustic speaker/microphone frequency response simulation.
 */
public enum class FilterType {
    LOW_PASS,
    HIGH_PASS,
    NOTCH
}

/**
 * Filter configuration for speaker and microphone frequency shaping.
 *
 * @property type Filter response type (low-pass, high-pass, notch).
 * @property cutoffHz Cutoff or center frequency in Hz.
 * @property q Quality factor (default 0.707 for Butterworth flat response).
 */
public data class FilterSection(
    public val type: FilterType,
    public val cutoffHz: Double,
    public val q: Double = 0.707
)

/**
 * Hardware speaker and microphone acoustic frequency response presets.
 */
public enum class PhoneHardwarePreset {
    FLAT,
    GOOD_PHONE,
    WEAK_ULTRASOUND_PHONE,
    TINY_SPEAKER;

    /**
     * Returns filter sections representing the transducer acoustic characteristics.
     */
    public fun sections(): List<FilterSection> = when (this) {
        FLAT -> emptyList()
        // Flat acoustic response across vocal band and ultrasound (up to 21 kHz)
        GOOD_PHONE -> listOf(
            FilterSection(FilterType.HIGH_PASS, cutoffHz = 150.0, q = 0.707),
            FilterSection(FilterType.LOW_PASS, cutoffHz = 21000.0, q = 0.707)
        )
        // Budget or older phone microphone with sharp roll-off above 17 kHz
        WEAK_ULTRASOUND_PHONE -> listOf(
            FilterSection(FilterType.HIGH_PASS, cutoffHz = 150.0, q = 0.707),
            FilterSection(FilterType.LOW_PASS, cutoffHz = 17000.0, q = 0.85)
        )
        // Tiny smartphone loudspeaker with severe acoustic cut-off below 800 Hz
        TINY_SPEAKER -> listOf(
            FilterSection(FilterType.HIGH_PASS, cutoffHz = 800.0, q = 0.707),
            FilterSection(FilterType.LOW_PASS, cutoffHz = 20000.0, q = 0.707)
        )
    }
}

/**
 * Direct Form I / II Transposed Biquad IIR filter implementation.
 */
public class BiquadFilter(
    public val section: FilterSection,
    public val sampleRateHz: Int
) {
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a1: Double
    private val a2: Double

    private var z1: Double = 0.0
    private var z2: Double = 0.0

    init {
        val omega = 2.0 * PI * (section.cutoffHz / sampleRateHz.toDouble())
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        val alpha = sinOmega / (2.0 * section.q)

        var b0Raw = 0.0
        var b1Raw = 0.0
        var b2Raw = 0.0
        var a0Raw = 1.0 + alpha
        var a1Raw = -2.0 * cosOmega
        var a2Raw = 1.0 - alpha

        when (section.type) {
            FilterType.LOW_PASS -> {
                b0Raw = (1.0 - cosOmega) / 2.0
                b1Raw = 1.0 - cosOmega
                b2Raw = (1.0 - cosOmega) / 2.0
            }
            FilterType.HIGH_PASS -> {
                b0Raw = (1.0 + cosOmega) / 2.0
                b1Raw = -(1.0 + cosOmega)
                b2Raw = (1.0 + cosOmega) / 2.0
            }
            FilterType.NOTCH -> {
                b0Raw = 1.0
                b1Raw = -2.0 * cosOmega
                b2Raw = 1.0
                a0Raw = 1.0 + alpha
                a1Raw = -2.0 * cosOmega
                a2Raw = 1.0 - alpha
            }
        }

        b0 = b0Raw / a0Raw
        b1 = b1Raw / a0Raw
        b2 = b2Raw / a0Raw
        a1 = a1Raw / a0Raw
        a2 = a2Raw / a0Raw
    }

    /**
     * Filters normalized audio samples in place.
     */
    public fun process(samples: FloatArray, offset: Int = 0, count: Int = samples.size - offset) {
        for (i in offset until offset + count) {
            val x = samples[i].toDouble()
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            samples[i] = y.toFloat()
        }
    }

    /**
     * Resets filter internal state delay elements.
     */
    public fun reset() {
        z1 = 0.0
        z2 = 0.0
    }
}
