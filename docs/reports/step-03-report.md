# Step 03 Report: Token State and Persistence

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 3 establishes the reactive runtime token state and persistence architecture for ShoutSend, satisfying Rules 10, 11, 12, 14, and 15.

Key accomplishments:
- Implemented `ThemeController` which exposes a reactive `StateFlow<DesignTokens>` combining base factory tokens with sparse overrides.
- Implemented `TokenOverrides`—a sparse `@Serializable` data model storing only explicitly modified color roles and corner radii.
- Established a multi-flavor seam architecture:
  - `src/dev`: Backed by AndroidX Preferences DataStore storing overrides as JSON with 300ms write debouncing and corruption fallback.
  - `src/user`: Backed by `NoOpTokenOverridesRepository`, ensuring zero developer persistence overhead and zero class leakages in the production User APK.
- Wired `MainActivity.kt` to collect `themeController.tokens` via `collectAsStateWithLifecycle()`, allowing instantaneous tree-wide recomposition without restarting the Activity.
- Verified via DEX inspection of the User APK that zero `DevDataStore` classes exist in production.

---

## 2. Implemented Architecture

### 2.1 Core Contracts (`app.shoutsend.core.designsystem`)
- `TokenOverrides.kt`: Sparse data model holding `colors: Map<String, Long>`, `radii: Map<String, Float>`, and `extra: Map<String, String>`. Includes `TokenOverrides.validate()` to reject unknown role names and out-of-range radii.
- `TokenOverridesRepository.kt`: Defines `TokenOverridesSource`, `TokenOverridesWriter`, `TokenOverridesRepository`, and the baseline `NoOpTokenOverridesRepository`.
- `ThemeController.kt`: State management class combining `_baseTokens` with `repository.overrides`. Provides the programmatic API:
  - `setColor(role, color)`
  - `setRadius(name, radiusDp)`
  - `resetColor(role)`
  - `resetRadius(name)`
  - `resetAll()`
  - `exportToJson()`
  - `importFromJson(jsonString)`
  - `updateThemeMode(mode)`
  - `updateDynamicColor(enabled)`

### 2.2 Flavor Seam Implementation
- `src/user/java/app/shoutsend/core/designsystem/TokenOverridesRepositoryFactory.kt`: Returns `NoOpTokenOverridesRepository()`.
- `src/dev/java/app/shoutsend/core/designsystem/`:
  - `DevDataStoreTokenOverridesRepository.kt`: Uses Preferences DataStore with a `MutableSharedFlow` debounce pipeline (300 ms). Catches corrupted JSON and falls back safely to default tokens.
  - `TokenOverridesRepositoryFactory.kt`: Instantiates `DevDataStoreTokenOverridesRepository(context, scope)`.

### 2.3 UI Integration (`MainActivity.kt` & `AppContainer.kt`)
- `AppContainer.kt` initializes `themeController` on the application coroutine scope.
- `MainActivity.kt` consumes `themeController.tokens.collectAsStateWithLifecycle()` and wraps the content in `ChirpTheme(tokens = tokens)`.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Unit Test Execution
Command: `./gradlew testDevDebugUnitTest testUserDebugUnitTest`
```text
> Task :app:compileDevDebugUnitTestKotlin
> Task :app:testDevDebugUnitTest
> Task :app:compileUserDebugUnitTestKotlin
> Task :app:testUserDebugUnitTest

BUILD SUCCESSFUL in 10s
44 actionable tasks: 4 executed, 40 up-to-date
```
Tests verified:
- `themeControllerMergesColorOverrides` (PASS)
- `themeControllerMergesRadiusOverridesAndUpdatesComponents` (PASS)
- `resetIndividualKeysAndResetAll` (PASS)
- `validationRejectsUnknownKeysAndOutOfRangeValues` (PASS)
- `exportAndImportJsonIntegrity` (PASS)
- `importRejectsCorruptedJson` (PASS)
- `writeDebounceCollapsesRapidUpdates` (PASS)
- `componentShapeTokensReferenceRadiusTokens` (PASS)
- `everyColorRoleHasSpecifiedValue` (PASS)
- `defaultPalettesMeetWcagContrastAA` (PASS)
- `tokenSetDefaultsIntegrity` (PASS)
- `spacingAndSizesHierarchy` (PASS)

### 3.2 APK Assembly (Both Flavors)
Command: `./gradlew assembleBothDebug`
```text
> Task :app:assembleDevDebug UP-TO-DATE
> Task :app:assembleUserDebug UP-TO-DATE
> Task :app:assembleBothDebug UP-TO-DATE

BUILD SUCCESSFUL in 3s
72 actionable tasks: 72 up-to-date
```

### 3.3 Rule 15 User APK Purity Verification (DEX Inspection)
Script: Inspection of DEX classes inside `app/build/outputs/apk/user/debug/app-user-debug.apk`.
```text
Found dex files: ['classes.dex', 'classes2.dex', 'classes3.dex', 'classes5.dex', 'classes6.dex', 'classes4.dex', 'classes7.dex']
Total designsystem classes in User APK: 80
Dev-specific token classes found in User APK: []
```
Classes in Dev APK confirmed:
```text
DevDataStore classes found in Dev APK: {
  'Lapp/shoutsend/core/designsystem/DevDataStoreTokenOverridesRepository;',
  'Lapp/shoutsend/core/designsystem/DevDataStoreTokenOverridesRepository$persistToDataStore$1;',
  'Lapp/shoutsend/core/designsystem/DevDataStoreTokenOverridesRepository$Companion;',
  ...
}
```

### 3.4 Strict Rule 6 & 7 Scan
- Hex scan `grep -rEn "#[0-9A-Fa-f]{6,8}|Color\(0x" app/src`: **Matches exist exclusively in `DefaultTokens.kt`** (0 leaks).
- Radii scan `RoundedCornerShape(` outside `core/designsystem`: **0 matches**.
- Numeric dp/sp scan outside `core/designsystem`: **0 matches**.

---

## 4. Documentation & Artifacts
- `docs/DESIGN_SYSTEM.md`: Documented Section 6 with architecture flow diagram and override characteristics.
- `docs/PROJECT_MEMORY.md`: Updated current project state for Step 3.
- `docs/DECISIONS.md`: Logged DEC-009, DEC-010, and DEC-011.
- `docs/ROADMAP.md`: Marked Step 3 as Done.

---

## 5. Next Steps
- Proceed to **Step 4: Developer panel (dev flavor)** (`DevTools` Compose panel, interactive color pickers, radii sliders, live token overrides, export/import JSON, reset).
