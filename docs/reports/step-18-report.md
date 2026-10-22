# Step 18 Report: Goertzel Detector and Receiver Front End

**Date:** 2026-10-23
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 18 delivers the primary discrete acoustic frequency detector in `:modem` (`app.shoutsend.modem.detector`) conforming to Rules 21, 22 (zero allocations on reception hot paths), 23, and 27. It provides calibrated, high-speed energy estimation across arbitrary acoustic tone sets for both 48 kHz and 44.1 kHz audio streams.

Key components delivered:
- **`GoertzelBank`:** Filter bank evaluating generalized second-order IIR Goertzel resonators ($q_0 = 2\cos(2\pi f / F_s) q_1 - q_2 + x[n]$) with double-precision state and precomputed Hann analysis window. Precomputed normalization factors ($\frac{16}{N^2}$ for windowed, $\frac{4}{N^2}$ for unwindowed) yield calibrated output power $A^2$ within 1% for input peak amplitude $A$. Zero allocations during block evaluation; supports both `FloatArray` and 16-bit linear PCM `ShortArray`.
- **`EnergyListener`:** Primitive SAM interface (`onBlockEnergies(sampleIndex, energies)`) delivering reusable energy arrays to downstream receivers without heap allocation.
- **`BlockDetector`:** Streaming audio ingestion wrapper with an internal ring buffer. Ingests arbitrary chunk sizes (e.g. from `AudioRecord`), delivering tone energy vectors at a configured `hopSize` interval (e.g. $N$ or $N/4$) with 0 heap allocations per emit.

---

## 2. Experimental Measurements & Performance

### 2.1 Measured Crosstalk Matrices ($16 \times 16$ Leakage)
Synthesized each of the 16 data tones alone with raised-cosine windowing across all bands, sample rates, and speeds, measuring maximum off-diagonal leakage into adjacent filter bins:

| Band | Sample Rate | Speed Preset | Worst Leakage | Required Bound | Margin |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Stealth** | 48,000 Hz | **Robust** | **-62.86 dB** | $\le -25.0$ dB | $+37.86$ dB |
| **Stealth** | 48,000 Hz | **Normal** | **-44.57 dB** | $\le -25.0$ dB | $+19.57$ dB |
| **Stealth** | 48,000 Hz | **Fast** | **-48.58 dB** | $\le -20.0$ dB | $+28.58$ dB |
| **Stealth** | 44,100 Hz | **Robust** | **-62.84 dB** | $\le -25.0$ dB | $+37.84$ dB |
| **Stealth** | 44,100 Hz | **Normal** | **-44.58 dB** | $\le -25.0$ dB | $+19.58$ dB |
| **Stealth** | 44,100 Hz | **Fast** | **-48.63 dB** | $\le -20.0$ dB | $+28.63$ dB |
| **Retro** | 48,000 Hz | **Robust** | **-56.71 dB** | $\le -25.0$ dB | $+31.71$ dB |
| **Retro** | 48,000 Hz | **Normal** | **-43.40 dB** | $\le -25.0$ dB | $+18.40$ dB |
| **Retro** | 48,000 Hz | **Fast** | **-31.04 dB** | $\le -20.0$ dB | $+11.04$ dB |
| **Retro** | 44,100 Hz | **Robust** | **-56.71 dB** | $\le -25.0$ dB | $+31.71$ dB |
| **Retro** | 44,100 Hz | **Normal** | **-43.43 dB** | $\le -25.0$ dB | $+18.43$ dB |
| **Retro** | 44,100 Hz | **Fast** | **-31.05 dB** | $\le -20.0$ dB | $+11.05$ dB |

*Observation: Every configuration comfortably beats the roadmap requirements with substantial margin ($+11$ dB to $+38$ dB margin).*

### 2.2 Amplitude Linearity Across 60 dB Dynamic Range
Evaluated pure tone at $18,500$ Hz across variable input amplitudes:
- -6 dBFS ($A = 0.501$): Measured $0.2512$ ($0.0\%$ error vs $A^2$)
- -20 dBFS ($A = 0.100$): Measured $0.0100$ ($0.0\%$ error)
- -40 dBFS ($A = 0.010$): Measured $0.000100$ ($0.0\%$ error)
- -60 dBFS ($A = 0.001$): Measured $0.00000100$ ($0.0\%$ error)

### 2.3 Frequency Tolerance ($\pm 30$ Hz Offset)
Evaluated at 10 ms analysis window ($480$ samples at 48 kHz):
- **Stealth Band ($18,350$ Hz):**
  - $+30$ Hz offset: power ratio = **$0.890$** ($-0.51$ dB drop, exceeds $0.80$ / $-0.97$ dB requirement)
  - $-30$ Hz offset: power ratio = **$0.890$** ($-0.51$ dB drop)
- **Retro Band ($2,125$ Hz):**
  - $+30$ Hz offset: power ratio = **$0.890$** ($-0.51$ dB drop)
  - $-30$ Hz offset: power ratio = **$0.890$** ($-0.51$ dB drop)

### 2.4 Micro-Benchmark (Full 18-Filter Bank)
Evaluated continuous processing throughput on 18 simultaneous tone filters (16 data + 2 sync tones):
```text
GoertzelBank Throughput:
- Processing Speed: 15,449,211 samples/sec
- Data Throughput: 58.93 MB/s
- Realtime Factor: 321.86x realtime audio (at 48 kHz)
```
*Hot-path analysis is 320x faster than realtime stream ingestion on host hardware.*

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Test Suite Execution (`./gradlew :modem:test`)
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:compileTestKotlin
> Task :modem:test

BUILD SUCCESSFUL in 17s
8 actionable tasks: 4 executed, 4 up-to-date
```

### 3.2 Full Project Check (`./gradlew check`)
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:verifyChirpRules
> Task :app:lintDevDebug
> Task :app:check

BUILD SUCCESSFUL in 18s
131 actionable tasks: 28 executed, 103 up-to-date
```

---

## 4. Next Step
Proceed to **Step 19: Noise floor tracker and SNR** (exponential moving average tracker, dynamic thresholding, SNR estimation, impulse rejection).
