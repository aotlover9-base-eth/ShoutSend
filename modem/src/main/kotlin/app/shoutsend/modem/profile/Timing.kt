package app.shoutsend.modem.profile

import app.shoutsend.modem.AudioSpec
import kotlin.math.roundToInt

/**
 * Computes exact sample counts for tone symbol durations and raised-cosine ramp edges.
 */
public object Timing {

    /**
     * Total sample count for a symbol at the given [speed] and [audioSpec].
     */
    public fun symbolSamples(speed: Speed, audioSpec: AudioSpec): Int {
        return symbolSamples(speed.durationMs, audioSpec)
    }

    /**
     * Total sample count for an arbitrary duration in milliseconds.
     */
    public fun symbolSamples(durationMs: Int, audioSpec: AudioSpec): Int {
        val samples = durationMs.toDouble() * audioSpec.sampleRateHz / 1000.0
        return samples.roundToInt()
    }

    /**
     * Sample count for the raised-cosine transition ramp at the given [speed] and [audioSpec].
     */
    public fun rampSamples(speed: Speed, audioSpec: AudioSpec): Int {
        return rampSamples(speed.rampMs, audioSpec)
    }

    /**
     * Sample count for an arbitrary ramp duration in milliseconds.
     */
    public fun rampSamples(rampMs: Int, audioSpec: AudioSpec): Int {
        val samples = rampMs.toDouble() * audioSpec.sampleRateHz / 1000.0
        return samples.roundToInt()
    }
}
