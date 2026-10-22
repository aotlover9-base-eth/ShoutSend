package app.shoutsend.modem.spectrum

import app.shoutsend.modem.AudioSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class SpectrumAnalyzerTest {

    private fun referenceDft(real: FloatArray, imag: FloatArray): Pair<FloatArray, FloatArray> {
        val n = real.size
        val outR = FloatArray(n)
        val outI = FloatArray(n)
        for (k in 0 until n) {
            var sumR = 0.0
            var sumI = 0.0
            for (t in 0 until n) {
                val angle = -2.0 * PI * k * t / n
                val cosA = cos(angle)
                val sinA = sin(angle)
                sumR += real[t] * cosA - imag[t] * sinA
                sumI += real[t] * sinA + imag[t] * cosA
            }
            outR[k] = sumR.toFloat()
            outI[k] = sumI.toFloat()
        }
        return Pair(outR, outI)
    }

    @Test
    fun testFftMatchesReferenceDft() {
        val sizes = intArrayOf(64, 256, 512, 1024)
        val random = Random(42)

        for (n in sizes) {
            val fft = Fft(n)
            val real = FloatArray(n) { random.nextFloat() * 2.0f - 1.0f }
            val imag = FloatArray(n) { random.nextFloat() * 2.0f - 1.0f }

            val (expectedR, expectedI) = referenceDft(real, imag)

            val actualR = real.clone()
            val actualI = imag.clone()
            fft.transform(actualR, actualI)

            for (i in 0 until n) {
                assertEquals("Real mismatch at bin $i for N=$n", expectedR[i], actualR[i], 1e-3f)
                assertEquals("Imag mismatch at bin $i for N=$n", expectedI[i], actualI[i], 1e-3f)
            }
        }
    }

    @Test
    fun testPureTonePeaksAtExpectedBin() {
        val spec = AudioSpec(48000)
        val fftSize = 1024
        val targetFreq = 3000.0 // 3 kHz
        val band = AnalysisBand(startHz = 1000.0, endHz = 5000.0, displayBins = 256)

        var lastRow: FloatArray? = null
        val analyzer = SpectrumAnalyzer(
            spec = spec,
            band = band,
            fftSize = fftSize,
            listener = { _, row -> lastRow = row.clone() }
        )

        // Generate 3000 Hz sine wave
        val sampleCount = 4800
        val pcm = FloatArray(sampleCount) { t ->
            sin(2.0 * PI * targetFreq * t / spec.sampleRateHz).toFloat()
        }
        analyzer.push(pcm)

        val row = requireNotNull(lastRow) { "Expected at least one spectrum row emitted" }
        val maxIdx = row.indices.maxByOrNull { row[it] } ?: -1

        // Frequency represented by maxIdx:
        val peakFreq = band.startHz + (maxIdx.toDouble() / (band.displayBins - 1)) * (band.endHz - band.startHz)
        val binResolution = spec.sampleRateHz.toDouble() / fftSize // 46.875 Hz
        assertEquals(targetFreq, peakFreq, binResolution * 2.0)
    }

    @Test
    fun testTwoTonesProduceTwoPeaks() {
        val spec = AudioSpec(48000)
        val fftSize = 1024
        val freq1 = 2000.0
        val freq2 = 4000.0
        val band = AnalysisBand(startHz = 1000.0, endHz = 6000.0, displayBins = 500)

        var lastRow: FloatArray? = null
        val analyzer = SpectrumAnalyzer(
            spec = spec,
            band = band,
            fftSize = fftSize,
            listener = { _, row -> lastRow = row.clone() }
        )

        val pcm = FloatArray(4800) { t ->
            0.5f * sin(2.0 * PI * freq1 * t / spec.sampleRateHz).toFloat() +
                0.5f * sin(2.0 * PI * freq2 * t / spec.sampleRateHz).toFloat()
        }
        analyzer.push(pcm)

        val row = requireNotNull(lastRow)
        val pos1 = analyzer.toneMarkerPosition(freq1, band)
        val pos2 = analyzer.toneMarkerPosition(freq2, band)

        val bin1 = (pos1 * (band.displayBins - 1)).toInt()
        val bin2 = (pos2 * (band.displayBins - 1)).toInt()

        // Peaks should exist within +/- 3 bins of expected positions
        var maxNear1 = 0.0f
        for (b in (bin1 - 3).coerceAtLeast(0)..(bin1 + 3).coerceAtMost(band.displayBins - 1)) {
            if (row[b] > maxNear1) maxNear1 = row[b]
        }

        var maxNear2 = 0.0f
        for (b in (bin2 - 3).coerceAtLeast(0)..(bin2 + 3).coerceAtMost(band.displayBins - 1)) {
            if (row[b] > maxNear2) maxNear2 = row[b]
        }

        assertTrue("Tone 1 at $freq1 Hz peak ($maxNear1) should be prominent", maxNear1 > 0.6f)
        assertTrue("Tone 2 at $freq2 Hz peak ($maxNear2) should be prominent", maxNear2 > 0.6f)

        // Middle region should have lower valley
        val midBin = (bin1 + bin2) / 2
        assertTrue("Valley at $midBin should be noticeably lower than peaks", row[midBin] < maxNear1 - 0.2f)
    }

    @Test
    fun testToneAmplitudeMatchesExpectedDbfsWithinTolerance() {
        val spec = AudioSpec(48000)
        val fftSize = 1024
        // Pick an exact integer bin frequency: bin 64 -> 64 * 48000 / 1024 = 3000 Hz
        val exactFreq = 3000.0
        val amplitude = 0.1f // -20 dBFS
        val minDbfs = -90.0
        val maxDbfs = 0.0
        val band = AnalysisBand(startHz = 2000.0, endHz = 4000.0, displayBins = 200)

        var lastRow: FloatArray? = null
        val analyzer = SpectrumAnalyzer(
            spec = spec,
            band = band,
            fftSize = fftSize,
            minDbfs = minDbfs,
            maxDbfs = maxDbfs,
            listener = { _, row -> lastRow = row.clone() }
        )

        val pcm = FloatArray(2048) { t ->
            amplitude * sin(2.0 * PI * exactFreq * t / spec.sampleRateHz).toFloat()
        }
        analyzer.push(pcm)

        val row = requireNotNull(lastRow)
        val peakVal = row.maxOrNull() ?: 0.0f

        // -20 dBFS mapped into [-90, 0]: (-20 - (-90)) / (0 - (-90)) = 70 / 90 = 0.7778
        val expectedNormalized = ((-20.0 - minDbfs) / (maxDbfs - minDbfs)).toFloat()
        assertEquals(expectedNormalized, peakVal, 0.03f) // within ~1 dB equivalent
    }

    @Test
    fun testDigitalSilenceMapsToZero() {
        val spec = AudioSpec(48000)
        var lastRow: FloatArray? = null
        val analyzer = SpectrumAnalyzer(
            spec = spec,
            listener = { _, row -> lastRow = row.clone() }
        )

        val silence = FloatArray(2048) { 0.0f }
        analyzer.push(silence)

        val row = requireNotNull(lastRow)
        for (v in row) {
            assertEquals("Silence must map to 0.0f", 0.0f, v, 1e-6f)
        }
    }

    @Test
    fun testFullScaleSquareWaveStaysWithinRange() {
        val spec = AudioSpec(48000)
        var lastRow: FloatArray? = null
        val analyzer = SpectrumAnalyzer(
            spec = spec,
            listener = { _, row -> lastRow = row.clone() }
        )

        // 1 kHz square wave of full scale +/- 1.0
        val squareWave = FloatArray(2048) { t ->
            if ((t / 24) % 2 == 0) 1.0f else -1.0f
        }
        analyzer.push(squareWave)

        val row = requireNotNull(lastRow)
        for (v in row) {
            assertTrue("Value $v must be in [0.0, 1.0]", v in 0.0f..1.0f)
        }
    }

    @Test
    fun testBandCroppingAtBothSampleRates() {
        val rates = intArrayOf(48000, 44100)
        for (rate in rates) {
            val spec = AudioSpec(rate)
            val stealthBand = AnalysisBand(startHz = 15000.0, endHz = 21000.0, displayBins = 128)
            var rowEmitted = false
            val analyzer = SpectrumAnalyzer(
                spec = spec,
                band = stealthBand,
                listener = { _, row ->
                    assertEquals(128, row.size)
                    rowEmitted = true
                }
            )

            // Tone inside band: 18 kHz
            val pcm = FloatArray(2048) { t ->
                sin(2.0 * PI * 18000.0 * t / rate).toFloat()
            }
            analyzer.push(pcm)
            assertTrue("Row must be emitted at $rate Hz", rowEmitted)
        }
    }

    @Test
    fun testToneMarkerPosition() {
        val band = AnalysisBand(startHz = 1000.0, endHz = 5000.0, displayBins = 100)
        val analyzer = SpectrumAnalyzer(band = band)

        assertEquals(0.0f, analyzer.toneMarkerPosition(1000.0, band), 1e-4f)
        assertEquals(0.5f, analyzer.toneMarkerPosition(3000.0, band), 1e-4f)
        assertEquals(1.0f, analyzer.toneMarkerPosition(5000.0, band), 1e-4f)
        assertEquals(0.0f, analyzer.toneMarkerPosition(500.0, band), 1e-4f) // clamped
        assertEquals(1.0f, analyzer.toneMarkerPosition(6000.0, band), 1e-4f) // clamped
    }

    @Test
    fun testBenchmarkRowsPerSecond() {
        val spec = AudioSpec(48000)
        var rowCount = 0
        val analyzer = SpectrumAnalyzer(
            spec = spec,
            listener = { _, _ -> rowCount++ }
        )

        // 10 seconds of random noise
        val samples = FloatArray(48000 * 10) { Random.nextFloat() * 2.0f - 1.0f }

        val startNs = System.nanoTime()
        analyzer.push(samples)
        val durationMs = (System.nanoTime() - startNs) / 1_000_000.0

        println("Benchmark: processed 10s audio in ${durationMs}ms, produced $rowCount rows.")
        val rowsPerSecondThroughput = rowCount / (durationMs / 1000.0)
        println("Throughput: ${rowsPerSecondThroughput.toInt()} spectrum rows/sec")

        assertTrue("Should produce ~600 rows for 10s of audio", rowCount in 590..610)
        assertTrue("Processing should run vastly faster than real time", durationMs < 500.0)
    }
}
