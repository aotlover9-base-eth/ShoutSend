# Step 33 Report: Modem Config Provider (Dev Override Seam)

**Date**: 2026-10-25  
**Author**: Antigravity Assistant  
**Status**: Completed  
**Branch**: `main`

---

## 1. Executive Summary

Step 33 formalizes the runtime architectural seam separating physical acoustic DSP parameters and device hardware overrides from immutable user production code. All physical DSP numbers (output level, lead-in/tail silence, symbol and ramp durations per preset, erasure confidence threshold, detection margins, sync thresholds, marker miss limit, noise tracker time constants, spectrum FFT size, and dynamic dBFS range) are aggregated into a single validated `ModemTuning` data class within `:modem`. 

In `:app`, the `ModemConfigProvider` interface bundles active user preferences (`Settings`) with physical `ModemTuning` and an optional hardware audio source override (`audioSourceOverride`). In accordance with **Rule 15** and **Rule 28**, `FixedModemConfigProvider` in `src/user` guarantees immutable default tuning and zero audio source overrides with zero dev tool leakage, while `OverridableModemConfigProvider` in `src/dev` provides the live runtime hook for the Step 50 DSP Lab. All sessions (`TransmitSession`, `ReceiveSession`), the calibration engine (`CalibrationRunner`), and the hardware receiver (`DefaultAcousticReceiver`) dynamically query the provider at session startup.

---

## 2. Key Architectural Deliverables

### A. Pure JVM Modem Tuning Hierarchy (`:modem`)
- **`ModemTuning.kt`**:
  - Implements complete parameter bundle with defaults identical to hardcoded constants tuned in Step 23.
  - Implements `validate(): ModemTuningValidationResult` strictly enforcing valid physical operating envelopes:
    - `outputLevel in 0.01f..1.0f`
    - `leadInSilenceMs in 50..2000`, `tailSilenceMs in 50..2000`
    - `fastDurationMs`, `normalDurationMs`, `robustDurationMs` $\ge 10$ ms
    - `fastRampMs`, `normalRampMs`, `robustRampMs` $\ge 1$ ms and $< \text{duration} / 2$
    - `erasureThreshold in 0.05..0.95`
    - `detectionMarginDb in 1.0..24.0`, `absoluteDetectionFloorDbfs in -120.0..-20.0`
    - `minPreambleSlots in 2..16`, `preambleSnrThresholdDb in 1.0..24.0`, `markerMissLimit in 0..8`
    - `noiseAttackSec > 0`, `noiseDecaySec > 0`
    - `fftSize` power of 2 in `128..8192`
    - `spectrumMinDbfs < spectrumMaxDbfs`
- **Component Refactoring**:
  - `TransmissionBuilder`, `SyncDetector`, `ReceiverPipeline`, `SpectrumAnalyzer`, and `NoiseFloorTracker` now consume `ModemTuning` parameters on invocation.
- **Waveform Invariance Verification**:
  - `ModemTuningTest.kt` verifies that generating a transmission with `ModemTuning.DEFAULT` produces a byte-for-byte, bit-identical PCM buffer and SHA-256 hash matching default parameters (`7db9cf6fb7d4022dbf37b98fecaa1c17290ebce2fe8d8442ec9103c800a73d32`).

### B. Config Provider Seam (`:app`)
- **`ModemConfig.kt`**:
  - Bundles `Band`, `Speed`, `Repeats`, `Scheme`, `rsProfileId`, `ModemTuning`, and `audioSourceOverride`.
  - Exposes `val profile: ModemProfile` property for modulation assembly.
- **`ModemConfigProvider.kt`**:
  - `interface ModemConfigProvider { val config: StateFlow<ModemConfig> }`
- **`FixedModemConfigProvider.kt` (`src/user`)**:
  - Maps `SettingsRepository.settings` to `ModemConfig` with immutable `ModemTuning.DEFAULT` and `audioSourceOverride = null`.
- **`OverridableModemConfigProvider.kt` (`src/dev`)**:
  - Combines `SettingsRepository.settings`, `customTuning`, and `customAudioSourceOverride`.
  - Exposes `updateTuning()`, `updateAudioSourceOverride()`, and `resetTuning()`.

### C. Session & Transceiver Integration
- **`DefaultTransmitSession`**: Queries `configProvider?.config?.value` dynamically on each `send()` invocation, resolving `effectiveProfile` and `effectiveTuning` to pass to `TransmissionBuilder.build()`.
- **`DefaultReceiveSession`**: Queries `configProvider?.config?.value` dynamically on each `start()` invocation, resolving `effectiveBand` and `effectiveTuning` to pass to `receiver.start()`.
- **`DefaultAcousticReceiver`**: Uses `effectiveTuning` for `SpectrumAnalyzer` and `ReceiverPipeline`.
- **`CalibrationRunner`**: Resolves `effectiveTuning` from provider before initiating loopback calibration.

---

## 3. Verification & Quality Gates

### A. Modem Waveform & Test Verification
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test
BUILD SUCCESSFUL in 1m 44s
8 actionable tasks: 8 executed
```
- Bit-identical transmission SHA-256 hash verified:
  `7db9cf6fb7d4022dbf37b98fecaa1c17290ebce2fe8d8442ec9103c800a73d32`

### B. Unit Test Suites
- `ModemConfigProviderTest`: 4 tests (provider update propagation, transmit session dynamic pick-up, receive session dynamic pick-up, parameter validation rejecting dangerous values).
- `OverridableModemConfigProviderTest`: 3 tests (default emission, dev overrides and resets, settings updates).
- `FixedModemConfigProviderTest`: 2 tests (immutable defaults, settings updates, null audioSourceOverride).
```text
> Task :app:testDevDebugUnitTest
BUILD SUCCESSFUL

> Task :app:testUserDebugUnitTest
BUILD SUCCESSFUL
```

### C. Architectural Guards & User APK Cleanliness
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:verifyChirpRules
BUILD SUCCESSFUL
```
- Dev marker `OverridableModemConfigProvider` registered in `config/dev-markers.txt` and verified 100% absent from User APK DEX.

---

## 4. Conclusion & Next Step

Phase D (Android Audio & Data Layer) is now fully complete (Steps 26–33). The architecture cleanly separates user and developer configurations while maintaining strict zero-allocation performance and offline privacy.

-> Next: **Phase E: The UI (Step 34: Design system tokens & colormap)**.
