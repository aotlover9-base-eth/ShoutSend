package app.shoutsend.modem.testutil

import app.shoutsend.modem.AudioSpec

/**
 * Fluent builder for creating concatenated PCM test buffers (silence, tones, noise).
 */
class PcmBufferBuilder(public val audioSpec: AudioSpec) {

    private val samples = mutableListOf<Float>()

    fun addSilence(durationSeconds: Double): PcmBufferBuilder {
        val count = (durationSeconds * audioSpec.sampleRateHz).toInt()
        return addSilenceSamples(count)
    }

    fun addSilenceSamples(count: Int): PcmBufferBuilder {
        for (i in 0 until count) {
            samples.add(0f)
        }
        return this
    }

    fun addTone(
        frequencyHz: Double,
        durationSeconds: Double,
        amplitude: Float = 1.0f,
        initialPhase: Double = 0.0
    ): PcmBufferBuilder {
        val count = (durationSeconds * audioSpec.sampleRateHz).toInt()
        return addToneSamples(frequencyHz, count, amplitude, initialPhase)
    }

    fun addToneSamples(
        frequencyHz: Double,
        sampleCount: Int,
        amplitude: Float = 1.0f,
        initialPhase: Double = 0.0
    ): PcmBufferBuilder {
        val tone = TonePcm.generateSineSamples(
            frequencyHz = frequencyHz,
            sampleCount = sampleCount,
            sampleRateHz = audioSpec.sampleRateHz,
            amplitude = amplitude,
            initialPhase = initialPhase
        )
        for (s in tone) {
            samples.add(s)
        }
        return this
    }

    fun addSamples(extraSamples: FloatArray): PcmBufferBuilder {
        for (s in extraSamples) {
            samples.add(s)
        }
        return this
    }

    fun build(): FloatArray = samples.toFloatArray()

    fun sampleCount(): Int = samples.size
}
