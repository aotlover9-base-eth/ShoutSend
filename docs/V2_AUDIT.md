# ShoutSend v2: Baseline Architectural & UI Audit

**Date:** 2026-10-28  
**Scope:** Step R0 Baseline Audit  
**Author:** Antigravity  

---

## 1. SDK and Dependency Versions & Documentation Discrepancies

### Real Code Versions
- **`compileSdk`**: `35` (`app/build.gradle.kts:13`)
- **`minSdk`**: `31` (`app/build.gradle.kts:17`)
- **`targetSdk`**: `35` (`app/build.gradle.kts:18`)
- **AGP (Android Gradle Plugin)**: `8.5.2` (`gradle/libs.versions.toml:2`)
- **Kotlin**: `2.0.0` (`gradle/libs.versions.toml:3`)
- **Compose Compiler**: Kotlin 2.0.0 Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`)
- **Compose BOM**: `2024.06.00` (`gradle/libs.versions.toml:4`)
- **Material 3**: Transitively resolved via Compose BOM `2024.06.00` to `androidx.compose.material3:material3:1.2.1`
- **KSP**: `2.0.0-1.0.24` (`gradle/libs.versions.toml:14`)
- **Navigation Compose**: `2.8.3` (`gradle/libs.versions.toml:10`)
- **Lifecycle / ViewModel**: `2.8.2` (`gradle/libs.versions.toml:8`)
- **Activity Compose**: `1.9.0` (`gradle/libs.versions.toml:9`)
- **Room**: `2.6.1` (`gradle/libs.versions.toml:13`)
- **DataStore**: `1.1.1` (`gradle/libs.versions.toml:7`)

### Discrepancies in `docs/RUN_SUMMARY.md` and `README.md`
1. **Target & Minimum SDK Discrepancy**:
   - `docs/RUN_SUMMARY.md` (Table row 8) states: `Target Android API: API 34 (UpsideDownCake) | Min API 26 (Android 8.0 Oreo)`.
   - **Reality in code**: `minSdk = 31` (Android 12 SnowCone), `targetSdk = 35` (Android 15 VanillaIceCream), `compileSdk = 35`.
   - `README.md` correctly has the badge `platform-Android 12+`, but `RUN_SUMMARY.md` is inaccurate.
2. **Reed-Solomon Algorithm Discrepancy**:
   - `docs/RUN_SUMMARY.md` (Section 2, Steps 8–13) states: `Reed-Solomon (63, k) forward error correction with Cauchy matrix and Galois field arithmetic.`
   - **Reality in code**: `modem/src/main/kotlin/app/shoutsend/modem/fec/` uses standard **Berlekamp-Massey with Forney algorithm and Chien search** over $GF(2^8)$. Zero Cauchy matrices exist.
   - Codeword block length $n$ is **60** (`RsProfile`: $n=60$, with $k \in \{54, 48, 40\}$), NOT 63.
3. **Permissions Summary Discrepancy**:
   - `docs/RUN_SUMMARY.md` notes 0 internet permissions (which is true for v1), but v2 Wi-Fi Direct will require the amended permission set from Rule 24 (replaced).

---

## 2. Screen & Route Inventory (Anti-Slop Audit)

| Route | Source File | Destination Purpose | Anti-Slop Violations (Emoji / Glyphs, Monospace, ALL CAPS, Neon/Glow, Raw Telemetry) |
|---|---|---|---|
| **`TopLevelDestination`** | `app/src/main/java/app/shoutsend/navigation/Routes.kt` | Bottom navigation bar / rail destinations | - Unicode glyph icons: `Routes.kt:47` (`"▲"`), `Routes.kt:48` (`"▼"`), `Routes.kt:49` (`"■"`) |
| **`AppScaffold`** | `app/src/main/java/app/shoutsend/navigation/AppScaffold.kt` | Top bar chrome and navigation scaffolding | - Unicode glyph icons: `AppScaffold.kt:147` (`"←"` back), `AppScaffold.kt:165` (`"🎛"` calibration), `AppScaffold.kt:174` (`"⚙"` settings) |
| **`OnboardingRoute`** | `app/src/main/java/app/shoutsend/feature/onboarding/OnboardingScreen.kt` | 4-page introduction and microphone permission setup | - Neon cards: `OnboardingScreen.kt:198, 237, 300, 363` (`NeonCard`)<br>- Glow styling: `glowPrimary` |
| **`SendRoute`** | `app/src/main/java/app/shoutsend/feature/send/SendScreen.kt`<br>`WifiComposer.kt`<br>`ContactComposer.kt` | Compose screen for text, URLs, Wi-Fi credentials, and contacts | - Emoji icons: `SendScreen.kt:182` (`"🔒"`), `SendScreen.kt:233` (`"👁"` / `"🙈"`), `SendScreen.kt:347` (`"🔒"`), `SendScreen.kt:391` (`"📋"`), `SendScreen.kt:422` (`"📋"`), `SendScreen.kt:526` (`"⚙"`), `WifiComposer.kt:114` (`"👁"` / `"🙈"`), `WifiComposer.kt:127` (`"📋"`)<br>- Monospace: `SendScreen.kt:299, 364` (`FontFamily.Monospace`)<br>- ALL CAPS: `SendScreen.kt:320` (`RadioReadout` uppercase label)<br>- Neon styling: `SendScreen.kt:177, 283` (`NeonCard`), `WifiComposer.kt:156` (`NeonCard`) |
| **`TransmitRoute`** | `app/src/main/java/app/shoutsend/feature/send/TransmitScreen.kt` | Audio playback visualizer with acoustic rings and progress | - Emoji icons: `TransmitScreen.kt:237` (`"✓"`)<br>- Monospace & ALL CAPS: `TransmitScreen.kt:122` (`RadioReadout`), `TransmitScreen.kt:187, 216` (`FontFamily.Monospace`)<br>- Neon styling: `TransmitScreen.kt:105` (`NeonCard`), `RingsCanvas.kt` (`glowRadius`, `glowAlpha`) |
| **`ReceiveRoute`** | `app/src/main/java/app/shoutsend/feature/receive/ReceiveScreen.kt`<br>`ReceivedPayloadCard.kt`<br>`SegmentedProgressBar.kt` | Listening waterfall spectrogram, lock indicator, and payload display | - Emoji & glyph icons: `ReceivedPayloadCard.kt:100` (`"✓"`), `ReceivedPayloadCard.kt:118` (`"★"` / `"☆"`), `ReceivedPayloadCard.kt:553, 568` (`"✓ "`), `SegmentedProgressBar.kt:189` (`"✓ "`)<br>- Raw technical telemetry numbers shown to end users: `ReceiveScreen.kt:208` (`"LOCKED (%.1f dB SNR)"`), `ReceiveScreen.kt:212` (`"AMB: %.1f dBFS"`), `ReceiveScreen.kt:216` (`"EST: %.1f dB SNR"`)<br>- Monospace & ALL CAPS: `ReceiveScreen.kt:157` (`RadioReadout`), `ReceiveScreen.kt:169, 177` (`FontFamily.Monospace`), `StatusPill.kt:50` (`"LOCKED"`), `ReceivedPayloadCard.kt:163` (`FontFamily.Monospace`)<br>- Neon & Scanlines: `ReceiveScreen.kt:130` (`NeonCard`), `ReceiveScreen.kt:144` (`ScanlineOverlay`), `ReceiveScreen.kt:186` (`WaterfallCanvas` using `colormap` and `spectrumGlow`), `ReceivedPayloadCard.kt:82, 142` (`NeonCard`), `SegmentedProgressBar.kt:164` (`NeonCard`) |
| **`VaultRoute`** | `app/src/main/java/app/shoutsend/feature/vault/VaultScreen.kt` | Transmission history list with filters and search | - Emoji & glyph icons: `VaultScreen.kt:392` (`"↑ SENT"` / `"↓ RCVD"`), `VaultScreen.kt:406` (`"🔒"`), `VaultScreen.kt:420` (`"★"` / `"☆"`)<br>- Monospace: `VaultScreen.kt:162, 394, 452, 500` (`FontFamily.Monospace`), `RadioReadout`<br>- Neon cards: `VaultScreen.kt:263, 371` (`NeonCard`) |
| **`VaultDetailRoute`** | `app/src/main/java/app/shoutsend/feature/vault/VaultDetailScreen.kt` | Detailed inspection of transmission telemetry and payload | - Emoji & glyph icons: `VaultDetailScreen.kt:233` (`"←"`), `VaultDetailScreen.kt:255` (`"★"` / `"☆"`), `VaultDetailScreen.kt:272` (`"🗑"`), `VaultDetailScreen.kt:302` (`"↑ SENT"` / `"↓ RECEIVED"`), `VaultDetailScreen.kt:701` (`"✓ "`)<br>- Raw technical numbers: `VaultDetailScreen.kt:355` (`"SNR: +14.2 dB"`), `VaultDetailScreen.kt:373` (`"Duration: 1.48s"`), `VaultDetailScreen.kt:394` (`"Profile: RETRO MFSK16"`)<br>- Monospace: `VaultDetailScreen.kt:304, 321, 338, 355, 373, 394, 429, 460, 492, 534, 588` (`FontFamily.Monospace`)<br>- Neon cards: `VaultDetailScreen.kt:97, 281, 403` (`NeonCard`) |
| **`SettingsRoute`** | `app/src/main/java/app/shoutsend/feature/settings/SettingsScreen.kt` | Grouped application settings and data wipe | - Glyph icons: `SettingsScreen.kt:191` (`"←"`)<br>- Monospace: `SettingsScreen.kt:574` (`FontFamily.Monospace`), `RadioReadout`<br>- Neon cards: `SettingsScreen.kt:205, 309, 372, 485, 555, 590` (`NeonCard`) |
| **`CalibrationRoute`** | `app/src/main/java/app/shoutsend/feature/calibration/CalibrationScreen.kt` | Acoustic link calibration and transducer probe | - Raw telemetry: Noise floor in dBFS, SNR in dB, transducer roll-off curves<br>- Monospace & Neon: `CalibrationScreen.kt:120, 170` (`NeonCard`), `CalibrationScreen.kt:217, 223` (`RadioReadout`) |
| **`LinkTestRoute`** | `app/src/main/java/app/shoutsend/feature/linktest/LinkTestScreen.kt` | Loopback link diagnostic test | - Raw telemetry: Bit Error Rate (BER), Packet Error Rate (PER)<br>- Monospace & Neon: `LinkTestScreen.kt:66` (`NeonCard`), `LinkTestScreen.kt:81` (`RadioReadout`) |

---

## 3. ViewModels Construction Audit

Every ViewModel in the current application is instantiated inside `MainActivity.setContent` using Jetpack Compose's `remember { ... }`, completely bypassing Android's `ViewModelStoreOwner`, `ViewModelProvider.Factory`, and `viewModel()` composable scope:

1. **`SendViewModel`** (`MainActivity.kt:96`):
   ```kotlin
   val sendViewModel = remember { SendViewModel(settingsRepository, transmitSession) }
   ```
2. **`TransmitViewModel`** (`MainActivity.kt:99`):
   ```kotlin
   val transmitViewModel = remember {
       TransmitViewModel(
           transmitSession = transmitSession,
           settingsRepository = settingsRepository,
           vaultRepository = vaultRepository,
           audioRouteMonitor = audioRouteMonitor,
           context = this@MainActivity, // Leaks Activity context
           audioFeedbackPlayer = audioFeedbackPlayer,
           tactileFeedback = tactileFeedbackController
       )
   }
   ```
3. **`ReceiveViewModel`** (`MainActivity.kt:111`):
   ```kotlin
   val receiveViewModel = remember {
       ReceiveViewModel(
           acousticReceiver = acousticReceiver,
           audioCoordinator = audioCoordinator,
           settingsRepository = settingsRepository,
           vaultRepository = vaultRepository,
           audioFeedbackPlayer = audioFeedbackPlayer,
           tactileFeedback = tactileFeedbackController
       )
   }
   ```
4. **`VaultViewModel`** (`MainActivity.kt:121`):
   ```kotlin
   val vaultViewModel = remember { VaultViewModel(vaultRepository) }
   ```
5. **`SettingsViewModel`** (`MainActivity.kt:122`):
   ```kotlin
   val settingsViewModel = remember {
       SettingsViewModel(
           settingsRepository = settingsRepository,
           vaultRepository = vaultRepository,
           capabilityProbe = CapabilityProbe(this@MainActivity) // Leaks Activity context
       )
   }
   ```
6. **`CalibrationViewModel`** (`MainActivity.kt:129`):
   ```kotlin
   val calibrationViewModel = remember {
       CalibrationViewModel(
           calibrationRunner = app.appContainer.calibrationRunner,
           settingsRepository = settingsRepository,
           audioCoordinator = audioCoordinator
       )
   }
   ```
7. **`OnboardingViewModel`** (`MainActivity.kt:136`):
   ```kotlin
   val onboardingViewModel = remember {
       OnboardingViewModel(settingsRepository, permissionRepository)
   }
   ```
8. **Dev Flavor ViewModels** (`FloatingDevHandle.kt:36`, `DevPanelViewModel.kt:47`):
   - `DevPanelViewModel` created via `remember(themeController) { DevPanelViewModel(themeController, scope) }`.
   - `DspLabViewModel` instantiated directly as a field `val dspLabViewModel = DspLabViewModel()`.

### Defects from this Pattern:
- **No Configuration Change Survival**: Rotating the device recreates `MainActivity`, destroying in-flight state and active receive/transmit tracking.
- **No `onCleared()` Lifecycle**: Coroutine scopes and resource releases hooked into `ViewModel.onCleared()` are never called when the user leaves a destination.
- **Activity Context Leaks**: `TransmitViewModel` and `SettingsViewModel` retain hard references to `ComponentActivity`, preventing garbage collection on configuration restarts.

---

## 4. Dead Code Audit

Classes declared in production source sets (`app/src/main/`) that are never referenced from production code:

1. **`ReceiveSession`** & **`DefaultReceiveSession`** (`app/src/main/java/app/shoutsend/core/session/ReceiveSession.kt`):
   - Intended to coordinate audio focus and `AcousticReceiver`.
   - Never wired into `AppContainer.kt`. Never injected into `ReceiveViewModel`.
   - `ReceiveViewModel` talks directly to `AcousticReceiver` and `AudioCoordinator`, making `ReceiveSession` completely dead in production (only exercised in `SessionIntegrationTest`).
2. **`ReceiveState`** (`app/src/main/java/app/shoutsend/core/session/ReceiveState.kt`):
   - Only used as the state holder of `ReceiveSession`. `ReceiveViewModel` defines its own `ReceiveUiState`.
3. **`SessionLifecycleManager`** (`app/src/main/java/app/shoutsend/core/session/SessionLifecycleManager.kt`):
   - Intended to automatically pause receiving after background grace periods.
   - Never instantiated in `AppContainer`, `MainActivity`, or any ViewModel. Only referenced in `SessionIntegrationTest.kt`.
4. **Dead Design System Component Wrappers**:
   - `CdCard.kt`: 0 usages (replaced universally by `NeonCard`).
   - `CdBottomSheet.kt`: 0 usages.
   - `CdChip.kt`: 0 usages (screens implemented chips via raw `Surface`/`Row`).
   - `CdDialog.kt`: 0 usages (raw `AlertDialog` called directly).
   - `CdDivider.kt`: 0 usages.
   - `CdTopBar.kt`: 0 usages.

---

## 5. "Cyber Radio" Design System Artifacts

The following tokens, color roles, modifiers, and components exist solely for the v1 "Cyber Radio" aesthetic and violate Material 3 Expressive principles:

### Components & Visual Overlays
- **`NeonCard`** (`app/src/main/java/app/shoutsend/core/designsystem/components/NeonCard.kt`): Multi-pass custom canvas stroke simulating an outer neon glow halo.
- **`ScanlineOverlay`** (`app/src/main/java/app/shoutsend/core/designsystem/components/ScanlineOverlay.kt`): CRT-style horizontal scanline canvas overlay.
- **`RadioReadout`** (`app/src/main/java/app/shoutsend/core/designsystem/components/RadioReadout.kt`): Industrial hardware readout box with forced `label.uppercase()` and monospace typography.
- **`BrandMark`** (`app/src/main/java/app/shoutsend/core/designsystem/components/BrandMark.kt`): Concentric neon radar ripple canvas with `glowOpacity`.
- **`StatusPill`** (`app/src/main/java/app/shoutsend/core/designsystem/components/StatusPill.kt`): Glowing indicator badge with forced ALL CAPS status (`"LOCKED"`, `"IDLE"`).

### Extended Color Roles (`ColorTokens.kt`, `DefaultTokens.kt`)
- `spectrumGlow`
- `signalActive`, `signalIdle`, `signalError`
- `signalLow`, `signalMid`, `signalHigh`, `signalPeak`
- `noiseFloorLine`
- `syncMarker` (`0xFFFF007F` neon magenta)
- `toneGuide`
- `glowPrimary` (`0xFF00F0FF` neon cyan)
- `glowSecondary` (`0xFF00FF66` neon lime)
- `scanline`

### Typography & Sizing Tokens
- **`TypographyTokens.kt`**: `readout`, `readoutSmall`, `readoutLarge` all bound to `FontFamily.Monospace`.
- **`RadioVisualTokens.kt`**: `glowRadius: Dp`, `glowAlpha: Float`.
- **`OpacityTokens.kt`**: `glow: Float`.
