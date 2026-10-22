package app.shoutsend.modem.sync

/**
 * Symbol clock and fine timing drift tracker for active acoustic transmissions in the LOCKED state.
 *
 * Implements:
 * 1. Symbol boundary progression with sub-sample accumulator tracking.
 * 2. Early-versus-late energy comparison on frame markers for loop feedback timing correction.
 * 3. Tracking surviving at least +/- 100 ppm sample rate mismatch across multi-second transmissions.
 * 4. Automatic loss declaration when 3 consecutive frame markers are missed.
 */
public class TimingTracker(
    public val sampleRateHz: Int,
    public val loopGain: Float = 0.15f
) {
    private var symbolStartSample: Long = 0L
    private var subSampleDrift: Float = 0.0f
    private var missedMarkers: Int = 0
    private var totalCorrectionsApplied: Float = 0.0f

    /**
     * Initializes tracker with the first symbol's absolute start sample.
     */
    public fun reset(initialStartSample: Long) {
        symbolStartSample = initialStartSample
        subSampleDrift = 0.0f
        missedMarkers = 0
        totalCorrectionsApplied = 0.0f
    }

    /**
     * Current estimated start sample for the next symbol.
     */
    public fun currentSymbolStart(): Long {
        return symbolStartSample + subSampleDrift.toLong()
    }

    /**
     * Advances the nominal symbol boundary by [durationSamples].
     */
    public fun advanceSymbol(durationSamples: Int) {
        symbolStartSample += durationSamples
    }

    /**
     * Updates timing alignment using early and late energy estimates on a frame marker symbol.
     *
     * @param found Whether the expected frame marker tone was detected above threshold.
     * @param earlyEnergy Energy measured in early window (shifted by -D/8).
     * @param lateEnergy Energy measured in late window (shifted by +D/8).
     * @param symbolDurationSamples Nominal symbol duration in samples.
     * @return `true` if lock is maintained, or `false` if lock is lost (3 consecutive misses).
     */
    public fun updateFromMarker(
        found: Boolean,
        earlyEnergy: Float,
        lateEnergy: Float,
        symbolDurationSamples: Int
    ): Boolean {
        if (!found) {
            missedMarkers++
            return missedMarkers < 3
        }

        missedMarkers = 0
        val sum = earlyEnergy + lateEnergy
        if (sum > 1e-12f) {
            // Normalized timing error: positive when energy is late, negative when early
            val error = (lateEnergy - earlyEnergy) / sum
            val maxShift = symbolDurationSamples / 8.0f
            val shift = error * maxShift * loopGain
            subSampleDrift += shift
            totalCorrectionsApplied += shift
        }
        return true
    }

    /**
     * Count of consecutive missing frame markers.
     */
    public fun missedMarkerCount(): Int = missedMarkers

    /**
     * Total accumulated timing drift correction in samples.
     */
    public fun currentDrift(): Float = totalCorrectionsApplied
}
