# Step R1 Verification Report: Foundation

**Project:** ShoutSend v2  
**Step:** R1 (Foundation: dependency upgrade, proper ViewModels, permission and network guards)  
**Date:** 2026-10-28  
**Author:** Antigravity  

---

## 1. Executive Summary

Step R1 establishes the architectural foundation for ShoutSend v2 before beginning visual redesign work in Step R2. All 5 tasks outlined in `docs/V2_PLAN.md` and the extra review criteria have been executed:
1. **Dependency Modernization**: Upgraded AGP, Kotlin, KSP, Compose BOM, navigation, lifecycle, activity, coroutines, and serialization to their latest stable inter-compatible releases. Material 3 is pinned to `1.4.0-alpha08`, providing full access to all Material 3 Expressive APIs. Added `graphics-shapes:1.0.1` and `material-kolor:2.0.2`.
2. **Proper ViewModels**: Migrated all ViewModels in `app/src/main` and `app/src/dev` to a centralized `AppViewModelProvider.Factory`. Completely eliminated all 7 `remember { ...ViewModel }` instances from `MainActivity`. Scoped ViewModels properly to `NavBackStackEntry` or `ComponentActivity`. Zero ViewModels retain an Activity `Context`.
3. **Dead Code Deletion**: Deleted unreferenced session managers (`ReceiveSession`, `ReceiveState`, `SessionLifecycleManager`) and deleted 6 look-alike design system wrappers (`CdCard`, `CdBottomSheet`, `CdChip`, `CdDialog`, `CdDivider`, `CdTopBar`), migrating all 50+ call sites across the application directly to real `androidx.compose.material3` components in compliance with Rule 3 (amended).
4. **Architectural Guards in Code**:
   - `docs/RULES.md`: Appended v2 amendments (Rules 3, 6, 24) and Rules 29–32.
   - `PermissionsGuard`: Enforced Rule 24 (replaced) permission set across both flavors. Added permissions to `AndroidManifest.xml`.
   - `NetworkClientsGuard` (`checkNoNetworkClients`): Enforces Rule 29, rejecting all HTTP/network clients and dynamic DNS, restricting raw sockets strictly to `core/transfer/`. Hooked into `check`.
   - `StyleGuard` (`checkNoHardcodedStyle`): Extended with Rule 32a (no emojis/glyphs in UI strings) and Rule 32b (no monospace, no uppercase on Text). Initial 57 legacy violations staged in `config/style-allowlist.txt` under `# remove in R3 to R6`.
5. **Full Verification**: All 599 unit tests re-run and passed from clean (`--rerun-tasks`). Lint completed with 0 errors and 99 warnings for both flavors (reduced from 102).

---

## 2. Dependency Modernization Table

| Dependency / Plugin | Before Version | After Version | License | Size / Notes |
|---|---|---|---|---|
| **Android Gradle Plugin (`agp`)** | `8.5.2` | `8.6.1` | Apache-2.0 | Stable AGP release compatible with JDK 17 & 21 |
| **Kotlin** | `2.0.0` | `2.0.21` | Apache-2.0 | Stable Kotlin compiler release |
| **KSP** | `2.0.0-1.0.24` | `2.0.21-1.0.28` | Apache-2.0 | Compatible with Kotlin 2.0.21 and Room 2.6.1 |
| **Compose BOM** | `2024.06.00` | `2024.12.01` | Apache-2.0 | Dec 2024 Compose BOM aligning UI runtimes |
| **Material 3 (`compose-material3`)** | `1.2.1` | `1.4.0-alpha08` | Apache-2.0 | Explicitly pinned for Material 3 Expressive APIs |
| **`androidx.graphics:graphics-shapes`** | *(none)* | `1.0.1` | Apache-2.0 | ~85 KB; polygon and corner morphing primitives |
| **`com.materialkolor:material-kolor`** | *(none)* | `2.0.2` | Apache-2.0 | ~140 KB, minSdk 21; generates 30+ M3 colors from seed |
| **`androidx.compose.material:material-icons-core`** | *(transitive)* | BOM managed | Apache-2.0 | Core Material symbols |
| **`androidx.compose.material:material-icons-extended`**| *(none)* | BOM managed | Apache-2.0 | Extended Material symbols |
| **Lifecycle (`lifecycle-runtime-ktx`)** | `2.8.2` | `2.8.7` | Apache-2.0 | Stable AndroidX Lifecycle release |
| **Activity Compose (`activity-compose`)** | `1.9.0` | `1.9.3` | Apache-2.0 | Edge-to-edge and ComponentActivity integration |
| **Navigation Compose (`navigation-compose`)**| `2.8.3` | `2.8.5` | Apache-2.0 | Type-safe navigation runtime |
| **Core KTX (`core-ktx`)** | `1.13.1` | `1.15.0` | Apache-2.0 | Android 15 platform extensions |
| **Coroutines (`kotlinx-coroutines-*`)** | `1.8.1` | `1.9.0` | Apache-2.0 | Coroutine runtime matching Kotlin 2.0.21 |
| **Serialization (`kotlinx-serialization-json`)**| `1.6.3` | `1.7.3` | Apache-2.0 | JSON compaction serializer |
| **Room (`room-*`)** | `2.6.1` | `2.6.1` | Apache-2.0 | Stable, unchanged |
| **DataStore (`datastore-preferences`)** | `1.1.1` | `1.1.1` | Apache-2.0 | Stable, unchanged |
| **minSdk** | `31` | `31` | — | Android 12 SnowCone baseline maintained |

### Material 3 Expressive API Verification (`1.4.0-alpha08`)
All 5 required Expressive API capabilities verified present and resolvable:
- `MaterialExpressiveTheme`: Available (`androidx.compose.material3.MaterialExpressiveTheme`).
- `MotionScheme.Companion.expressive()`: Available (`androidx.compose.material3.MotionScheme.expressive()`).
- `MaterialShapes`: Available (`androidx.compose.material3.MaterialShapes` with `PointNRound`, `toShape()`).
- `LoadingIndicator`: Available (`androidx.compose.material3.LoadingIndicator`, `ContainedLoadingIndicator`).
- `ButtonGroup`: Available (`androidx.compose.material3.ButtonGroup`, `ButtonGroupDefaults`).

---

## 3. ViewModel Migration Checklist (Audit Section 3)

| ViewModel | File | Factory / Resolution Mechanism | Scoped Owner | Activity Context Held? |
|---|---|---|---|---|
| **`SendViewModel`** | `feature/send/SendViewModel.kt` | `AppViewModelProvider.Factory` via `viewModel(...)` | `ComponentActivity` (shared for retries/sharing) | **No** (0 context params) |
| **`TransmitViewModel`** | `feature/send/TransmitViewModel.kt` | `AppViewModelProvider.Factory` via `viewModel(...)` | `ComponentActivity` (shared between compose & sending) | **No** (uses `applicationContext`) |
| **`ReceiveViewModel`** | `feature/receive/ReceiveViewModel.kt` | `AppViewModelProvider.Factory` via `viewModel(...)` | `NavBackStackEntry` (scoped to `ReceiveRoute`) | **No** (0 context params) |
| **`VaultViewModel`** | `feature/vault/VaultViewModel.kt` | `AppViewModelProvider.Factory` via `viewModel(...)` | `NavBackStackEntry` (scoped to `VaultRoute`/Detail) | **No** (0 context params) |
| **`SettingsViewModel`** | `feature/settings/SettingsViewModel.kt`| `AppViewModelProvider.Factory` via `viewModel(...)` | `NavBackStackEntry` (scoped to `SettingsRoute`) | **No** (`CapabilityProbe(applicationContext)`) |
| **`CalibrationViewModel`**| `feature/calibration/CalibrationViewModel.kt`| `AppViewModelProvider.Factory` via `viewModel(...)` | `NavBackStackEntry` (scoped to `CalibrationRoute`) | **No** (0 context params) |
| **`OnboardingViewModel`** | `feature/onboarding/OnboardingViewModel.kt` | `AppViewModelProvider.Factory` via `viewModel(...)` | `NavBackStackEntry` (scoped to `OnboardingRoute`) | **No** (0 context params) |
| **`DevPanelViewModel`** | `core/devtools/DevPanelViewModel.kt` | Scoped ViewModel factory via `viewModel(...)` | Local Composable / Activity | **No** (extends `ViewModel()`, uses `viewModelScope`) |
| **`DspLabViewModel`** | `core/devtools/DspLabViewModel.kt` | Extends `ViewModel()` | Dev panel child ViewModel | **No** (pure DSP logic, 0 context params) |

**MainActivity Status**: Zero `remember { ...ViewModel }` calls remain. All 7 ViewModel references in `MainActivity.kt` were removed.

---

## 4. Dead Code Audit & Deletion

1. **`ReceiveSession.kt` & `ReceiveState.kt`**:
   - Status: **Deleted**.
   - Justification: `ReceiveViewModel` talks directly to `AudioCoordinator` and `AcousticReceiver`. Production never wired `ReceiveSession`. Direct receiver coordination is simpler and eliminates redundant state synchronization.
2. **`SessionLifecycleManager.kt`**:
   - Status: **Deleted**.
   - Justification: Was completely unreferenced by production code; background audio lifecycle is properly handled by Android audio routing and ViewModel scopes.
3. **`SessionIntegrationTest.kt`**:
   - Status: **Updated**.
   - Updated to verify `AudioCoordinator` and `AcousticReceiver` interaction directly without `ReceiveSession`.
4. **Deleted Design System Wrappers**:
   - `CdCard.kt`: Deleted.
   - `CdBottomSheet.kt`: Deleted.
   - `CdChip.kt`: Deleted.
   - `CdDialog.kt`: Deleted.
   - `CdDivider.kt`: Deleted.
   - `CdTopBar.kt`: Deleted.
   - *Audit Note*: Section 4 of `V2_AUDIT.md` incorrectly identified these wrappers as having 0 usages. They were in fact referenced across 8 production screens and previews. In compliance with Rule 3 (amended: *"No look-alike custom components when an M3 component exists"*), all call sites across `AppScaffold.kt`, `SendScreen.kt`, `TransmitScreen.kt`, `ReceiveScreen.kt`, `VaultScreen.kt`, `VaultDetailScreen.kt`, `SettingsScreen.kt`, `WifiComposer.kt`, `DeveloperPanel.kt`, `DspLabScreen.kt`, and previews were cleanly migrated directly to `androidx.compose.material3` components (`AlertDialog`, `TopAppBar`, `CenterAlignedTopAppBar`, `FilterChip`, `AssistChip`, `SuggestionChip`, `ModalBottomSheet`, `HorizontalDivider`, `Card`, `OutlinedCard`, `ElevatedCard`).

---

## 5. Architectural Guards & Permissions

### Permissions Enforcement (Rule 24 Replaced)
Merged manifests for both `devDebug` and `userDebug` were audited via `aapt2 dump permissions` and `verifyPermissions`:

```text
package: app.shoutsend
uses-permission: name='android.permission.RECORD_AUDIO'
uses-permission: name='android.permission.VIBRATE'
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_WIFI_STATE'
uses-permission: name='android.permission.CHANGE_WIFI_STATE'
uses-permission: name='android.permission.NEARBY_WIFI_DEVICES' usesPermissionFlags='neverForLocation'
uses-permission: name='android.permission.ACCESS_FINE_LOCATION' maxSdkVersion='32'
permission: app.shoutsend.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='app.shoutsend.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```
```text
package: app.shoutsend.dev
uses-permission: name='android.permission.RECORD_AUDIO'
uses-permission: name='android.permission.VIBRATE'
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_WIFI_STATE'
uses-permission: name='android.permission.CHANGE_WIFI_STATE'
uses-permission: name='android.permission.NEARBY_WIFI_DEVICES' usesPermissionFlags='neverForLocation'
uses-permission: name='android.permission.ACCESS_FINE_LOCATION' maxSdkVersion='32'
permission: app.shoutsend.dev.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='app.shoutsend.dev.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```

### Network Clients Guard (`checkNoNetworkClients`)
- Enforces Rule 29.
- Scans `app/src` and `modem/src`.
- Fails build on `HttpURLConnection`, `HttpsURLConnection`, `openConnection(`, `okhttp`, `ktor`, `retrofit`, `java.net.URL(` I/O, `WebView`, or dynamic non-literal `InetAddress.getByName(`.
- Constrains raw `Socket` and `ServerSocket` strictly to `core/transfer/`.
- Verification output: `checkNoNetworkClients: Clean! Zero network client libraries or outbound HTTP callers found.`

### Anti-Slop Style Guard (`checkNoHardcodedStyle`)
- Extended to detect:
  - Rule 32a: Forbidden glyphs (`▲`, `▼`, `■`, `●`, `◆`, `ℹ`, `✓`, `✔`, `✕`, `✖`) and emoji code points in UI strings.
  - Rule 32b: `FontFamily.Monospace` in user-facing UI code (DSP Lab exempt).
  - Rule 32b: `.uppercase()` on UI text passed to `Text()`.
- 57 existing legacy v1 violations cataloged in `config/style-allowlist.txt` under `# remove in R3 to R6`.
- Verification output: `checkNoHardcodedStyle: All sources comply with tokenization rules.`

---

## 6. Standard Verification (SV) Execution Results

### 1. `:modem:test`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).
> Task :modem:test
BUILD SUCCESSFUL
```
- Tests run: **166** | Passed: **166** | Failed: **0** | Skipped: **0**

### 2. `testDevDebugUnitTest` (`--rerun-tasks`)
```text
> Task :app:testDevDebugUnitTest
BUILD SUCCESSFUL
```
- Tests run: **225** | Passed: **225** | Failed: **0** | Skipped: **0**

### 3. `testUserDebugUnitTest` (`--rerun-tasks`)
```text
> Task :app:testUserDebugUnitTest
BUILD SUCCESSFUL
```
- Tests run: **208** | Passed: **208** | Failed: **0** | Skipped: **0**

**Total Unit Tests Executed Cleanly**: **599 / 599 passed (100%)**.

### 4. `assembleDevDebug` & `assembleUserDebug`
```text
> Task :app:assembleDevDebug
> Task :app:assembleUserDebug
BUILD SUCCESSFUL
```

### 5. `lintDevDebug` & `lintUserDebug`
- **Baseline Before Changes (R0)**:
  - `lintDevDebug`: 0 errors, 102 warnings
  - `lintUserDebug`: 0 errors, 102 warnings
- **After Step R1 Changes**:
  - `lintDevDebug`: **0 errors, 99 warnings** (net reduction of 3 warnings)
  - `lintUserDebug`: **0 errors, 99 warnings** (net reduction of 3 warnings)
- Output: `Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-userDebug.html`

### 6. `verifyUserApkClean`
```text
> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.
BUILD SUCCESSFUL
```

### 7. `checkNoHardcodedStyle`
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.
BUILD SUCCESSFUL
```

### 8. `checkNoNetworkClients`
```text
> Task :app:checkNoNetworkClients
checkNoNetworkClients: Clean! Zero network client libraries or outbound HTTP callers found.
BUILD SUCCESSFUL
```

### 9. `verifyPermissions`
```text
> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.
BUILD SUCCESSFUL
```

---

## 7. What is NOT Verified

- **Emulator Rotation During Transmission**: Not verified. No running emulator or physical hardware attached at verification time (`adb devices` returned 0 attached devices). The ViewModel migration to Activity-scoped and NavBackStackEntry-scoped instances structurally preserves transmission state across configuration changes, but dynamic runtime rotation on physical hardware remains to be confirmed by the owner.
- **Physical Wi-Fi Direct Connection**: In accordance with the roadmap, Wi-Fi Direct sockets and peer-to-peer transport will be implemented in Step R8 and field-tested in Step R9 (OWNER RUNS).

---

## 8. Summary of Files Changed

- **Build Configuration**:
  - `gradle/libs.versions.toml`: Upgraded dependency versions; pinned Material 3 to `1.4.0-alpha08`; added `graphics-shapes`, `material-kolor`, and `material-icons`.
  - `app/build.gradle.kts`: Wired new dependencies, registered `checkNoNetworkClients`, included in `verifyChirpRules`.
  - `buildSrc/src/main/kotlin/app/shoutsend/buildtools/PermissionsGuard.kt`: Updated to Rule 24 (replaced).
  - `buildSrc/src/main/kotlin/app/shoutsend/buildtools/NetworkClientsGuard.kt`: Added Rule 29 network client detector.
  - `buildSrc/src/main/kotlin/app/shoutsend/buildtools/tasks/CheckNoNetworkClientsTask.kt`: Task wrapper registered in Gradle.
  - `buildSrc/src/main/kotlin/app/shoutsend/buildtools/StyleGuard.kt`: Added Rules 32a and 32b checks.
  - `buildSrc/src/test/kotlin/app/shoutsend/buildtools/`: Added tests for `NetworkClientsGuardTest`, updated `PermissionsGuardTest` and `StyleGuardTest`.
  - `config/style-allowlist.txt`: Added 57 legacy v1 UI violations for removal in R3–R6.
- **App Manifest**:
  - `app/src/main/AndroidManifest.xml`: Declared exact Rule 24 (replaced) permissions (`INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `NEARBY_WIFI_DEVICES`, `ACCESS_FINE_LOCATION`).
- **ViewModels & Navigation**:
  - `app/src/main/java/app/shoutsend/AppViewModelProvider.kt`: Created centralized `ViewModelProvider.Factory`.
  - `app/src/main/java/app/shoutsend/MainActivity.kt`: Removed all 7 `remember { ...ViewModel }` instances; zero Activity Context passed to ViewModels.
  - `app/src/main/java/app/shoutsend/navigation/ShoutSendNavHost.kt`: ViewModels resolved via `viewModel(factory = AppViewModelProvider.Factory)`.
  - `app/src/main/java/app/shoutsend/feature/send/TransmitViewModel.kt`: Refactored to accept `applicationContext: Context?`.
  - `app/src/dev/java/app/shoutsend/core/devtools/DevPanelViewModel.kt`: Extended `ViewModel()`, uses `viewModelScope`.
  - `app/src/dev/java/app/shoutsend/core/devtools/DspLabViewModel.kt`: Extended `ViewModel()`.
  - `app/src/dev/java/app/shoutsend/core/devtools/FloatingDevHandle.kt`: Resolves `DevPanelViewModel` via `viewModel(...)`.
- **Dead Code Cleanup & Direct M3 Migration**:
  - Deleted: `ReceiveSession.kt`, `ReceiveState.kt`, `SessionLifecycleManager.kt`.
  - Deleted: `CdCard.kt`, `CdBottomSheet.kt`, `CdChip.kt`, `CdDialog.kt`, `CdDivider.kt`, `CdTopBar.kt`.
  - Migrated call sites in `AppScaffold.kt`, `SendScreen.kt`, `TransmitScreen.kt`, `ReceiveScreen.kt`, `VaultScreen.kt`, `VaultDetailScreen.kt`, `SettingsScreen.kt`, `WifiComposer.kt`, `DeveloperPanel.kt`, `DspLabScreen.kt`, `CardsAndFieldsPreview.kt`, and `SurfacesAndModalsPreview.kt`.
- **Documentation**:
  - `docs/RULES.md`: Updated with v2 amendments and Rules 29–32.
  - `docs/DECISIONS.md`: Appended `DEC-070`.
  - `docs/ROADMAP.md`: Marked Step R1 as Done.
  - `docs/PROJECT_MEMORY.md`: Recorded Step R1 completion.
  - `docs/reports/v2-step-R01-report.md`: Authored Step R1 report.
