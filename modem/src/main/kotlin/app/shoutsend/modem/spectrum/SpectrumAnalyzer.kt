package app.shoutsend.modem.spectrum

import app.shoutsend.modem.AudioSpec
import app.shoutsend.modem.tuning.ModemTuning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Frequency analysis band configuration for spectrum display.
 *
 * @param startHz Lower frequency bound in Hz.
 * @param endHz Upper frequency bound in Hz.
 * @param displayBins Number of display points / columns across the band.
 */
public data class AnalysisBand(
    public val startHz: Double,
    public val endHz: Double,
    public val displayBins: Int
) {
    init {
        require(startHz >= 0.0) { "startHz must be non-negative, got: $startHz" }
        require(endHz > startHz) { "endHz ($endHz) must be greater than startHz ($startHz)" }
        require(displayBins > 0) { "displayBins must be positive, got: $displayBins" }
    }
}

/**
 * Preallocated listener callback delivering spectrum rows with zero allocations per frame.
 */
public fun interface SpectrumListener {
    /**
     * Invoked when a new spectral row is computed.
     *
     * @param timestampSample Sample index at the end of this FFT window.
     * @param row Normalized spectrum row with values in [0.0f, 1.0f]. Reused internally;
     *            callers must not retain a reference without copying.
     */
    public fun onSpectrumRow(timestampSample: Long, row: FloatArray)
}

/**
 * High-performance, streaming Spectrum Analyzer for the live waterfall display.
 *
 * Computes windowed FFTs at ~60 frames per second over ingested audio, converts magnitude
 * to normalized dBFS, crops to [AnalysisBand], and maps to [AnalysisBand.displayBins]
 * using max-pooling (when FFT bins >= displayBins) or linear interpolation (when fewer).
 *
 * Operates with strictly zero heap allocations on the audio streaming loop (Rule 22).
 */
public class SpectrumAnalyzer(
    public val spec: AudioSpec = AudioSpec.RATE_48K,
    public var band: AnalysisBand = DEFAULT_BAND,
    public val fftSize: Int = Fft.DEFAULT_SIZE,
    public val targetFps: Double = DEFAULT_TARGET_FPS,
    public val minDbfs: Double = DEFAULT_MIN_DBFS,
    public val maxDbfs: Double = DEFAULT_MAX_DBFS,
    private val listener: SpectrumListener? = null
) {
    public constructor(
        spec: AudioSpec,
        band: AnalysisBand,
        tuning: ModemTuning,
        listener: SpectrumListener? = null
    ) : this(
        spec = spec,
        band = band,
        fftSize = tuning.fftSize,
        minDbfs = tuning.spectrumMinDbfs.toDouble(),
        maxDbfs = tuning.spectrumMaxDbfs.toDouble(),
        listener = listener
    )

    init {
        require(fftSize > 0 && (fftSize and (fftSize - 1)) == 0) {
            "fftSize must be a power of 2, got: $fftSize"
        }
        require(minDbfs < maxDbfs) {
            "minDbfs ($minDbfs) must be less than maxDbfs ($maxDbfs)"
        }
    }

    private val fft = Fft(fftSize)
    public val hopSamples: Int = (spec.sampleRateHz / targetFps).toInt().coerceAtLeast(1)

    // Precomputed Hann window and coherent power scaling
    private val hannWindow = FloatArray(fftSize)
    // Coherent amplitude factor: for Hann window, coherent gain is 0.5.
    // For a sinusoid with peak amplitude A: sum_n (A * cos(...) * w[n]) = A * (N / 4).
    // Hence A = 4 * sqrt(re^2 + im^2) / N.
    // Power P = A^2 = 16 * (re^2 + im^2) / (N * N).
    private val coherentPowerScale: Double = 16.0 / (fftSize.toDouble() * fftSize.toDouble())

    init {
        val denom = (fftSize - 1).toDouble()
        for (i in 0 until fftSize) {
            hannWindow[i] = (0.5 * (1.0 - cos(2.0 * PI * i / denom))).toFloat()
        }
    }

    // Ring buffer: size power-of-2 >= 2 * fftSize
    private val ringCapacity = 8192
    private val ringMask = ringCapacity - 1
    private val ringBuffer = FloatArray(ringCapacity)
    private var writeHead: Long = 0L
    private var lastHopHead: Long = 0L

    // Preallocated buffers for FFT and spectrum row
    private val fftReal = FloatArray(fftSize)
    private val fftImag = FloatArray(fftSize)
    private val fullBandPowers = FloatArray(fftSize / 2 + 1)
    private var displayRow = FloatArray(band.displayBins)

    // Smoothing & peak-hold state
    private var smoothedRow = FloatArray(band.displayBins)
    private var peakHoldRow = FloatArray(band.displayBins)
    public var smoothingAlpha: Float = 0.0f // 0.0 = no smoothing, 0.8 = heavy smoothing
    public var peakDecay: Float = 0.05f     // amount subtracted per row

    /**
     * Updates the current analysis band, reallocating display buffers only on band change.
     */
    public fun updateBand(newBand: AnalysisBand) {
        if (band != newBand) {
            band = newBand
            if (displayRow.size != newBand.displayBins) {
                displayRow = FloatArray(newBand.displayBins)
                smoothedRow = FloatArray(newBand.displayBins)
                peakHoldRow = FloatArray(newBand.displayBins)
            }
        }
    }

    /**
     * Ingests float PCM samples normalized to [-1.0f, 1.0f].
     */
    public fun push(samples: FloatArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "Invalid range" }
        for (i in offset until offset + count) {
            ringBuffer[(writeHead and ringMask.toLong()).toInt()] = samples[i]
            writeHead++
            checkAndProcessHops()
        }
    }

    /**
     * Ingests 16-bit signed PCM samples.
     */
    public fun push(samples: ShortArray, offset: Int = 0, count: Int = samples.size - offset) {
        require(offset >= 0 && count >= 0 && offset + count <= samples.size) { "Invalid range" }
        for (i in offset until offset + count) {
            ringBuffer[(writeHead and ringMask.toLong()).toInt()] = samples[i] / 32768.0f
            writeHead++
            checkAndProcessHops()
        }
    }

    private fun checkAndProcessHops() {
        if (writeHead >= fftSize) {
            if (lastHopHead == 0L) {
                lastHopHead = writeHead - hopSamples
            }
            while (writeHead - lastHopHead >= hopSamples) {
                lastHopHead += hopSamples
                processFrame(lastHopHead)
            }
        }
    }

    /**
     * Processes an FFT frame centered / ending at [endSampleIndex].
     */
    public fun processFrame(endSampleIndex: Long) {
        val startSampleIndex = endSampleIndex - fftSize

        // 1. Copy windowed samples into fftReal and zero out fftImag
        var sampleIdx = startSampleIndex
        for (i in 0 until fftSize) {
            val s = ringBuffer[(sampleIdx and ringMask.toLong()).toInt()]
            fftReal[i] = s * hannWindow[i]
            fftImag[i] = 0.0f
            sampleIdx++
        }

        // 2. Perform in-place FFT
        fft.transform(fftReal, fftImag)

        // 3. Compute normalized power in dBFS for positive frequency bins [0..N/2]
        val halfN = fftSize / 2
        val dbRange = maxDbfs - minDbfs

        for (k in 0..halfN) {
            val r = fftReal[k].toDouble()
            val im = fftImag[k].toDouble()
            val rawPower = (r * r + im * im) * coherentPowerScale

            // Clamp small values before log10 to prevent -Infinity
            val safePower = if (rawPower < 1e-12) 1e-12 else rawPower
            val dbfs = 10.0 * log10(safePower)

            // Map [minDbfs, maxDbfs] -> [0.0f, 1.0f]
            val normalized = ((dbfs - minDbfs) / dbRange).toFloat().coerceIn(0.0f, 1.0f)
            fullBandPowers[k] = normalized
        }

        // 4. Crop to AnalysisBand and map to displayBins
        val sampleRate = spec.sampleRateHz.toDouble()
        val binHz = sampleRate / fftSize
        val kMin = (band.startHz / binHz).toInt().coerceIn(0, halfN)
        val kMax = (band.endHz / binHz).toInt().coerceIn(kMin, halfN)
        val fftBinsInBand = max(1, kMax - kMin + 1)
        val outBins = band.displayBins

        if (fftBinsInBand >= outBins) {
            // Max-pooling: downsample FFT bins to display bins
            for (d in 0 until outBins) {
                val bStart = kMin + (d * fftBinsInBand) / outBins
                var bEnd = kMin + ((d + 1) * fftBinsInBand) / outBins - 1
                if (bEnd < bStart) bEnd = bStart
                if (bEnd > kMax) bEnd = kMax

                var maxVal = fullBandPowers[bStart]
                for (b in bStart + 1..bEnd) {
                    if (fullBandPowers[b] > maxVal) {
                        maxVal = fullBandPowers[b]
                    }
                }
                displayRow[d] = maxVal
            }
        } else {
            // Linear interpolation: upsample FFT bins to display bins
            val denom = (outBins - 1).coerceAtLeast(1).toDouble()
            for (d in 0 until outBins) {
                val pos = kMin + d.toDouble() * (fftBinsInBand - 1) / denom
                val b0 = pos.toInt().coerceIn(kMin, kMax)
                val b1 = (b0 + 1).coerceIn(kMin, kMax)
                val frac = (pos - b0).toFloat()
                displayRow[d] = (1.0f - frac) * fullBandPowers[b0] + frac * fullBandPowers[b1]
            }
        }

        // 5. Apply smoothing and peak-hold if active
        if (smoothingAlpha > 0.0f) {
            val a = smoothingAlpha.coerceIn(0.0f, 1.0f)
            val invA = 1.0f - a
            for (d in 0 until outBins) {
                smoothedRow[d] = a * smoothedRow[d] + invA * displayRow[d]
                displayRow[d] = smoothedRow[d]
            }
        }

        if (peakDecay > 0.0f) {
            for (d in 0 until outBins) {
                val cur = displayRow[d]
                val decayed = max(0.0f, peakHoldRow[d] - peakDecay)
                peakHoldRow[d] = max(cur, decayed)
            }
        }

        // 6. Deliver spectrum row to listener
        listener?.onSpectrumRow(endSampleIndex, displayRow)
    }

    /**
     * Returns the normalized position [0.0f, 1.0f] of [freqHz] within [band].
     * Can be used to draw vertical or horizontal tone guides over the waterfall.
     */
    public fun toneMarkerPosition(freqHz: Double, band: AnalysisBand = this.band): Float {
        if (freqHz <= band.startHz) return 0.0f
        if (freqHz >= band.endHz) return 1.0f
        return ((freqHz - band.startHz) / (band.endHz - band.startHz)).toFloat()
    }

    /**
     * Resets internal ring buffer and timing counters.
     */
    public fun reset() {
        ringBuffer.fill(0.0f)
        writeHead = 0L
        lastHopHead = 0L
        displayRow.fill(0.0f)
        smoothedRow.fill(0.0f)
        peakHoldRow.fill(0.0f)
    }

    public companion object {
        public const val DEFAULT_TARGET_FPS: Double = 60.0
        public const val DEFAULT_MIN_DBFS: Double = -90.0
        public const val DEFAULT_MAX_DBFS: Double = -10.0
        public val DEFAULT_BAND: AnalysisBand = AnalysisBand(startHz = 1000.0, endHz = 22000.0, displayBins = 256)
    }
}
