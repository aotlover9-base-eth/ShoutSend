package app.shoutsend.core.feedback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure Kotlin in-memory acoustic earcon audio synthesizer.
 *
 * Generates subtle, non-intrusive acoustic feedback signals as 16-bit linear PCM
 * mono audio without external asset dependencies.
 */
object EarconSynthesizer {

    /** Default native sample rate in Hertz (Android standard primary rate). */
    const val DEFAULT_SAMPLE_RATE_HZ: Int = 48000

    private const val TWO_PI = 2.0 * PI

    /**
     * Synthesizes an upward chirp sweep earcon for transmission start.
     * Duration: 100 ms, sweeping from 550 Hz to 1350 Hz with raised cosine windowing.
     */
    fun synthesizeTransmitStart(sampleRate: Int = DEFAULT_SAMPLE_RATE_HZ): ShortArray {
        val durationSec = 0.100
        val sampleCount = (sampleRate * durationSec).toInt()
        val out = ShortArray(sampleCount)

        val f0 = 550.0
        val f1 = 1350.0
        val sweepRate = (f1 - f0) / durationSec
        val peakAmplitude = 0.35f * Short.MAX_VALUE

        val attackDuration = 0.010
        val decayDuration = 0.020
        val decayStart = durationSec - decayDuration

        for (n in 0 until sampleCount) {
            val t = n.toDouble() / sampleRate
            val phase = TWO_PI * (f0 * t + 0.5 * sweepRate * t * t)

            val envelope = when {
                t < attackDuration -> 0.5 * (1.0 - cos(PI * (t / attackDuration)))
                t > decayStart -> 0.5 * (1.0 + cos(PI * ((t - decayStart) / decayDuration)))
                else -> 1.0
            }

            val sample = (peakAmplitude * envelope * sin(phase)).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[n] = sample.toShort()
        }
        return out
    }

    /**
     * Synthesizes a subtle acoustic click earcon for preamble sync / reception lock.
     * Duration: 15 ms, 2200 Hz damped impulse with 3 ms exponential decay.
     */
    fun synthesizeReceiveLocked(sampleRate: Int = DEFAULT_SAMPLE_RATE_HZ): ShortArray {
        val durationSec = 0.015
        val sampleCount = (sampleRate * durationSec).toInt()
        val out = ShortArray(sampleCount)

        val frequency = 2200.0
        val tau = 0.003
        val attackDuration = 0.001
        val peakAmplitude = 0.30f * Short.MAX_VALUE

        for (n in 0 until sampleCount) {
            val t = n.toDouble() / sampleRate
            val attackEnv = if (t < attackDuration) 0.5 * (1.0 - cos(PI * (t / attackDuration))) else 1.0
            val decayEnv = exp(-t / tau)
            val envelope = attackEnv * decayEnv

            val phase = TWO_PI * frequency * t
            val sample = (peakAmplitude * envelope * sin(phase)).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[n] = sample.toShort()
        }
        return out
    }

    /**
     * Synthesizes an ascending chord chime for successful frame delivery or transmission completion.
     * Duration: 240 ms, 4 harmonic bell tones in an ascending major triad (C5, E5, G5, C6).
     */
    fun synthesizeSuccess(sampleRate: Int = DEFAULT_SAMPLE_RATE_HZ): ShortArray {
        val durationSec = 0.240
        val sampleCount = (sampleRate * durationSec).toInt()
        val out = ShortArray(sampleCount)

        data class Tone(val freq: Double, val startSec: Double, val durationSec: Double)
        val tones = listOf(
            Tone(523.25, 0.000, 0.120), // C5
            Tone(659.25, 0.045, 0.125), // E5
            Tone(783.99, 0.090, 0.130), // G5
            Tone(1046.50, 0.135, 0.105) // C6
        )

        val tau = 0.045
        val attackDuration = 0.006
        val peakAmplitude = 0.35f * Short.MAX_VALUE

        for (n in 0 until sampleCount) {
            val t = n.toDouble() / sampleRate
            var sum = 0.0

            for (tone in tones) {
                if (t >= tone.startSec && t < tone.startSec + tone.durationSec) {
                    val dt = t - tone.startSec
                    val attackEnv = if (dt < attackDuration) 0.5 * (1.0 - cos(PI * (dt / attackDuration))) else 1.0
                    val decayEnv = exp(-dt / tau)
                    val env = attackEnv * decayEnv

                    val phase1 = TWO_PI * tone.freq * dt
                    val phase2 = TWO_PI * (tone.freq * 2.0) * dt
                    val toneSample = env * (0.8 * sin(phase1) + 0.2 * sin(phase2))
                    sum += toneSample
                }
            }

            // Global soft taper at final 15 ms to ensure perfect zero-crossing finish
            val masterFade = if (t > 0.225) 0.5 * (1.0 + cos(PI * ((t - 0.225) / 0.015))) else 1.0
            val normalized = (sum * 0.55 * masterFade).coerceIn(-1.0, 1.0)
            val sample = (peakAmplitude * normalized).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[n] = sample.toShort()
        }
        return out
    }

    /**
     * Synthesizes a subtle low tone for transmission or decode failure.
     * Duration: 180 ms, dual low-frequency pulses (220 Hz down to 175 Hz).
     */
    fun synthesizeError(sampleRate: Int = DEFAULT_SAMPLE_RATE_HZ): ShortArray {
        val durationSec = 0.180
        val sampleCount = (sampleRate * durationSec).toInt()
        val out = ShortArray(sampleCount)

        val peakAmplitude = 0.32f * Short.MAX_VALUE
        val attackDuration = 0.008

        for (n in 0 until sampleCount) {
            val t = n.toDouble() / sampleRate
            val sampleVal: Double = when {
                t < 0.080 -> {
                    val dt = t
                    val toneDur = 0.080
                    val env = if (dt < attackDuration) {
                        0.5 * (1.0 - cos(PI * (dt / attackDuration)))
                    } else {
                        0.5 * (1.0 + cos(PI * ((dt - attackDuration) / (toneDur - attackDuration))))
                    }
                    env * sin(TWO_PI * 220.0 * dt)
                }
                t in 0.080..0.095 -> 0.0 // subtle gap between pulses
                else -> {
                    val dt = t - 0.095
                    val toneDur = 0.085
                    val env = if (dt < attackDuration) {
                        0.5 * (1.0 - cos(PI * (dt / attackDuration)))
                    } else {
                        0.5 * (1.0 + cos(PI * ((dt - attackDuration) / (toneDur - attackDuration))))
                    }
                    env * sin(TWO_PI * 175.0 * dt)
                }
            }

            val sample = (peakAmplitude * sampleVal).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[n] = sample.toShort()
        }
        return out
    }

    /**
     * Synthesizes audio samples for the specified [Earcon].
     */
    fun synthesize(earcon: Earcon, sampleRate: Int = DEFAULT_SAMPLE_RATE_HZ): ShortArray = when (earcon) {
        Earcon.TRANSMIT_START -> synthesizeTransmitStart(sampleRate)
        Earcon.RECEIVE_LOCKED -> synthesizeReceiveLocked(sampleRate)
        Earcon.SUCCESS -> synthesizeSuccess(sampleRate)
        Earcon.ERROR -> synthesizeError(sampleRate)
    }

    // Lazily pre-synthesized PCM buffers at default 48 kHz for zero runtime heap allocation
    val transmitStartPcm: ShortArray by lazy { synthesizeTransmitStart() }
    val receiveLockedPcm: ShortArray by lazy { synthesizeReceiveLocked() }
    val successPcm: ShortArray by lazy { synthesizeSuccess() }
    val errorPcm: ShortArray by lazy { synthesizeError() }
}
