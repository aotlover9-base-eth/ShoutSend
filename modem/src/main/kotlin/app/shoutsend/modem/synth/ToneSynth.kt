package app.shoutsend.modem.synth

import app.shoutsend.modem.profile.TonePlan
import java.util.Arrays
import kotlin.math.PI
import kotlin.math.sin

/**
 * Deterministic audio sample synthesizer with raised-cosine windowing across symbol transitions.
 *
 * Implements:
 * 1. Absolute phase continuous oscillators: `sin(2 * pi * f * n / sampleRate)`
 * 2. Hann raised-cosine ramp transitions ensuring envelope conservation (`up + down == 1.0f`)
 * 3. Exact bit-identical rendering across arbitrary chunk sizes (1, 7, 480, 10,000, etc.)
 * 4. Zero memory allocations during steady-state rendering.
 */
public object ToneSynth {

    /**
     * Default peak audio level for single-tone emissions (0.8 of full scale).
     */
    public const val DEFAULT_OUTPUT_LEVEL: Float = 0.8f

    /**
     * Default peak audio level per tone for two-tone DTFSK emissions (0.4 of full scale).
     */
    public const val DEFAULT_DTFSK_LEVEL_PER_TONE: Float = 0.4f

    /**
     * Renders floating-point audio samples for the requested range `[startSample, startSample + count)` into [out].
     *
     * @param schedule Ordered timeline of symbol events.
     * @param tonePlan Tone plan mapping tone indices to acoustic frequencies.
     * @param startSample Absolute zero-based sample index of the first sample to render.
     * @param out Preallocated float array to receive normalized audio samples.
     * @param outOffset Starting offset within [out].
     * @param count Number of samples to render.
     */
    public fun render(
        schedule: SymbolSchedule,
        tonePlan: TonePlan,
        startSample: Long,
        out: FloatArray,
        outOffset: Int = 0,
        count: Int = out.size - outOffset
    ) {
        require(startSample >= 0L) { "startSample cannot be negative: $startSample" }
        require(outOffset >= 0) { "outOffset cannot be negative: $outOffset" }
        require(count >= 0) { "count cannot be negative: $count" }
        require(outOffset + count <= out.size) {
            "out buffer overflow: outOffset ($outOffset) + count ($count) > out.size (${out.size})"
        }

        if (count == 0) return

        val endOffset = outOffset + count
        Arrays.fill(out, outOffset, endOffset, 0.0f)

        if (schedule.events.isEmpty() || startSample >= schedule.totalSamples) {
            return
        }

        val windowEnd = startSample + count
        val sampleRateHz = tonePlan.audioSpec.sampleRateHz.toDouble()
        val firstCandidate = schedule.findFirstCandidateIndex(startSample)

        val eventCount = schedule.events.size
        var i = firstCandidate
        while (i < eventCount) {
            val event = schedule.events[i]
            if (event.startSample >= windowEnd) {
                // Schedule is strictly sorted by startSample; no subsequent events can overlap the window
                break
            }

            val symStart = event.startSample
            val symEnd = event.endSample
            val renderStart = if (symStart > startSample) symStart else startSample
            val renderEnd = if (symEnd < windowEnd) symEnd else windowEnd

            if (renderStart >= renderEnd) continue

            val rampSamples = event.rampSamples
            val lengthSamples = event.lengthSamples
            val level = event.levelPerTone
            val rampTable = if (rampSamples > 0) RaisedCosineWindow.getRamp(rampSamples) else null
            val steadyStart = symStart + rampSamples
            val decayStart = symStart + lengthSamples - rampSamples

            val toneIndexes = event.toneIndexes
            val numTones = toneIndexes.size

            if (numTones == 1) {
                val freq0 = tonePlan.frequency(toneIndexes[0])
                val omega0 = (2.0 * PI * freq0) / sampleRateHz

                for (s in renderStart until renderEnd) {
                    val envelope = when {
                        rampSamples == 0 -> 1.0f
                        s < steadyStart -> rampTable!!.rampUp[(s - symStart).toInt()]
                        s >= decayStart -> rampTable!!.rampDown[(s - decayStart).toInt()]
                        else -> 1.0f
                    }

                    val osc = sin(omega0 * s)
                    val outIdx = outOffset + (s - startSample).toInt()
                    out[outIdx] += (envelope * level * osc).toFloat()
                }
            } else {
                val freq0 = tonePlan.frequency(toneIndexes[0])
                val freq1 = tonePlan.frequency(toneIndexes[1])
                val omega0 = (2.0 * PI * freq0) / sampleRateHz
                val omega1 = (2.0 * PI * freq1) / sampleRateHz

                for (s in renderStart until renderEnd) {
                    val envelope = when {
                        rampSamples == 0 -> 1.0f
                        s < steadyStart -> rampTable!!.rampUp[(s - symStart).toInt()]
                        s >= decayStart -> rampTable!!.rampDown[(s - decayStart).toInt()]
                        else -> 1.0f
                    }

                    val osc = sin(omega0 * s) + sin(omega1 * s)
                    val outIdx = outOffset + (s - startSample).toInt()
                    out[outIdx] += (envelope * level * osc).toFloat()
                }
            }
            i++
        }

        // Final amplitude clamp [-1.0f, 1.0f]
        for (i in outOffset until endOffset) {
            val v = out[i]
            if (v > 1.0f) {
                out[i] = 1.0f
            } else if (v < -1.0f) {
                out[i] = -1.0f
            }
        }
    }
}
