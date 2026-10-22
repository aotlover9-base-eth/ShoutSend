package app.shoutsend.modem.detector

/**
 * Streaming audio wrapper accepting arbitrary-size sample chunks and delivering tone energy blocks
 * at a regular hop interval.
 *
 * Implements an internal zero-allocation circular ring buffer.
 * Output is emitted via [listener] using preallocated arrays.
 */
public class BlockDetector(
    public val bank: GoertzelBank,
    public val hopSize: Int = bank.blockSize,
    public val listener: EnergyListener
) {
    init {
        require(hopSize > 0) { "hopSize must be positive: $hopSize" }
        require(hopSize <= bank.blockSize) {
            "hopSize ($hopSize) cannot exceed bank.blockSize (${bank.blockSize})"
        }
    }

    public val blockSize: Int = bank.blockSize

    // Ring buffer capacity comfortably accommodates incoming chunks and overlap
    private val bufferCapacity = maxOf(blockSize * 4, 8192)
    private val ringBuffer = FloatArray(bufferCapacity)
    private var head = 0
    private var bufferedCount = 0
    private var absoluteSampleIndex = 0L

    // Preallocated linear block scratch and output buffer for zero-allocation execution
    private val blockScratch = FloatArray(blockSize)
    private val energiesOut = FloatArray(bank.filterCount)

    /**
     * Feeds normalized floating-point samples into the detector.
     * Evaluates Goertzel filters and triggers [listener] whenever a full block of [blockSize]
     * is available, advancing by [hopSize]. Zero heap allocations.
     */
    public fun process(samples: FloatArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0) { "offset cannot be negative: $offset" }
        require(count >= 0) { "count cannot be negative: $count" }
        require(offset + count <= samples.size) { "Buffer overflow: offset + count > samples.size" }

        var inOffset = offset
        var remaining = count

        while (remaining > 0) {
            val freeSpace = bufferCapacity - bufferedCount
            val chunk = if (remaining < freeSpace) remaining else freeSpace
            if (chunk == 0) {
                // Ring buffer full (should not happen when drained)
                break
            }

            var i = 0
            while (i < chunk) {
                val writeIdx = (head + bufferedCount) % bufferCapacity
                ringBuffer[writeIdx] = samples[inOffset + i]
                bufferedCount++
                i++
            }
            inOffset += chunk
            remaining -= chunk

            drainBlocks()
        }
    }

    /**
     * Feeds 16-bit signed PCM samples into the detector.
     * Converts to normalized float and evaluates with zero heap allocations.
     */
    public fun process(samples: ShortArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0) { "offset cannot be negative: $offset" }
        require(count >= 0) { "count cannot be negative: $count" }
        require(offset + count <= samples.size) { "Buffer overflow: offset + count > samples.size" }

        var inOffset = offset
        var remaining = count

        while (remaining > 0) {
            val freeSpace = bufferCapacity - bufferedCount
            val chunk = if (remaining < freeSpace) remaining else freeSpace
            if (chunk == 0) {
                break
            }

            var i = 0
            while (i < chunk) {
                val raw = samples[inOffset + i]
                val sFloat = if (raw >= 0) {
                    raw.toFloat() / 32767.0f
                } else {
                    raw.toFloat() / 32768.0f
                }
                val writeIdx = (head + bufferedCount) % bufferCapacity
                ringBuffer[writeIdx] = sFloat
                bufferedCount++
                i++
            }
            inOffset += chunk
            remaining -= chunk

            drainBlocks()
        }
    }

    private fun drainBlocks() {
        while (bufferedCount >= blockSize) {
            var n = 0
            while (n < blockSize) {
                val readIdx = (head + n) % bufferCapacity
                blockScratch[n] = ringBuffer[readIdx]
                n++
            }

            bank.energies(blockScratch, 0, energiesOut)
            listener.onBlockEnergies(absoluteSampleIndex, energiesOut)

            head = (head + hopSize) % bufferCapacity
            bufferedCount -= hopSize
            absoluteSampleIndex += hopSize
        }
    }

    /**
     * Resets internal ring buffer state and resets absolute sample counter to 0.
     */
    public fun reset() {
        head = 0
        bufferedCount = 0
        absoluteSampleIndex = 0L
    }
}
