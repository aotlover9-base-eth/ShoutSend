package app.shoutsend.modem.detector

import app.shoutsend.modem.AudioSpec
import kotlin.math.PI
import kotlin.math.cos

/**
 * High-performance, zero-allocation Goertzel filter bank for acoustic tone detection.
 *
 * Implements generalized second-order IIR Goertzel resonators:
 * - Difference equation: `q0 = 2 * cos(2 * pi * f / Fs) * q1 - q2 + x[n]`
 * - Squared magnitude: `|X|^2 = q1^2 + q2^2 - coeff * q1 * q2`
 * - Generalized coefficients allowing exact non-integer FFT bin tone detection.
 * - Optional precomputed Hann analysis window `w[n] = 0.5 * (1 - cos(2 * pi * (n + 0.5) / N))`.
 * - Calibrated power normalization: a pure sine wave of peak amplitude A yields power A^2 within 1%.
 */
public class GoertzelBank(
    public val frequencies: DoubleArray,
    public val audioSpec: AudioSpec,
    public val blockSize: Int,
    public val windowed: Boolean = true
) {
    init {
        require(frequencies.isNotEmpty()) { "Frequencies cannot be empty" }
        require(blockSize > 0) { "blockSize must be positive: $blockSize" }
    }

    /**
     * Number of frequency resonators in this bank.
     */
    public val filterCount: Int = frequencies.size

    // Precomputed generalized Goertzel coefficients: 2.0 * cos(2 * pi * f / Fs)
    private val coeffs = DoubleArray(filterCount) { i ->
        2.0 * cos((2.0 * PI * frequencies[i]) / audioSpec.sampleRateHz.toDouble())
    }

    // Precomputed Hann window weights (if windowed)
    private val windowTable = if (windowed) {
        FloatArray(blockSize) { n ->
            (0.5 * (1.0 - cos((2.0 * PI * (n + 0.5)) / blockSize.toDouble()))).toFloat()
        }
    } else null

    // Power normalization factor:
    // A sine wave s(n) = A * sin(omega * n) has complex amplitude A / (2j).
    // For rectangular window (unwindowed), sum_w = N, so |X|^2 = (A * N / 2)^2 = A^2 * N^2 / 4.
    //   -> normFactor = 4.0 / (N * N)
    // For Hann window, coherent gain sum_w = N / 2, so |X|^2 = (A * N / 4)^2 = A^2 * N^2 / 16.
    //   -> normFactor = 16.0 / (N * N)
    private val normFactor = if (windowed) {
        16.0f / (blockSize.toFloat() * blockSize.toFloat())
    } else {
        4.0f / (blockSize.toFloat() * blockSize.toFloat())
    }

    /**
     * Computes normalized power for each configured frequency over [blockSize] samples from [samples] at [offset].
     * Writes results into [out] (size at least [filterCount]).
     *
     * Invariant: A pure sine tone of peak amplitude A at frequency f yields power A^2 within 1%.
     * Guaranteed zero heap allocations on the hot path.
     */
    public fun energies(samples: FloatArray, offset: Int, out: FloatArray) {
        require(offset >= 0) { "offset cannot be negative: $offset" }
        require(offset + blockSize <= samples.size) {
            "Buffer underrun: offset ($offset) + blockSize ($blockSize) > samples.size (${samples.size})"
        }
        require(out.size >= filterCount) {
            "out buffer too small: ${out.size} < $filterCount"
        }

        val win = windowTable
        val nMax = blockSize
        val numFilters = filterCount

        var fIdx = 0
        while (fIdx < numFilters) {
            val coeff = coeffs[fIdx]
            var q0: Double
            var q1 = 0.0
            var q2 = 0.0

            if (win != null) {
                var n = 0
                while (n < nMax) {
                    val s = (samples[offset + n] * win[n]).toDouble()
                    q0 = coeff * q1 - q2 + s
                    q2 = q1
                    q1 = q0
                    n++
                }
            } else {
                var n = 0
                while (n < nMax) {
                    val s = samples[offset + n].toDouble()
                    q0 = coeff * q1 - q2 + s
                    q2 = q1
                    q1 = q0
                    n++
                }
            }

            val p = (q1 * q1 + q2 * q2 - coeff * q1 * q2).toFloat() * normFactor
            out[fIdx] = if (p > 0.0f) p else 0.0f
            fIdx++
        }
    }

    /**
     * Overload for 16-bit signed PCM ShortArray inputs.
     * Normalized so that full-scale 32767 has peak amplitude 1.0f.
     */
    public fun energies(samples: ShortArray, offset: Int, out: FloatArray) {
        require(offset >= 0) { "offset cannot be negative: $offset" }
        require(offset + blockSize <= samples.size) {
            "Buffer underrun: offset ($offset) + blockSize ($blockSize) > samples.size (${samples.size})"
        }
        require(out.size >= filterCount) {
            "out buffer too small: ${out.size} < $filterCount"
        }

        val win = windowTable
        val nMax = blockSize
        val numFilters = filterCount

        var fIdx = 0
        while (fIdx < numFilters) {
            val coeff = coeffs[fIdx]
            var q0: Double
            var q1 = 0.0
            var q2 = 0.0

            if (win != null) {
                var n = 0
                while (n < nMax) {
                    val rawSample = samples[offset + n]
                    val sFloat = if (rawSample >= 0) {
                        rawSample.toFloat() / 32767.0f
                    } else {
                        rawSample.toFloat() / 32768.0f
                    }
                    val s = (sFloat * win[n]).toDouble()
                    q0 = coeff * q1 - q2 + s
                    q2 = q1
                    q1 = q0
                    n++
                }
            } else {
                var n = 0
                while (n < nMax) {
                    val rawSample = samples[offset + n]
                    val sFloat = if (rawSample >= 0) {
                        rawSample.toFloat() / 32767.0f
                    } else {
                        rawSample.toFloat() / 32768.0f
                    }
                    val s = sFloat.toDouble()
                    q0 = coeff * q1 - q2 + s
                    q2 = q1
                    q1 = q0
                    n++
                }
            }

            val p = (q1 * q1 + q2 * q2 - coeff * q1 * q2).toFloat() * normFactor
            out[fIdx] = if (p > 0.0f) p else 0.0f
            fIdx++
        }
    }
}
