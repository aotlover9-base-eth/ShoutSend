# ShoutSend Project Memory

## 1. Project Overview
- **Name**: ShoutSend
- **Type**: Native Android Application (Kotlin, Jetpack Compose, Material 3, MVVM)
- **Purpose**: Send text, URLs, Wi-Fi credentials, and contacts between phones using sound waves (MFSK / DTFSK, Goertzel decoding, Reed-Solomon error correction). Zero Wi-Fi, Bluetooth, internet, or pairing.
- **Min SDK**: 31 (Android 12)
- **Compile SDK**: 35
- **Target SDK**: 35
- **Package**: `app.shoutsend`

## 2. Architecture & Modules
- `:app`: Android application module with multi-flavor setup (`dev` and `user`).
- `:modem` (upcoming in Step 6): Pure Kotlin JVM module for audio synthesis, DSP, error correction, and channel simulation.

## 3. Product Flavors
- Dimension: `audience`
  - `dev`: Application ID `app.shoutsend.dev`, Name `ShoutSend (Dev)`, contains developer panel and DSP lab (`src/dev`).
  - `user`: Application ID `app.shoutsend`, Name `ShoutSend`, clean production build (`src/user`).

## 4. Package Structure
```
app/src/main/java/app/shoutsend/
  ├── core/
  │   ├── designsystem/   # Design tokens, ThemeController, component wrappers
  │   ├── audio/          # AudioTrack transmitter, AudioRecord receiver, probe
  │   ├── data/           # Room vault, Keystore encryption, DataStore settings
  │   ├── common/         # Result types, dispatchers, clock, haptics
  │   └── devtools/       # DevTools interface seam
  ├── feature/
  │   ├── onboarding/     # Intro and permission flow
  │   ├── send/           # Compose payload, soundwave ring transmitter
  │   ├── receive/        # Spectrogram waterfall, Goertzel receiver, action cards
  │   ├── vault/          # History storage and playback
  │   ├── settings/       # Audio & UI configuration
  │   └── calibration/    # Acoustic response testing
  ├── navigation/         # Type-safe Compose navigation routes
  ├── AppContainer.kt     # Manual dependency injection container
  ├── ShoutSendApp.kt     # Application class
  └── MainActivity.kt     # Edge-to-edge Compose host
app/src/dev/java/app/shoutsend/core/devtools/   # Active dev tools implementation
app/src/user/java/app/shoutsend/core/devtools/  # No-op dev tools implementation
```

## 5. Design System & Tokens
- Follows Material 3 & Material You.
- Theme: Cyber Radio dark default, optional dynamic wallpaper color toggle in Settings.
- Zero raw hex or radii literals outside token files.

## 6. Permissions
- Target: Exactly `RECORD_AUDIO` and `VIBRATE` (arriving in Step 26).
- `INTERNET` permission is strictly forbidden.
- Step 1 status: 0 permissions.

## 7. Current State
- Step 1 completed: Project setup, multi-flavor build, docs scaffolding, seam architecture verified.
- Step 2 completed: Design system core, token models, single-hex exception, Cyber Radio Dark & Light, 12 component wrappers, previews, tests passing.
- Step 3 completed: Token state and persistence, `ThemeController` StateFlow, debounced DataStore in `src/dev`, `NoOpTokenOverridesRepository` in `src/user`.
- Step 4 completed: Developer panel (dev flavor only), floating edge handle, `DeveloperPanel` bottom sheet with Colors and Shapes tabs, pure Kotlin HSV color picker, escape hatch reset guard, export/import JSON via clipboard, 0 forbidden dev strings in User APK, full unit test suite passing in both flavors.
- Step 5 completed: Architectural verification guards in `buildSrc` (`StyleGuard`, `ApkPurityGuard`, `PermissionsGuard`). Automated Gradle tasks `checkNoHardcodedStyle`, `verifyUserApkClean`, `verifyPermissions`, and `verifyChirpRules` bound to `check`. Full verification passed on User APK, and dev marker detection was proven by failure run against Dev APK. Clean manifest and strict offline zero-internet policy enforced.
