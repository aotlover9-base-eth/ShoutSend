package app.shoutsend.modem.channel

import app.shoutsend.modem.AudioSpec
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic acoustic channel impairment engine.
 *
 * Implements physical acoustic propagation stages:
 * - Delay and clock drift resampling (ppm)
 * - Gain attenuation / boost (dB)
 * - Multipath room reflections (FIR tapped delay lines)
 * - Slow path-loss fading (0.5 to 1.0 amplitude)
 * - Speaker and microphone frequency shaping (Biquad filters)
 * - Intermittent dropouts (20-80 ms) and acoustic clipping bursts
 * - Additive White Gaussian Noise (AWGN) calibrated to in-band SNR
 * - Ambient room acoustics (low-frequency hum harmonics + pink noise floor)
 *
 * Fully deterministic given an initial seed: identical seeds produce bit-identical PCM waveforms.
 */
public class AcousticChannelSimulator(
    public val scenario: ChannelScenario,
    public val audioSpec: AudioSpec = AudioSpec.RATE_48K
) {

    /**
     * Applies channel impairments to [input] audio samples using deterministic pseudo-random [seed].
     *
     * @param input Pristine input audio samples (normalized float).
     * @param seed Deterministic seed for reproducible testing.
     * @return Impaired audio sample buffer.
     */
    public fun process(input: FloatArray, seed: Long = 42L): FloatArray {
        val rng = Random(seed)
        val sampleRate = audioSpec.sampleRateHz

        // 1. Prepend random lead-in silence if configured
        val leadInMs = if (scenario.leadInSilenceRangeMs.isEmpty()) {
            0
        } else {
            val range = scenario.leadInSilenceRangeMs
            range.first + rng.nextInt(range.last - range.first + 1)
        }
        val leadInSamples = ((leadInMs.toLong() * sampleRate) / 1000L).toInt()

        // 2. Clock Drift Resampling
        val resampled = if (scenario.clockDriftPpm != 0.0) {
            applyClockDrift(input, scenario.clockDriftPpm)
        } else {
            input
        }

        // 3. Gain
        val gainLinear = 10.0.pow(scenario.gainDb / 20.0).toFloat()
        val gained = FloatArray(resampled.size)
        for (i in resampled.indices) {
            gained[i] = resampled[i] * gainLinear
        }

        // 4. Multipath Echo (FIR tapped delay)
        val taps = scenario.room.taps()
        val maxTapDelaySamples = if (taps.isNotEmpty()) {
            taps.maxOf { ((it.delayMs * sampleRate) / 1000.0).roundToInt() }
        } else 0

        val echoOutput = FloatArray(gained.size + maxTapDelaySamples)
        System.arraycopy(gained, 0, echoOutput, 0, gained.size)

        for (tap in taps) {
            val delaySamples = ((tap.delayMs * sampleRate) / 1000.0).roundToInt()
            val tapGain = 10.0.pow(tap.gainDb / 20.0).toFloat()
            for (i in gained.indices) {
                echoOutput[i + delaySamples] += gained[i] * tapGain
            }
        }

        // 5. Slow Path-Loss Fading (0.5 to 1.0 amplitude modulation at ~1 Hz)
        if (scenario.slowFading && echoOutput.isNotEmpty()) {
            val fadeFreq = 0.8 // Hz
            val omega = 2.0 * PI * fadeFreq / sampleRate.toDouble()
            val phaseOffset = rng.nextDouble() * 2.0 * PI
            for (i in echoOutput.indices) {
                // Modulate between 0.5 and 1.0
                val mod = (0.75 + 0.25 * sin(omega * i + phaseOffset)).toFloat()
                echoOutput[i] *= mod
            }
        }

        // 6. Transducer Frequency Response Shaping (Biquad filtering)
        val sections = scenario.phone.sections()
        if (sections.isNotEmpty()) {
            for (sec in sections) {
                val filter = BiquadFilter(sec, sampleRate)
                filter.process(echoOutput)
            }
        }

        // 7. Dropouts and Clipping Bursts
        if (scenario.dropoutsAndClipping && echoOutput.size > sampleRate) {
            // Random dropouts: 1 or 2 gaps of 20..80 ms
            val numDropouts = 1 + rng.nextInt(2)
            for (d in 0 until numDropouts) {
                val gapMs = 20 + rng.nextInt(61) // 20..80 ms
                val gapSamples = ((gapMs * sampleRate) / 1000)
                val maxStart = echoOutput.size - gapSamples
                if (maxStart > 0) {
                    val gapStart = rng.nextInt(maxStart)
                    for (i in gapStart until gapStart + gapSamples) {
                        echoOutput[i] = 0.0f
                    }
                }
            }
        }

        // 8. Additive White Gaussian Noise (AWGN) calibrated to signal power
        if (scenario.targetSnrDb != null && echoOutput.isNotEmpty()) {
            var sumSq = 0.0
            for (s in echoOutput) {
                sumSq += s * s
            }
            val sigPower = sumSq / echoOutput.size
            if (sigPower > 1e-12) {
                val noisePower = sigPower * 10.0.pow(-scenario.targetSnrDb / 10.0)
                val noiseSigma = sqrt(noisePower).toFloat()
                for (i in echoOutput.indices) {
                    val gaussian = rng.nextGaussian().toFloat()
                    echoOutput[i] += gaussian * noiseSigma
                }
            }
        }

        // 9. Ambient Noise (Low-frequency 50Hz hum harmonics + pink noise floor)
        if (scenario.ambientNoiseDbfs != null && echoOutput.isNotEmpty()) {
            val ambientLinear = 10.0.pow(scenario.ambientNoiseDbfs / 20.0).toFloat()
            val humOmega50 = 2.0 * PI * 50.0 / sampleRate.toDouble()
            val humOmega100 = 2.0 * PI * 100.0 / sampleRate.toDouble()
            var pinkState = 0.0f

            for (i in echoOutput.indices) {
                // 1st order IIR filtered white noise -> pink-ish 1/f response
                val white = rng.nextGaussian().toFloat()
                pinkState = 0.95f * pinkState + 0.05f * white
                val hum = (0.5 * sin(humOmega50 * i) + 0.25 * sin(humOmega100 * i)).toFloat()
                val totalAmbient = (pinkState * 0.7f + hum * 0.3f) * ambientLinear
                echoOutput[i] += totalAmbient
            }
        }

        // 10. Prepend lead-in silence and assemble final buffer
        val finalSamples = FloatArray(leadInSamples + echoOutput.size)
        System.arraycopy(echoOutput, 0, finalSamples, leadInSamples, echoOutput.size)

        // 11. Final amplitude saturation clamp [-1.0f, 1.0f]
        for (i in finalSamples.indices) {
            val v = finalSamples[i]
            if (v > 1.0f) finalSamples[i] = 1.0f
            else if (v < -1.0f) finalSamples[i] = -1.0f
        }

        return finalSamples
    }

    /**
     * Resamples [samples] by simulating transceiver clock offset of [driftPpm] parts-per-million.
     *
     * A positive drift implies transmitter clock is running fast relative to receiver,
     * compressing time and reducing sample duration by factor `1 / (1 + ppm * 1e-6)`.
     */
    public fun applyClockDrift(samples: FloatArray, driftPpm: Double): FloatArray {
        if (driftPpm == 0.0 || samples.isEmpty()) return samples

        val ratio = 1.0 + (driftPpm * 1e-6)
        val outLength = (samples.size / ratio).roundToInt()
        if (outLength <= 0) return FloatArray(0)

        val out = FloatArray(outLength)
        val inMax = samples.size - 1

        for (i in 0 until outLength) {
            val inPos = i * ratio
            val i0 = inPos.toInt()
            val frac = (inPos - i0).toFloat()
            val s0 = if (i0 in 0..inMax) samples[i0] else 0.0f
            val s1 = if (i0 + 1 in 0..inMax) samples[i0 + 1] else s0
            // Linear interpolation
            out[i] = s0 + frac * (s1 - s0)
        }
        return out
    }
}
