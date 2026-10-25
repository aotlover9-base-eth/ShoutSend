# Step 36 Report: Onboarding and Permission Flow

**Date**: 2026-10-26  
**Author**: Antigravity Assistant  
**Status**: Completed  
**Branch**: `main`

---

## 1. Executive Summary

Step 36 delivers the complete first-run onboarding sequence and reusable microphone permission UI. Built using Jetpack Compose's `HorizontalPager`, the onboarding experience guides users through four key aspects of acoustic data transfer: (1) core concept, (2) 3-step emit/listen/verify mechanics, (3) operational guidance (media volume, range, and privacy), and (4) permission explanation emphasizing that audio processing is entirely local and offline.

All illustrations are drawn with Compose `Canvas` primitives and design system tokens, avoiding third-party animation libraries or raster assets. They respect `LocalReduceMotion`, switching smoothly to static geometries. For subsequent screens (`Receive`, `Calibration`), a shared `MicPermissionCard` standardizes permission rationale, request triggers, and system settings deep-linking.

---

## 2. Key Architectural Deliverables

### A. Four-Page Guided Onboarding (`OnboardingScreen.kt`)
- **Page 1 (Talk in Sound)**:
  - `TalkInSoundIllustration`: Concentric acoustic soundwave ripples expanding between two smartphone silhouettes.
- **Page 2 (How It Works)**:
  - `HowItWorksIllustration`: 3-node sequence representing emission, acoustic listening, and Reed-Solomon verification.
- **Page 3 (Good to Know)**:
  - `GoodToKnowIllustration`: Acoustic cone with safety shield highlighting optimal volume (70–90%), Stealth proximity (<0.5m), and privacy considerations.
- **Page 4 (Allow the Microphone)**:
  - `AllowMicrophoneIllustration`: Microphone silhouette with pulsing green privacy indicator.
  - Action buttons: "Allow Microphone" and "Not Now", transitioning to "Get Started" once granted.
- **Morphing Indicator & Quick Skip**:
  - Horizontal pill indicator expanding dynamically on the active page.
  - "Skip" option on pages 1–3 jumping directly to page 4.

### B. Reusable Microphone Permission Component (`MicPermissionCard.kt`)
- Reusable design-system card responding to `MicPermissionState`:
  - `CanAsk` & `ShouldExplain`: Presents rationale and prompts for permission.
  - `PermanentlyDenied`: Displays warning styling and launches Android application settings via `createAppSettingsIntent(context)`.
- Integrated directly into `ReceiveScreen` and `CalibrationScreen`.
- Automatically refreshed on application foreground return via `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)`.

### C. State Machine & Persistence (`OnboardingViewModel.kt`)
- Tracks `currentPage` (0..3) and resolves effective permission state combining `Settings.micPermissionRequested` with active system status.
- Finishing onboarding writes `onboardingCompleted = true` to DataStore settings, ensuring subsequent launches navigate directly to the primary operational shell.
- Unit test suite (`OnboardingViewModelTest.kt`) verifies page transitions, boundary clipping, skip actions, and permission resolution callbacks.

### D. Strict String Tokenization (`res/values/strings.xml`)
- All user-facing strings externalized to English string resources with zero hardcoded literals in composables.

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
BUILD SUCCESSFUL in 22s

> Task :app:testDevDebugUnitTest
BUILD SUCCESSFUL in 45s (87 tests passing)

> Task :app:testUserDebugUnitTest
BUILD SUCCESSFUL in 45s (87 tests passing)
```

---

## 4. Architectural Rules Compliance
- **Rule 2 (Accessibility)**: Screen reader content descriptions on all illustrations; touch target tokens respected.
- **Rule 6 (Single Hex File)**: Zero hex literals in onboarding code.
- **Rule 7 (No Measurement Literals)**: All dimensions consume design system tokens.
- **Rule 8 (Single Design System)**: All components consume `ChirpTheme`.
- **Rule 15 (Dev/User Separation)**: User APK clean of dev markers.
- **Rule 24 (Strict String Resources)**: Zero hardcoded text strings in composables.
- **Rule 26 (Offline / Zero Internet)**: Exactly two permissions (`RECORD_AUDIO`, `VIBRATE`). Zero internet.
