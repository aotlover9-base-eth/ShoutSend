# Step 45 Report: Acoustic Calibration and Link Test Screen

**Date:** 2026-10-26  
**Status:** Complete & Verified  
**Author:** Antigravity (Leader)  

---

## 1. Executive Summary & What Was Implemented
Step 45 delivers the hardware acoustic calibration and loopback link testing suite:

- **`CalibrationViewModel` (`app.shoutsend.feature.calibration.CalibrationViewModel`)**:
  - Injected with `CalibrationRunner`, `SettingsRepository`, and `AudioCoordinator`.
  - Coordinates loopback acoustic self-calibration lifecycle:
    - `CalibrationStage.Idle`: Ready state, restores last saved calibration from `SettingsRepository.settings.first().lastCalibration`.
    - `CalibrationStage.NoiseSampling`: 1000 ms ambient noise floor profiling.
    - `CalibrationStage.TransmittingTones(progress)`: Modulated test chirp playback through `AcousticTransmitter` while simultaneously recording loopback via `AcousticReceiver.rawAudioTap` (half-duplex architectural exception).
    - `CalibrationStage.Analyzing`: Fast FFT spectral evaluation via `CalibrationAnalyzer`.
    - `CalibrationStage.Completed(result)`: Stores result in state and serializes JSON to `SettingsRepository.setLastCalibration`.
    - `CalibrationStage.Failed(reason)`: Gracefully handles hardware or cancellation errors.
  - Supports `applyRecommendations()`: Updates `settingsRepository.setBand` and `settingsRepository.setSpeed` to optimal presets recommended by the calibration engine.
- **`CalibrationScreen` (`app.shoutsend.feature.calibration.CalibrationScreen`)**:
  - Cyber Radio UI conforming 100% to design system tokens (zero hardcoded hex, dp, sp, or radii).
  - Navigation header with accessible back button (`Icon(Icons.AutoMirrored.Filled.ArrowBack)`).
  - Ambient noise and loopback overview card with dynamic `StatusPill` and live progress bar during testing.
  - Telemetry Results Card displaying:
    - Hardware routing and volume warnings (`VOLUME_LOW`, `HEADPHONES_CONNECTED`).
    - Recommended band and speed preset pills (`RadioReadout`).
    - Frequency response signal quality meters (`SignalMeter`) for Audible Retro and Ultrasonic Stealth bands.
    - Exact minimum SNR in dB and ambient noise floor in dBFS.
    - One-tap "Apply Recommendations" button with animated checkmark confirmation.
  - Permission integration via `MicPermissionCard` when `RECORD_AUDIO` is not granted.
- **`LinkTestScreen` (`app.shoutsend.feature.linktest.LinkTestScreen`)**:
  - Token-bound acoustic channel diagnostics screen with back navigation and channel state readout.
- **Dependency Injection & Routing**:
  - `AppContainer`: Added `calibrationRunner` lazy property.
  - `MainActivity`: Wired `calibrationRunner` into `CalibrationViewModel`.
  - `ShoutSendNavHost`: Wired navigation routes and backstack transitions.
- **Unit Tests & Previews**:
  - 5 comprehensive unit tests in `CalibrationViewModelTest.kt` verifying initial load, calibration progression, result persistence, and recommendation application.
  - Dev preview in `CalibrationScreenPreview.kt` (`src/dev`).

---

## 2. Automated Verification Results
Command executed:
```bash
./gradlew check assembleDevDebug assembleUserDebug lintDevDebug lintUserDebug
```

Key output extracts:
- `checkNoHardcodedStyle`: All sources comply with tokenization rules.
- `verifyPermissions`: Merged manifests comply with strict offline permissions policy (only `RECORD_AUDIO` and `VIBRATE`).
- `verifyUserApkClean`: User APK app-user-debug.apk is 100% clean of dev tools.
- `checkModemPurity`: :modem is 100% pure Kotlin JVM (0 Android imports).
- `testDevDebugUnitTest`: 10/10 passed.
- `testUserDebugUnitTest`: Passed.
- `lintDevDebug` and `lintUserDebug`: Passed with 0 errors.
- Result: **BUILD SUCCESSFUL in 3m 57s (172 actionable tasks: 58 executed, 114 up-to-date)**.

---

## 3. Files Created or Modified
- `app/src/main/java/app/shoutsend/AppContainer.kt` (added `calibrationRunner`)
- `app/src/main/java/app/shoutsend/MainActivity.kt` (injected `calibrationRunner` into `CalibrationViewModel`)
- `app/src/main/java/app/shoutsend/core/calibration/CalibrationRunner.kt` (added injectable dispatcher for test deterministic virtual time)
- `app/src/main/java/app/shoutsend/feature/calibration/CalibrationViewModel.kt` (implemented full calibration orchestrator)
- `app/src/main/java/app/shoutsend/feature/calibration/CalibrationScreen.kt` (implemented Cyber Radio calibration UI)
- `app/src/main/java/app/shoutsend/feature/linktest/LinkTestScreen.kt` (updated design tokens and navigation)
- `app/src/main/res/values/strings.xml` (added Step 45 calibration strings)
- `app/src/dev/java/app/shoutsend/feature/calibration/CalibrationScreenPreview.kt` (dev preview)
- `app/src/test/java/app/shoutsend/feature/calibration/CalibrationViewModelTest.kt` (unit test suite)

---

## 4. Security & Privacy Audit
- **Permissions**: Zero additional permissions added. Strictly limited to `RECORD_AUDIO` and `VIBRATE`. Zero `INTERNET`.
- **Acoustic Privacy**: Loopback calibration strictly processes local synthetic test sequences and ambient noise floor; zero user voice or room conversation data is ever stored, analyzed for speech, or logged.
- **Deterministic Persistence**: Calibration results stored in encrypted DataStore preferences contain only numerical metrics (band, speed, SNR in dB, noise floor in dBFS, timestamp).

---

## 5. Checks NOT Run
- None. Full test suite, lint, style token verification, permission checker, dev tool clean checker, and multi-flavor compilation were executed.

---

## 6. Deviations from Rules
- None.

---

## 7. OWNER RUNS
- Physical acoustic loopback calibration on actual device speakers and microphone in a quiet room vs noisy environment to verify SNR calibration recommendation accuracy.
