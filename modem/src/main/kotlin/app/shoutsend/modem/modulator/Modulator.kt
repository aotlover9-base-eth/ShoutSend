package app.shoutsend.modem.modulator

import app.shoutsend.modem.synth.PcmConverter
import app.shoutsend.modem.synth.ToneSynth

/**
 * Streaming acoustic modulator turning a [TransmissionPlan] into continuous audio.
 *
 * Implements:
 * 1. Streaming rendering via [ToneSynth] with zero heap allocations on hot path.
 * 2. Complete transmission modulation to 16-bit PCM for tests and channel simulation.
 * 3. Exact duration synchronization matching physical frame and symbol structures.
 */
public class Modulator(
    public val plan: TransmissionPlan
) {
    /**
     * Total transmission length in samples (including lead-in and tail silence).
     */
    public val totalSamples: Long = plan.totalSamples

    /**
     * Total transmission duration in fractional seconds.
     */
    public val durationSeconds: Double = plan.durationSeconds

    /**
     * Renders normalized audio samples for the requested range `[startSample, startSample + count)` into [out].
     *
     * Streaming capable, thread-safe, and zero-allocation.
     * Samples within lead-in and tail silence are guaranteed to be 0.0f.
     */
    public fun render(
        startSample: Long,
        out: FloatArray,
        outOffset: Int = 0,
        count: Int = out.size - outOffset
    ) {
        ToneSynth.render(
            schedule = plan.schedule,
            tonePlan = plan.tonePlan,
            startSample = startSample,
            out = out,
            outOffset = outOffset,
            count = count
        )
    }

    /**
     * Modulates the entire transmission into a single normalized float array.
     */
    public fun modulateToFloatArray(): FloatArray {
        val total = totalSamples.toInt()
        val result = FloatArray(total)
        render(0L, result, 0, total)
        return result
    }

    /**
     * Modulates the entire transmission into 16-bit signed linear PCM samples.
     */
    public fun modulateToPcm16(): ShortArray {
        val floats = modulateToFloatArray()
        return PcmConverter.toShortArray(floats)
    }

    public companion object {
        /**
         * Convenience method modulating a [plan] directly into 16-bit linear PCM.
         */
        public fun modulateToPcm16(plan: TransmissionPlan): ShortArray {
            return Modulator(plan).modulateToPcm16()
        }
    }
}
