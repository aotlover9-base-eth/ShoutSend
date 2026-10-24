package app.shoutsend.core.audio.receiver

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * High-performance, lock-free Single-Producer Single-Consumer (SPSC) ring buffer
 * for streaming 16-bit linear PCM audio samples between real-time audio threads.
 *
 * Guarantees zero heap allocations during [write] and [read] operations.
 *
 * @param capacity Must be a positive power of 2 (defaults to 131,072 samples = ~2.73 seconds at 48 kHz).
 */
class SpscRingBuffer(
    val capacity: Int = 131072
) {
    init {
        require(capacity > 0 && (capacity and (capacity - 1)) == 0) {
            "Capacity must be a positive power of 2, got $capacity"
        }
    }

    private val mask = capacity - 1
    private val buffer = ShortArray(capacity)

    // Independent cache-line aligned heads (avoiding false sharing)
    private val writeHead = AtomicLong(0L)
    private val readHead = AtomicLong(0L)

    private val overruns = AtomicLong(0L)

    /**
     * Total number of samples dropped due to buffer capacity saturation.
     */
    val overrunCount: Long
        get() = overruns.get()

    /**
     * Current number of unread samples stored in the ring buffer.
     */
    val availableSamples: Int
        get() {
            val w = writeHead.get()
            val r = readHead.get()
            return (w - r).coerceIn(0L, capacity.toLong()).toInt()
        }

    /**
     * Free capacity remaining in the buffer for writes before saturation.
     */
    val availableCapacity: Int
        get() = capacity - availableSamples

    /**
     * Writes up to [count] samples from [source] into the ring buffer.
     * Called strictly from the producer thread (e.g., AudioRecord reader).
     *
     * If free space is insufficient, writes as much as fits and records the remaining
     * dropped samples in [overrunCount].
     *
     * @return Number of samples successfully written into the buffer.
     */
    fun write(source: ShortArray, offset: Int = 0, count: Int = source.size - offset): Int {
        if (count <= 0) return 0

        val w = writeHead.get()
        val r = readHead.get()
        val inFlight = (w - r).toInt()
        val space = capacity - inFlight

        val toWrite = min(count, space)
        val dropped = count - toWrite
        if (dropped > 0) {
            overruns.addAndGet(dropped.toLong())
        }

        var idx = w
        for (i in offset until offset + toWrite) {
            buffer[(idx and mask.toLong()).toInt()] = source[i]
            idx++
        }

        // Lazy / volatile store ensures array elements are visible before writeHead advances
        writeHead.lazySet(w + toWrite)
        return toWrite
    }

    /**
     * Reads up to [count] samples from the ring buffer into [destination].
     * Called strictly from the consumer thread (e.g., DSP pipeline).
     *
     * @return Number of samples actually read (0 if empty).
     */
    fun read(destination: ShortArray, offset: Int = 0, count: Int = destination.size - offset): Int {
        if (count <= 0) return 0

        val w = writeHead.get()
        val r = readHead.get()
        val inFlight = (w - r).toInt()
        if (inFlight <= 0) return 0

        val toRead = min(count, inFlight)
        var idx = r
        for (i in offset until offset + toRead) {
            destination[i] = buffer[(idx and mask.toLong()).toInt()]
            idx++
        }

        readHead.lazySet(r + toRead)
        return toRead
    }

    /**
     * Clears all samples and resets indices.
     */
    fun clear() {
        val w = writeHead.get()
        readHead.set(w)
    }
}
