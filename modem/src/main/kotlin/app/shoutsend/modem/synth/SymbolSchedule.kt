package app.shoutsend.modem.synth

/**
 * Ordered timeline of scheduled acoustic symbol emissions.
 *
 * Provides fast binary-search range queries to identify active symbols for arbitrary audio sample chunks.
 */
public class SymbolSchedule(
    public val events: List<SymbolEvent>
) {
    /**
     * Total duration of the schedule in samples.
     */
    public val totalSamples: Long = events.maxOfOrNull { it.endSample } ?: 0L

    init {
        for (i in 0 until events.size - 1) {
            val curr = events[i]
            val next = events[i + 1]
            require(curr.startSample <= next.startSample) {
                "SymbolEvents must be sorted by startSample: event $i starts at ${curr.startSample}, next at ${next.startSample}"
            }
        }
    }

    /**
     * Finds the index of the first symbol whose [endSample] is strictly greater than [sample].
     * Returns [events.size] if all symbols end before [sample].
     */
    public fun findFirstCandidateIndex(sample: Long): Int {
        var low = 0
        var high = events.size - 1
        var result = events.size

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (events[mid].endSample > sample) {
                result = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return result
    }

    public companion object {
        public val EMPTY: SymbolSchedule = SymbolSchedule(emptyList())
    }
}
