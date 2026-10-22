package app.shoutsend.modem.synth

/**
 * Metadata defining a single acoustic tone emission event in a transmission schedule.
 *
 * @property startSample Absolute start sample index where this symbol begins emitting.
 * @property lengthSamples Total duration of this symbol in samples (including overlap ramps).
 * @property rampSamples Duration in samples of the raised-cosine transition ramp (both attack and decay).
 * @property toneIndexes 1 tone for MFSK16, or 2 tones for DTFSK (0..15 data, 16 for S0, 17 for S1).
 * @property levelPerTone Peak amplitude for each tone (default 0.8 for MFSK16, 0.4 for DTFSK).
 */
public data class SymbolEvent(
    public val startSample: Long,
    public val lengthSamples: Int,
    public val rampSamples: Int,
    public val toneIndexes: IntArray,
    public val levelPerTone: Float = 0.8f
) {
    init {
        require(startSample >= 0L) { "startSample cannot be negative: $startSample" }
        require(lengthSamples > 0) { "lengthSamples must be positive: $lengthSamples" }
        require(rampSamples >= 0) { "rampSamples cannot be negative: $rampSamples" }
        require(rampSamples <= lengthSamples / 2) {
            "rampSamples ($rampSamples) cannot exceed half the symbol length ($lengthSamples / 2)"
        }
        require(toneIndexes.isNotEmpty() && toneIndexes.size <= 2) {
            "toneIndexes must contain 1 or 2 tones, got ${toneIndexes.size}"
        }
        require(levelPerTone > 0.0f && levelPerTone <= 1.0f) {
            "levelPerTone must be in (0.0, 1.0], got $levelPerTone"
        }
    }

    public val endSample: Long
        get() = startSample + lengthSamples

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolEvent) return false
        if (startSample != other.startSample) return false
        if (lengthSamples != other.lengthSamples) return false
        if (rampSamples != other.rampSamples) return false
        if (!toneIndexes.contentEquals(other.toneIndexes)) return false
        if (levelPerTone != other.levelPerTone) return false
        return true
    }

    override fun hashCode(): Int {
        var result = startSample.hashCode()
        result = 31 * result + lengthSamples
        result = 31 * result + rampSamples
        result = 31 * result + toneIndexes.contentHashCode()
        result = 31 * result + levelPerTone.hashCode()
        return result
    }
}
