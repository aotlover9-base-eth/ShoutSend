# ShoutSend Decision Log

## 2026-10-22 - Step 1: Project Setup Decisions

### DEC-001: Toolchain and Android SDK Versions
- **Decision**: Set `compileSdk = 35`, `targetSdk = 35`, `minSdk = 31`.
- **Reason**: SDK 35 is installed and stable. Android 12 (SDK 31) provides full native support for Dynamic Color and low-latency audio APIs.
- **Java Toolchain**: Java 17 compatibility configured in Gradle.

### DEC-002: Version Catalog and Initial Dependencies
- **Decision**: Added Gradle Version Catalog (`gradle/libs.versions.toml`) with:
  - Compose BOM `2024.06.00`
  - Kotlin `2.0.0`
  - AGP `8.5.2`
  - AndroidX Core KTX `1.13.1`
  - Activity Compose `1.9.0`
  - Lifecycle `2.8.2` (Runtime KTX, Runtime Compose, ViewModel Compose)
  - Navigation Compose `2.7.7`
  - Kotlinx Coroutines `1.8.1` (Core, Android, Test)
  - Kotlinx Serialization JSON `1.6.3`
  - DataStore Preferences `1.1.1`
  - JUnit `4.13.2`
- **Reason**: Standardized dependencies approved in Section 7 of roadmap. No Room or Hilt added yet.

### DEC-003: Multi-Flavor Seam Architecture (Rule 15)
- **Decision**: Flavors configured on dimension `audience`: `dev` and `user`.
  - Application ID: `app.shoutsend.dev` (dev) and `app.shoutsend` (user).
  - App Name: `ShoutSend (Dev)` vs `ShoutSend`.
  - DevTools interface in `src/main`, bound to `DevToolsImpl` in `src/dev` and `NoOpDevTools` in `src/user`.
- **Reason**: Guarantees zero developer tooling code or leakages in the production User APK.

### DEC-004: Zero Permissions in Initial Scaffolding
- **Decision**: Manifest contains zero permissions at Step 1.
- **Reason**: Permissions (`RECORD_AUDIO` and `VIBRATE`) will be introduced strictly in Step 26 with proper rationale.

## 2026-10-22 - Step 2: Design System Core Decisions

### DEC-005: Token Model and Single-Hex Architecture (Rules 6 & 7)
- **Decision**: Created 9 immutable token groups (`ColorTokens`, `ShapeTokens`, `TypographyTokens`, `SpacingTokens`, `StrokeTokens`, `SizeTokens`, `ElevationTokens`, `OpacityTokens`, `MotionTokens`) unified under `DesignTokens`.
- **Constraint**: `DefaultTokens.kt` is established as the sole exception file containing hex literals and raw dimension numbers.
- **Enforcement**: Automated regex verification scripts and unit tests guarantee zero raw hex or dp/sp literals exist in any UI file outside `DefaultTokens.kt`.

### DEC-006: Cyber Radio Palette Selection and Contrast Validation
- **Decision**: Cyber Radio Dark selected as the default palette with `#0B0E14` dark blue-gray surface, `#00F0FF` cyan primary, `#00FF66` neon secondary, `#FFB300` amber tertiary, and high-contrast `#410002` error text over `#FF5449`.
- **Contrast**: Every key pair verified against WCAG AA standards (>= 4.5:1), with primary, secondary, background, and surface exceeding 9.3:1 to 15.5:1.
- **Fallback**: System dynamic color supported for Android 12+ (SDK 31+) with graceful fallback to Cyber Radio palettes.

### DEC-007: Material 3 Component Wrappers (`Cd*`)
- **Decision**: Wrapped 12 core Material 3 components (`CdButton`, `CdIconButton`, `CdCard`, `CdTextField`, `CdDialog`, `CdBottomSheet`, `CdTopBar`, `CdChip`, `CdSwitch`, `CdSnackbarHost`, `CdDivider`, `CdScaffold`) under `app.shoutsend.core.designsystem.components`.
- **Design**: All wrappers bind default styles, colors, shapes, and elevations strictly to `ChirpTheme` tokens.

### DEC-008: Previews Isolated to Developer Source Set (`src/dev`)
- **Decision**: All Compose `@Preview` composables placed strictly in `app/src/dev/java/app/shoutsend/core/designsystem/previews/`.
- **Reason**: Maintains production `user` flavor cleanliness per Rule 15.

## 2026-10-22 - Step 3: Token State and Persistence Decisions

### DEC-009: Sparse Token Overrides Model
- **Decision**: Modeled overrides in `TokenOverrides` as sparse maps (`colors: Map<String, Long>`, `radii: Map<String, Float>`).
- **Reason**: Guarantees that baseline token updates in `DefaultTokens.kt` do not get masked or overwritten unless the developer explicitly customized that specific role.
- **Validation**: Strict validation rejects unknown role keys and out-of-range radius values (< 0dp or > 100dp).

### DEC-010: Debounced DataStore in `src/dev` Flavor Seam (Rule 15)
- **Decision**: Token persistence backed by Preferences DataStore with a 300ms debounce loop exists exclusively in `src/dev`. The `user` flavor binds `NoOpTokenOverridesRepository`.
- **Reason**: Guarantees zero runtime persistence overhead or developer class leakages in the production User APK.
- **Verification**: Verified via DEX inspection of `app-user-debug.apk` showing 0 DevDataStore classes.

### DEC-011: Live Activity Recomposition Without Restart (Rule 11)
- **Decision**: `ThemeController` exposes `StateFlow<DesignTokens>` collected directly in `MainActivity.kt` with `collectAsStateWithLifecycle()`.
- **Reason**: Token changes instantly recompute the theme and recompose the UI hierarchy within the current frame without restarting the Activity.

## 2026-10-22 - Step 4: Developer Panel Decisions

### DEC-012: Developer Panel UI Built Exclusively With Design System Wrappers (Rule 9)
- **Decision**: The entire Developer Panel (`DeveloperPanel.kt`) and its floating handle (`FloatingDevHandle.kt`) are composed using `CdModalBottomSheet`, `CdButton`, `CdCard`, `CdTextField`, `CdFilterChip`, `CdIconButton`, `CdAlertDialog`, etc.
- **Reason**: Maintains visual consistency and enforces design-system discipline even within developer tooling.

### DEC-013: Pure Kotlin HSB/HSV Color Transformation
- **Decision**: Implemented HSB to RGB and RGB to HSB converters directly in Kotlin (`ColorUtils.kt`) rather than using Android's `android.graphics.Color.colorToHSV`.
- **Reason**: Guarantees fast, robust, 100% JVM-executable unit tests without requiring Android SDK mockers or Robolectric.

### DEC-014: Theme-Safe Escape Hatch Readability Guard
- **Decision**: Styled the "Reset All" action using un-overridable default tokens (`DefaultTokens.CyberRadioDark.errorContainer` and `onErrorContainer`).
- **Reason**: Protects developers from being locked out if they accidentally set background and text colors to the same value (e.g. black-on-black).

## 2026-10-22 - Step 5: Style Guard and User APK Purity Decisions

### DEC-015: BuildSrc Kotlin Implementation for Architectural Guards
- **Decision**: Implemented `StyleGuard`, `ApkPurityGuard`, and `PermissionsGuard` as small, pure Kotlin classes with JUnit tests inside `buildSrc`.
- **Reason**: Guarantees zero external lint-rule library dependencies while providing instant Gradle task integration and unit testability for architectural invariants.

### DEC-016: Style Guard Tokenization Enforcement Policy & Allowlist (Rules 6 & 7)
- **Decision**: `StyleGuard` scans all `.kt` and `.xml` source files under `app/src` and fails if:
  1. Raw hex colors or `Color(0x...)` exist outside `DefaultTokens.kt`.
  2. Named color constants (`Color.Red`, etc.) are used instead of theme tokens.
  3. Numeric literals are passed to `dp`, `sp`, or shape constructors outside `core/designsystem`.
- **Exceptions**: Tracked strictly in `config/style-allowlist.txt`. Currently only `ic_launcher_background.xml` (standard Android adaptive launcher vector) is permitted.

### DEC-017: User APK Dex Inspection & Purity Marker Policy (Rule 15)
- **Decision**: `verifyUserApkClean` unzips the assembled User APK and verifies that 0 markers from `config/dev-markers.txt` appear in DEX strings or AndroidManifest.
- **Verification**: Verified and proven by running against the Developer APK, which successfully failed with 6 detected markers.

### DEC-018: Strict Manifest Permissions Policy (Rule 24 & Rule 26)
- **Decision**: `verifyPermissions` parses the merged manifests of all flavors and enforces that the permission set contains only approved entries (`RECORD_AUDIO`, `VIBRATE`, and dynamic receiver permission).
- **Rule**: Hard-fails on `INTERNET` and `ACCESS_NETWORK_STATE`, ensuring 100% offline acoustic operation.

## 2026-10-22 - Step 6: :modem Module Scaffold Decisions

### DEC-019: Pure Kotlin JVM Architecture for :modem (Rule 21 & Rule 27)
- **Decision**: Created `:modem` as a pure Kotlin JVM library without any Android SDK, NDK, or third-party audio/DSP dependencies.
- **Reason**: Enables fast compilation, zero-overhead simulation, and 100% JVM-executable unit tests for the entire acoustic stack.

### DEC-020: Explicit API Mode (`explicitApi()`) (Rule 23)
- **Decision**: Configured Kotlin `explicitApi()` in `modem/build.gradle.kts`.
- **Reason**: Forces all exposed types and methods in `:modem` to declare explicit visibility (`public`, `internal`, `private`) and explicit return types, ensuring a disciplined and leak-proof API boundary.

### DEC-021: Automated Modem Purity Guard (`checkModemPurity`) (Rule 22)
- **Decision**: Implemented `ModemPurityGuard` in `buildSrc` to scan `:modem` source files for any `import android.*` or `import androidx.*`.
- **Reason**: Permanently guards against accidental leaks of Android framework code into the DSP engine.

## 2026-10-22 - Step 7: Acoustic Profiles & Physical Constants Decisions

### DEC-022: Physical Tone Plans and Frequency Allocation
- **Decision**: Implemented exact frequency grids from Protocol v1:
  - Stealth: 16 data tones from 17,600 Hz to 19,850 Hz (150 Hz steps), Sync tones S0 = 17,450 Hz, S1 = 20,000 Hz.
  - Retro: 16 data tones from 1,500 Hz to 3,375 Hz (125 Hz steps), Sync tones S0 = 1,375 Hz, S1 = 3,500 Hz.
- **Validation**: Enforced that all frequencies are below `sampleRate / 2 - 1500 Hz` (Nyquist guard band) and inter-tone spacing >= 100 Hz.

### DEC-023: Orthogonal Parity Mode Block Encoding
- **Decision**: Mode block packs scheme (2 bits), speed (2 bits), and rsProfileId (2 bits) with 2 orthogonal parity bits ($P_1 = b_7 \oplus b_5 \oplus b_3$, $P_0 = b_6 \oplus b_4 \oplus b_2$).
- **Reason**: Guarantees 100% detection of all 8 single-bit error permutations (verified across 192 test cases). The mode block is repeated once at MFSK16 Normal (30ms) speed for robustness.

### DEC-024: Zero-Sidelobe Sync Word and Non-Overlapping Frame Marker
- **Decision**: Sync word pattern selected as `[3, 11, 7, 14, 1, 9, 5, 12]` (8 distinct tones) ensuring 0 non-zero aperiodic autocorrelation matches. Frame marker selected as `[13, 2, 10, 4]` ensuring zero collision with the sync word.
