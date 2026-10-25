# Step 35 Report: App Shell, Adaptive Scaffold, and Type-Safe Navigation

**Date**: 2026-10-26  
**Author**: Antigravity Assistant  
**Status**: Completed  
**Branch**: `main`

---

## 1. Executive Summary

Step 35 establishes the foundational application scaffold and type-safe navigation framework for ShoutSend. Using Navigation Compose 2.8.3 with Kotlinx Serialization, all destinations (`Onboarding`, `Send`, `Receive`, `Vault`, `VaultDetail`, `Settings`, `Calibration`, `LinkTest`) are represented as strongly-typed, compile-time verified routes. 

The root `AppScaffold` dynamically adapts to screen geometry, rendering a bottom navigation bar (`CdNavigationBar`) on standard phones (< 600 dp) and a side navigation rail (`CdNavigationRail`) on tablets and foldables ($\ge 600$ dp). Top bar integration displays the animated `BrandMark` emblem and active `StatusPill`. To prevent accidental acoustic session termination, back navigation during an ongoing transmission or listening session intercepts the system back event with a tokenized confirmation dialog (`CdAlertDialog`). Cold launch restoration reads and updates `Settings.lastTab`.

---

## 2. Key Architectural Deliverables

### A. Type-Safe Navigation Routes (`app.shoutsend.navigation.Routes`)
- Strongly typed `@Serializable` definitions:
  - `OnboardingRoute`: First-run onboarding flow.
  - `SendRoute`: Soundwave payload transmission screen.
  - `ReceiveRoute`: Spectrogram waterfall and acoustic receiver screen.
  - `VaultRoute`: Acoustic transmission history.
  - `VaultDetailRoute(id: Long)`: Detailed payload inspection.
  - `SettingsRoute`: Acoustic preferences, theme mode, and retention limits.
  - `CalibrationRoute`: Speaker/microphone loopback test engine.
  - `LinkTestRoute`: Real-time acoustic diagnostics.
- `TopLevelDestination`: Enumerates bottom-bar / rail items (`SEND`, `RECEIVE`, `VAULT`) with metadata and fallback resolution.

### B. Adaptive Scaffold & Component Wrappers
- **`CdNavigationBar.kt` & `CdNavigationRail.kt`**:
  - Implements Material 3 navigation bars and rails locked to `ChirpTheme` color and elevation tokens.
- **`AppScaffold.kt`**:
  - Automatically queries `LocalConfiguration.current.screenWidthDp >= 600` to select the navigation rail or bottom bar.
  - Encapsulates edge-to-edge window insets, IME padding, and top bar action buttons.
  - Integrates `BackHandler` with `CdAlertDialog` confirmation during active acoustic sessions.

### C. Screen Architecture & ViewModel State Holders
- Implemented MVVM placeholder screens and ViewModels for all destinations:
  - `feature/send/`: `SendViewModel`, `SendScreen`
  - `feature/receive/`: `ReceiveViewModel`, `ReceiveScreen`
  - `feature/vault/`: `VaultViewModel`, `VaultScreen`, `VaultDetailScreen`
  - `feature/settings/`: `SettingsViewModel`, `SettingsScreen`
  - `feature/calibration/`: `CalibrationViewModel`, `CalibrationScreen`
  - `feature/onboarding/`: `OnboardingViewModel`, `OnboardingScreen`
  - `feature/linktest/`: `LinkTestScreen`
- Each placeholder demonstrates real Cyber Radio components (`NeonCard`, `StatusPill`, `SignalMeter`, `LevelMeter`, `ModeSelector`, `SpeedSelector`, `RadioReadout`).

### D. Motion Transitions (`ShoutSendNavHost.kt`)
- Top-level tabs employ vertical fade-through using `ChirpTheme.motion.durationMedium1` and `easingStandardDecelerate`.
- Sub-screens employ shared-axis horizontal sliding using `ChirpTheme.motion.durationMedium2`.
- Transition parameters are pre-captured in composable scope to ensure clean integration with non-composable transition lambdas.

### E. Tab Persistence & Cold Launch
- Extended `Settings` and `SettingsRepository` with `lastTab` property.
- Cold launches restore the last active operational tab (defaulting to `ReceiveRoute`) once onboarding is complete.

---

## 3. Verification & Quality Gates

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
BUILD SUCCESSFUL in 1m 50s

> Task :app:testDevDebugUnitTest
BUILD SUCCESSFUL in 53s (71 tests passing)

> Task :app:testUserDebugUnitTest
BUILD SUCCESSFUL in 53s (71 tests passing)
```

---

## 4. Architectural Rules Compliance
- **Rule 6 (Single Hex File)**: Zero hex literals in navigation or scaffold code.
- **Rule 7 (No Radii or Measurement Literals)**: All dimensions consume tokens.
- **Rule 8 (Single Design System)**: All components consume `ChirpTheme`.
- **Rule 9 (Consistent Wrappers)**: Scaffold, bars, buttons, and dialogs use `Cd*` wrappers.
- **Rule 15 (Dev/User Separation)**: Floating dev handle operates via `DevTools` seam above scaffold in dev only.
- **Rule 17 (Pure JVM Modem)**: `:modem` remains 100% pure JVM.
- **Rule 26 (Offline / Zero Internet)**: No network permissions requested or utilized.
