package app.shoutsend.modem.synth

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos

/**
 * Precomputed raised-cosine (Hann transition) ramp envelope tables.
 *
 * Guaranteed invariant:
 * `rampUp[i] + rampDown[i] == 1.0f` for all `i in 0 until rampLength`.
 */
public class RampTable(
    public val rampLength: Int,
    public val rampUp: FloatArray,
    public val rampDown: FloatArray
)

/**
 * Windowing utility for calculating and caching raised-cosine symbol transition ramps.
 */
public object RaisedCosineWindow {

    private val directTable: Array<RampTable?> = arrayOfNulls(2048)
    private val fallbackCache = ConcurrentHashMap<Int, RampTable>()

    /**
     * Evaluates a single raised-cosine ramp-up value for index [i] in ramp of length [rampLength].
     */
    public fun raisedCosineUp(i: Int, rampLength: Int): Float {
        if (rampLength <= 0) return 1.0f
        val clampedI = i.coerceIn(0, rampLength - 1)
        val phase = PI * (clampedI + 0.5) / rampLength
        return ((1.0 - cos(phase)) / 2.0).toFloat()
    }

    /**
     * Evaluates a single raised-cosine ramp-down value for index [i] in ramp of length [rampLength].
     */
    public fun raisedCosineDown(i: Int, rampLength: Int): Float {
        if (rampLength <= 0) return 0.0f
        val clampedI = i.coerceIn(0, rampLength - 1)
        val phase = PI * (clampedI + 0.5) / rampLength
        return ((1.0 + cos(phase)) / 2.0).toFloat()
    }

    /**
     * Retrieves or generates the precomputed [RampTable] for [rampLength].
     * Zero-allocation, lock-free lookup for all standard ramp lengths (< 2048).
     */
    public fun getRamp(rampLength: Int): RampTable {
        if (rampLength in 0 until 2048) {
            val existing = directTable[rampLength]
            if (existing != null) return existing
            val table = createRampTable(rampLength)
            directTable[rampLength] = table
            return table
        }
        val existing = fallbackCache[rampLength]
        if (existing != null) return existing
        val table = createRampTable(rampLength)
        fallbackCache[rampLength] = table
        return table
    }

    private fun createRampTable(r: Int): RampTable {
        val up = FloatArray(r)
        val down = FloatArray(r)
        for (i in 0 until r) {
            val phase = PI * (i + 0.5) / r
            val cosVal = cos(phase)
            val u = ((1.0 - cosVal) / 2.0).toFloat()
            val d = 1.0f - u // Exact mathematical complement ensures sum == 1.0f
            up[i] = u
            down[i] = d
        }
        return RampTable(r, up, down)
    }
}
