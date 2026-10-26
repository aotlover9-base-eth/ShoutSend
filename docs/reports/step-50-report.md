# Step 50 Report: Developer Panel — DSP Lab

**Date:** 2026-10-27  
**Status:** Complete & Verified  
**Author:** Antigravity (Leader)  

---

## 1. Executive Summary & What Was Implemented
Step 50 delivers a specialized diagnostic playground for acoustic digital signal processing directly inside the developer panel, enabling live Goertzel filter bank inspection, synthetic tone injection, and noise floor evaluation while preserving 100% User APK cleanliness:

- **DSP Lab ViewModel (`app/src/dev/java/app/shoutsend/core/devtools/DspLabViewModel.kt`)**:
  - Implements synthetic tone generation for all 16 MFSK data tone frequencies across `Band.RETRO` (1.5 kHz – 3.375 kHz) and `Band.STEALTH` (17.6 kHz – 19.85 kHz).
  - Drives `GoertzelBank` to compute exact normalized energies across symbol blocks matching `Speed` durations (`Timing.symbolSamples`).
  - Evaluates signal-to-noise ratio (SNR) in dB and noise floor dBFS with optional simulated additive Gaussian noise injection (+12 dB).
- **DSP Lab UI (`app/src/dev/java/app/shoutsend/core/devtools/DspLabScreen.kt`)**:
  - 16-bar MFSK Power Spectrum visualizer displaying real-time Goertzel power bins with peak tone detection highlights (`colors.signalPeak`).
  - Real-time demodulator telemetry card reporting detected frequency in Hz, calculated SNR in dB, and state messages.
  - 4x4 synthetic tone injection keypad (T0 through T15) for immediate acoustic testing without external transducers.
  - Additive noise toggle switch and reset controls.
- **Developer Panel Integration (`DeveloperPanel.kt` & `DevPanelViewModel.kt`)**:
  - Added `DevPanelTab.DSP_LAB` tab selector ("DSP Lab") alongside Colors and Shapes in the bottom sheet developer panel.
- **Strict Flavor Cleanliness & Zero Leakage**:
  - All DSP Lab components, ViewModels, and unit tests are contained strictly in `src/dev` and `src/testDev`.
  - `verifyUserApkClean` verified 0 occurrences of the `DspLab` marker in the User APK DEX.
- **Unit Tests**:
  - `DspLabViewModelTest.kt` in `src/testDev`: 6 tests verifying initial state tone frequencies, band switching to Stealth, speed updates, Goertzel power accuracy, noise injection, and meter reset.

---

## 2. Automated Verification Results
Command executed:
```bash
./gradlew check assembleDevDebug assembleUserDebug lintDevDebug lintUserDebug
```

Key output extracts:
- `checkNoHardcodedStyle`: All sources comply with tokenization rules.
- `verifyPermissions`: Merged manifests comply with strict offline permissions policy (only `RECORD_AUDIO` and `VIBRATE`).
- `verifyUserApkClean`: User APK app-user-debug.apk is 100% clean of dev tools (verified 0 `DspLab` markers).
- `checkModemPurity`: :modem is 100% pure Kotlin JVM (0 Android imports).
- `testDevDebugUnitTest`: 100% passed (including 6 `DspLabViewModelTest` tests).
- `testUserDebugUnitTest`: 100% passed (pure user flavor).
- `lintDevDebug` and `lintUserDebug`: Passed with 0 errors.

---

## 3. Files Created or Modified
- `app/src/dev/java/app/shoutsend/core/devtools/DspLabViewModel.kt` (created with tone synthesis and Goertzel inspection logic)
- `app/src/dev/java/app/shoutsend/core/devtools/DspLabScreen.kt` (created with 16-bar meter, telemetry, and injection pad)
- `app/src/dev/java/app/shoutsend/core/devtools/DevPanelViewModel.kt` (added DSP_LAB tab enum and DspLabViewModel instance)
- `app/src/dev/java/app/shoutsend/core/devtools/DeveloperPanel.kt` (added DSP Lab filter chip and content rendering)
- `app/src/testDev/java/app/shoutsend/core/devtools/DspLabViewModelTest.kt` (created with 6 unit tests)
- `docs/reports/step-50-report.md` (Step 50 verification report)

---

## 4. Security & Privacy Audit
- **Zero Leak in User Flavor**: Dev code is completely absent from the shipping User APK.
- **Permissions**: Zero additional permissions. Strictly `RECORD_AUDIO` and `VIBRATE`.

---

## 5. Checks NOT Run
- None. Full test suite, lint, style token verification, permission checker, dev tool clean checker, and multi-flavor compilation were executed.

---

## 6. Deviations from Rules
- None.

---

## 7. OWNER RUNS
- Manual evaluation of the DSP Lab UI in dev build by dragging the floating handle, tapping T7 on Retro and T12 on Stealth, and confirming meter peaks.
