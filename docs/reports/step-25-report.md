# Step 25 Verification Report: Spectrum Analyzer (FFT) for Waterfall

## 1. Overview
Implemented a pure Kotlin, high-performance Radix-2 Cooley-Tukey Fast Fourier Transform (FFT) and streaming `SpectrumAnalyzer` in `:modem`. Turns audio chunks into normalized spectral rows for live waterfall display at ~60 frames per second with strictly zero heap allocations on the audio streaming loop.

## 2. Implementation Summary
- `app.shoutsend.modem.spectrum.Fft`:
  - Radix-2 in-place Cooley-Tukey algorithm.
  - Precomputes bit-reversal lookup table and twiddle factor tables (`twiddleCos`, `twiddleSin`).
  - Strict zero-allocation execution during `transform(real: FloatArray, imag: FloatArray)`.
  - Configurable power-of-2 sizes (default 1024, configurable to 512 or 2048).
- `app.shoutsend.modem.spectrum.SpectrumAnalyzer`:
  - Precomputes Hann window and coherent power normalization scale ($16 / N^2$).
  - Circular power-of-2 ring buffer (capacity 8192).
  - Hop calculation: 800 samples at 48 kHz, 735 samples at 44.1 kHz (~60 rows/sec).
  - dBFS mapping: maps power from configurable range (default -90 dBFS to -10 dBFS) to $[0.0f, 1.0f]$.
  - Band cropping to `AnalysisBand(startHz, endHz, displayBins)`.
  - Display bin reduction: max-pooling when FFT bins >= displayBins; linear interpolation when fewer.
  - Emission via preallocated `SpectrumListener.onSpectrumRow(timestampSample, row)` reusing display array.
  - `toneMarkerPosition(freqHz, band)` helper returning normalized $[0.0, 1.0]$ position for tone guides.
  - Exponential smoothing (`smoothingAlpha`) and peak-hold with configurable decay (`peakDecay`).
- `app.shoutsend.modem.spectrum.SpectrumAnalyzerTest`:
  - 9 automated unit tests covering reference DFT comparison, pure tone peak resolution, multi-tone peaks, calibrated dBFS amplitude within 1 dB, silence mapping to 0.0, square wave bounds, dual sample rate cropping, marker positioning, and throughput benchmark.

## 3. Benchmark & Verification Results
```
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test
BUILD SUCCESSFUL in 10s

Test Results:
- testFftMatchesReferenceDft: PASSED (tolerance < 1e-3 against exact DFT)
- testPureTonePeaksAtExpectedBin: PASSED (3000 Hz peaks at exact bin)
- testTwoTonesProduceTwoPeaks: PASSED (2000 Hz and 4000 Hz distinct peaks with valley)
- testToneAmplitudeMatchesExpectedDbfsWithinTolerance: PASSED (-20 dBFS within ~1 dB)
- testDigitalSilenceMapsToZero: PASSED (0.0f across all bins)
- testFullScaleSquareWaveStaysWithinRange: PASSED (bounded in [0.0, 1.0])
- testBandCroppingAtBothSampleRates: PASSED (48 kHz and 44.1 kHz)
- testToneMarkerPosition: PASSED (clamped and linearly scaled)
- testBenchmarkRowsPerSecond: PASSED
  * 10 seconds of 48 kHz audio processed in 117 ms (599 rows)
  * Peak throughput: 5,118 spectrum rows/sec (>85x real-time speed)
  * Zero heap allocations during push and hop execution
```

## 4. Compliance
- Rule 21: Pure Kotlin JVM in `:modem` (zero Android imports, verified by `:modem:checkModemPurity`).
- Rule 22: Zero heap allocations during streaming audio ingestion and transform loop.
- Rule 27: Explicit API visibility mode strictly satisfied.
