package app.shoutsend.modem.synth

import kotlin.math.round

/**
 * High-performance, zero-allocation PCM conversion utilities between normalized 32-bit floating point
 * `[-1.0f, 1.0f]` and 16-bit signed linear PCM `[-32768, 32767]`.
 *
 * Implements round-to-nearest without dithering and strictly clamps values at extremes.
 */
public object PcmConverter {

    /**
     * Maximum positive magnitude for 16-bit signed PCM (32,767).
     */
    public const val MAX_PCM_VALUE: Short = 32767

    /**
     * Maximum negative magnitude for 16-bit signed PCM (-32,768).
     */
    public const val MIN_PCM_VALUE: Short = -32768

    /**
     * Converts a single normalized float sample `[-1.0f, 1.0f]` to 16-bit signed PCM.
     * Values outside `[-1.0f, 1.0f]` are safely clamped to [MIN_PCM_VALUE] or [MAX_PCM_VALUE].
     */
    public fun floatToShort(sample: Float): Short {
        val clamped = sample.coerceIn(-1.0f, 1.0f)
        val scaled = if (clamped >= 0.0f) {
            clamped * 32767.0f
        } else {
            clamped * 32768.0f
        }
        val rounded = round(scaled).toInt()
        return rounded.coerceIn(MIN_PCM_VALUE.toInt(), MAX_PCM_VALUE.toInt()).toShort()
    }

    /**
     * Converts a single 16-bit signed PCM sample to a normalized float `[-1.0f, 1.0f]`.
     */
    public fun shortToFloat(sample: Short): Float {
        return if (sample >= 0) {
            sample.toFloat() / 32767.0f
        } else {
            sample.toFloat() / 32768.0f
        }
    }

    /**
     * In-place batch conversion from float samples to short PCM samples. Zero heap allocations.
     */
    public fun convertFloatsToShorts(
        src: FloatArray,
        srcOffset: Int,
        dst: ShortArray,
        dstOffset: Int,
        count: Int
    ) {
        require(srcOffset >= 0 && dstOffset >= 0 && count >= 0) { "Offsets and count must be non-negative" }
        require(srcOffset + count <= src.size) { "Source buffer overflow" }
        require(dstOffset + count <= dst.size) { "Destination buffer overflow" }

        for (i in 0 until count) {
            dst[dstOffset + i] = floatToShort(src[srcOffset + i])
        }
    }

    /**
     * In-place batch conversion from short PCM samples to float samples. Zero heap allocations.
     */
    public fun convertShortsToFloats(
        src: ShortArray,
        srcOffset: Int,
        dst: FloatArray,
        dstOffset: Int,
        count: Int
    ) {
        require(srcOffset >= 0 && dstOffset >= 0 && count >= 0) { "Offsets and count must be non-negative" }
        require(srcOffset + count <= src.size) { "Source buffer overflow" }
        require(dstOffset + count <= dst.size) { "Destination buffer overflow" }

        for (i in 0 until count) {
            dst[dstOffset + i] = shortToFloat(src[srcOffset + i])
        }
    }

    /**
     * Converts a slice of a float array to a newly allocated 16-bit PCM ShortArray.
     */
    public fun toShortArray(
        floats: FloatArray,
        offset: Int = 0,
        count: Int = floats.size - offset
    ): ShortArray {
        val result = ShortArray(count)
        convertFloatsToShorts(floats, offset, result, 0, count)
        return result
    }

    /**
     * Converts a slice of a short PCM array to a newly allocated normalized FloatArray.
     */
    public fun toFloatArray(
        shorts: ShortArray,
        offset: Int = 0,
        count: Int = shorts.size - offset
    ): FloatArray {
        val result = FloatArray(count)
        convertShortsToFloats(shorts, offset, result, 0, count)
        return result
    }

    /**
     * Converts a slice of 16-bit signed PCM samples to little-endian bytes.
     */
    public fun toByteArray(
        shorts: ShortArray,
        offset: Int = 0,
        count: Int = shorts.size - offset
    ): ByteArray {
        val result = ByteArray(count * 2)
        for (i in 0 until count) {
            val s = shorts[offset + i].toInt()
            result[i * 2] = (s and 0xFF).toByte()
            result[i * 2 + 1] = ((s ushr 8) and 0xFF).toByte()
        }
        return result
    }

    /**
     * Reconstructs 16-bit signed PCM samples from little-endian bytes.
     */
    public fun fromByteArray(
        bytes: ByteArray,
        offset: Int = 0,
        count: Int = (bytes.size - offset) / 2
    ): ShortArray {
        val result = ShortArray(count)
        for (i in 0 until count) {
            val low = bytes[offset + i * 2].toInt() and 0xFF
            val high = bytes[offset + i * 2 + 1].toInt()
            result[i] = ((high shl 8) or low).toShort()
        }
        return result
    }
}
