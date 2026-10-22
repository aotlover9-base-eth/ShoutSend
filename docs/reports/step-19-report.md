# Step 19 Report: Dynamic Noise Floor Tracker and SNR Estimator

**Date:** 2026-10-23
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 19 implements the dynamic noise floor tracking, detection thresholding, and SNR estimation engine in `:modem` (`app.shoutsend.modem.detector`) conforming to Rules 21, 22 (zero allocations on update path), 23, and 27. It allows the modem receiver to adapt its sensitivity dynamically between quiet rooms and noisy public environments without suffering from threshold flutter.

Key components delivered:
- **`NoiseTrackerParams`:** Configuration specifying adaptation time constants, detection margins, and threshold floors. Converts time constants directly into discrete smoothing factors ($\alpha = 1 - e^{-\Delta t / \tau}$) based on hop rate.
- **`NoiseFloorTracker`:** Per-tone tracker implementing asymmetric exponential moving average smoothing:
  - Upward change (attack): slow time constant $\tau_{\text{attack}} = 1.5$s prevents transient noise/speech bursts from desensitizing the receiver.
  - Downward change (decay): fast time constant $\tau_{\text{decay}} = 0.25$s quickly tracks down to quiet acoustic troughs.
  - Freezing lock (`freeze()` / `unfreeze()`): locks floor estimation during active transmissions to prevent received data symbols from being integrated into the noise floor.
- **Dynamic Detection Thresholds:** $\text{threshold}(k) = \max(\text{floor}[k] \cdot 10^{\text{margin}/10}, \text{minFloorPower} \cdot 10^{\text{margin}/10})$ with default margin of $+6.0$ dB and absolute floor limit of $-70$ dBFS ($10^{-7}$).
- **Demodulator Confidence:** Evaluates continuous $[0.0, 1.0]$ confidence combining winner-to-runner separation and SNR margin for downstream Reed-Solomon erasure marking.
- **Broadband Ambient Metering:** Exposes `ambientLevelDbfs()` for UI noise diagnostics.

---

## 2. Experimental Measurements & Verification

### 2.1 Noise Floor Step Adaptation Times (Measured Table)
Evaluated with step changes from a $-40$ dBFS ($1.00 \times 10^{-4}$) baseline to reach within $1.0$ dB of target level at $10$ ms hop rate:

| Step Size | Direction | Target Power | Target dBFS | Time to Within 1 dB |
| :---      | :---      | :---         | :---        | :---                |
| **+20 dB** | Rise (Slow) | $1.00 \times 10^{-2}$ | -20.0 dBFS | **2.360 s** |
| **+10 dB** | Rise (Slow) | $1.00 \times 10^{-3}$ | -30.0 dBFS | **2.220 s** |
| **-10 dB** | Fall (Fast) | $1.00 \times 10^{-5}$ | -50.0 dBFS | **0.890 s** |
| **-20 dB** | Fall (Fast) | $1.00 \times 10^{-6}$ | -60.0 dBFS | **1.490 s** |

*Analysis: Downward adaptation (e.g. $-10$ dB in $0.890$s) is $2.5\times$ faster than upward adaptation ($+10$ dB in $2.220$s), confirming that the tracker actively hugs quiet troughs.*

### 2.2 White Noise Floor Stability
- Fed 1000 hops (10 seconds) of fluctuating white noise at $-40$ dBFS:
- Mean tracked floor across all 16 tones: $8.53 \times 10^{-5}$
- Standard deviation across tones: $8.23 \times 10^{-7}$
- Relative standard deviation: **$0.0096$ ($0.96\%$)**, well within the $< 3.0\%$ stability bound.

### 2.3 Acquisition Freezing Lock Proof
- Simulated 3-second loud tone burst ($A = 0.8$, power $0.64$, $+18$ dB above baseline):
- **When FROZEN:** Noise floor moved by **$0.00$ dB** (remained identical to initial $1.00 \times 10^{-4}$).
- **When NOT FROZEN:** Noise floor rose by **$>10\times$ ($+10$ dB)**, blinding the receiver to subsequent weaker symbols.

### 2.4 Digital Silence Clamping
- Fed 1000 hops of digital silence ($0.0f$):
- Tracker clamped strictly at `minFloorPower` ($1.00 \times 10^{-7} = -70$ dBFS).
- Zero NaNs, zero Infinities across all tones and ambient level metering.

### 2.5 Zero Heap Allocation Benchmark
- Evaluated 10,000 tracker updates (100 seconds of continuous stream updates):
- Measured allocated bytes: **$0$ bytes**.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Test Suite Execution (`./gradlew :modem:test`)
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:compileTestKotlin
> Task :modem:test

BUILD SUCCESSFUL in 14s
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

BUILD SUCCESSFUL in 15s
131 actionable tasks: 28 executed, 103 up-to-date
```

---

## 4. Next Step
Proceed to **Step 20: Preamble detection and symbol synchronization** (cross-correlation on alternating S0/S1 preamble, sync word correlation peak, symbol timing lock, confidence score).
