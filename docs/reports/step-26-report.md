# Step 26 Report: Manifest, Permissions Logic, and Audio Configuration

**Date:** 2026-10-23
**Status:** Complete & Verified
**Author:** Antigravity (Leader & Worker 2)

---

## 1. Executive Summary
Step 26 establishes the Android application permissions architecture, microphone permission state modeling, and hardware audio constants conforming to Rules 4, 24 (strict offline permissions), and 25.

Key deliverables:
- **Manifest Permissions (`AndroidManifest.xml`)**:
  - `android.permission.RECORD_AUDIO`: Runtime permission requested upon entering Receive / Calibration modes.
  - `android.permission.VIBRATE`: Install-time permission for acoustic haptic feedback.
  - `<uses-feature android:name="android.hardware.microphone" android:required="true" />`: Declares microphone hardware dependency.
  - Zero `INTERNET` or `ACCESS_NETWORK_STATE` permissions in main or merged manifests.
- **`AudioConfig` (`app.shoutsend.core.audio`)**:
  - `PREFERRED_SAMPLE_RATES`: `[48000, 44100]` in priority order.
  - `CHANNEL_CONFIG_IN`: Mono (`AudioFormat.CHANNEL_IN_MONO`).
  - `CHANNEL_CONFIG_OUT`: Mono (`AudioFormat.CHANNEL_OUT_MONO`).
  - `ENCODING_PCM`: 16-bit linear PCM (`AudioFormat.ENCODING_PCM_16BIT`).
  - `CHUNK_DURATION_MS`: 20 ms (960 samples at 48 kHz, 882 samples at 44.1 kHz).
  - `RING_BUFFER_DURATION_SEC`: 2 seconds (96,000 samples at 48 kHz).
- **`MicPermissionState` (`app.shoutsend.core.audio.permissions`)**:
  - Typed state enum: `Granted`, `CanAsk`, `ShouldExplain`, `PermanentlyDenied`.
  - Pure resolution function: `resolve(isGranted, shouldShowRationale, hasRequestedBefore)`.
- **`MicPermissionRequestedProvider`**: Interface decoupling permission request persistence from audio code.
- **`PermissionRepository` / `AndroidPermissionRepository`**: Exposes reactive `StateFlow<MicPermissionState>` with `refresh()` for lifecycle resumption.
- **Settings Intent Helper**: `createAppSettingsIntent(context)` linking to `ACTION_APPLICATION_DETAILS_SETTINGS`.

---

## 2. Unit Testing & Verification Results

### 2.1 Permission Resolution Truth Table (8 combinations)
- `isGranted = true` -> `Granted` (regardless of other flags) [4 cases].
- `isGranted = false, shouldShowRationale = true` -> `ShouldExplain` [2 cases].
- `isGranted = false, shouldShowRationale = false, hasRequestedBefore = true` -> `PermanentlyDenied`.
- `isGranted = false, shouldShowRationale = false, hasRequestedBefore = false` -> `CanAsk`.
- **Result:** 100% pass across all 8 branches.

### 2.2 Audio Configuration Verification
- Verified sample rate priority order (48000, 44100).
- Verified chunk sample calculations: 48,000 Hz -> 960 samples; 44,100 Hz -> 882 samples.
- Verified ring buffer capacity: 48,000 Hz -> 96,000 samples.

### 2.3 Verification Command Output

```text
> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:testDevDebugUnitTest
BUILD SUCCESSFUL in 15s
30 actionable tasks: 2 executed, 28 up-to-date
```
Merged manifest permission verification confirmed:
- `android.permission.RECORD_AUDIO`
- `android.permission.VIBRATE`
- `app.shoutsend.dev.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
- Zero forbidden permissions detected.
