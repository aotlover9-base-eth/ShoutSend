package app.shoutsend.modem.spectrum

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, in-place, Radix-2 Cooley-Tukey Fast Fourier Transform (FFT).
 *
 * Precomputes twiddle factor tables and bit-reversal indices at construction time.
 * Executes forward transforms with strictly zero heap allocations during [transform]
 * (Rule 22).
 *
 * @param size The FFT length (must be a positive power of 2, e.g. 512, 1024, 2048).
 */
public class Fft(public val size: Int = DEFAULT_SIZE) {

    init {
        require(size > 0 && (size and (size - 1)) == 0) {
            "FFT size must be a power of 2, got: $size"
        }
    }

    private val bitReversalTable = IntArray(size)
    // Precomputed twiddles for N: cos(2*PI*k / N) and -sin(2*PI*k / N)
    private val twiddleCos = FloatArray(size / 2)
    private val twiddleSin = FloatArray(size / 2)

    init {
        // Precompute bit reversal permutation
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) {
            var rev = 0
            var temp = i
            for (b in 0 until bits) {
                rev = (rev shl 1) or (temp and 1)
                temp = temp shr 1
            }
            bitReversalTable[i] = rev
        }

        // Precompute twiddles for forward transform: exp(-2*PI*i*k/N)
        val angleStep = -2.0 * PI / size
        for (k in 0 until size / 2) {
            twiddleCos[k] = cos(angleStep * k).toFloat()
            twiddleSin[k] = sin(angleStep * k).toFloat()
        }
    }

    /**
     * Performs an in-place Radix-2 forward FFT on the provided [real] and [imag] arrays.
     *
     * Both arrays must be of length [size]. No heap allocations occur.
     */
    public fun transform(real: FloatArray, imag: FloatArray) {
        require(real.size == size && imag.size == size) {
            "Array lengths must match FFT size $size (got real=${real.size}, imag=${imag.size})"
        }

        // 1. Bit-reversal permutation
        for (i in 0 until size) {
            val j = bitReversalTable[i]
            if (j > i) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
        }

        // 2. Cooley-Tukey butterfly stages
        var len = 2
        while (len <= size) {
            val halfLen = len shr 1
            val twiddleStep = size / len

            var j = 0
            while (j < halfLen) {
                val twIdx = j * twiddleStep
                val wReal = twiddleCos[twIdx]
                val wImag = twiddleSin[twIdx]

                var k = 0
                while (k < size) {
                    val uIdx = k + j
                    val vIdx = uIdx + halfLen

                    val vR = real[vIdx]
                    val vI = imag[vIdx]

                    val tReal = vR * wReal - vI * wImag
                    val tImag = vR * wImag + vI * wReal

                    val uR = real[uIdx]
                    val uI = imag[uIdx]

                    real[uIdx] = uR + tReal
                    imag[uIdx] = uI + tImag
                    real[vIdx] = uR - tReal
                    imag[vIdx] = uI - tImag

                    k += len
                }
                j++
            }
            len = len shl 1
        }
    }

    public companion object {
        public const val DEFAULT_SIZE: Int = 1024
    }
}
