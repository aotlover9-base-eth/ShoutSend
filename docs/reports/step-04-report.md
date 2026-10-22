# Step 04 Report: Developer Panel (Dev Flavor Only)

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 4 delivers the interactive runtime Developer Panel for the `dev` flavor, adhering strictly to Rules 10, 11, 12, 15, and 17. 

All developer tooling code resides strictly in `src/dev` (`app.shoutsend.core.devtools`) and is accessed via the `DevTools` seam established in Step 1. The panel itself is constructed exclusively using design-system component wrappers (`CdModalBottomSheet`, `CdButton`, `CdCard`, `CdTextField`, `CdFilterChip`, `CdIconButton`, `CdAlertDialog`), honoring Rule 9.

Key capabilities:
- **Floating Edge Handle:** Draggable pill overlay (`DEV`) on the screen edge.
- **Colors Tab:** Categorized editor for all 35 token roles with live swatch, expandable HSB sliders, and hex text input.
- **Shapes & Radii Tab:** Corner radius sliders for all base radii tokens with live component previews (`CdButton`, `CdCard`, `CdTextField`).
- **Escape Hatch Readability Guard:** The "Reset All" action uses un-overridable baseline tokens (`DefaultTokens.CyberRadioDark.errorContainer` / `onErrorContainer`) ensuring accessibility even if bad contrast colors are dialed in.
- **JSON Export / Import:** Clipboard-driven export and schema-validated import of `TokenOverrides`.

---

## 2. Implemented Architecture

### 2.1 Developer Tooling Components (`src/dev/java/app/shoutsend/core/devtools/`)
- `ColorUtils.kt`: Pure Kotlin RGB <-> HSV and Hex conversion functions. Operates without Android SDK dependencies for JVM unit test execution.
- `DevPanelViewModel.kt`: Reactive ViewModel managing tabs, color categories, radius values, modification detection, and persistence triggers.
- `DeveloperPanel.kt`: Modal bottom sheet UI featuring categorized color rows, HSB sliders, radius sliders, live component preview cards, and import/reset confirmation dialogs.
- `FloatingDevHandle.kt`: Edge-anchored draggable composable pill launching the developer sheet.
- `DevToolsImpl.kt`: Implements `DevTools` interface to expose `FloatingDevHandle` as the `entryPoint`.

### 2.2 Integration Seam
- `AppContainer.kt`: Passes `themeController` to `createDevTools(context, themeController)`.
- `MainActivity.kt`: Invokes `devTools.entryPoint?.invoke()` within the root Compose hierarchy.
- In the `user` flavor, `createDevTools` produces `NoOpDevTools` with a null `entryPoint`, resulting in zero runtime cost and zero dev UI classes in production.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Unit Test Execution
Command: `./gradlew testDevDebugUnitTest testUserDebugUnitTest`
```text
> Task :app:compileUserDebugUnitTestKotlin UP-TO-DATE
> Task :app:testUserDebugUnitTest UP-TO-DATE
> Task :app:compileDevDebugUnitTestKotlin
> Task :app:testDevDebugUnitTest

BUILD SUCCESSFUL in 6s
44 actionable tasks: 2 executed, 42 up-to-date
```
Tests passing:
- `colorUtilsHsvRoundtrip` (PASS)
- `colorUtilsHexParsing` (PASS)
- `viewModelUpdatesColorAndMarksModified` (PASS)
- `viewModelResetsColor` (PASS)
- `viewModelUpdatesRadius` (PASS)
- `viewModelExportAndImport` (PASS)
- All 12 persistence and design system invariant tests (PASS)

### 3.2 APK Assembly (Both Flavors)
Command: `./gradlew assembleBothDebug`
```text
> Task :app:assembleDevDebug
> Task :app:assembleUserDebug
> Task :app:assembleBothDebug

BUILD SUCCESSFUL in 15s
72 actionable tasks: 7 executed, 65 up-to-date
```

### 3.3 Rule 15 User APK Purity Verification (DEX String Search)
Command: Scanned DEX bytecode of `app/build/outputs/apk/user/debug/app-user-debug.apk` for forbidden developer terms:
```text
Forbidden dev panel terms in User APK count: 0
```
Verified that Dev APK contains all developer classes:
```text
Dev terms in Dev APK found: {'DevPanelViewModel', 'DeveloperPanel', 'FloatingDevHandle'}
```

### 3.4 Strict Rule 6 & 7 Scan
- Hex scan `grep -rEn "#[0-9A-Fa-f]{6,8}|Color\(0x" app/src`: **Matches exist exclusively in `DefaultTokens.kt`** (0 leaks).
- Radii scan `RoundedCornerShape(` outside `core/designsystem`: **0 matches**.
- Numeric dp/sp scan outside `core/designsystem`: **0 matches**.

---

## 4. Owner Verification Instructions (OWNER RUNS)

To test the physical interactive behavior on an emulator or connected device:

1. **Install and Launch Dev Build:**
   ```bash
   ./gradlew installDevDebug
   adb shell am start -n app.shoutsend.dev/app.shoutsend.MainActivity
   ```
2. **Open Developer Panel:**
   - Locate the small "DEV" pill on the right screen edge.
   - Drag vertically to verify smooth drag response.
   - Tap the pill to open the modal bottom sheet.
3. **Test Live Color Customization:**
   - Under the "Colors" tab, tap "primary".
   - Drag the Hue slider -> notice the primary headline "ShoutSend" updates color instantly behind the sheet without closing or restarting.
4. **Test Live Radius Customization:**
   - Switch to the "Shapes & Radii" tab.
   - Adjust the "small" slider -> observe the button and text field preview corners change live.
5. **Test Persistence:**
   - Close the app or terminate via `adb shell am force-stop app.shoutsend.dev`.
   - Relaunch the app -> verify the customized primary color remains active.
6. **Test Reset All:**
   - Reopen the panel and tap "Reset All" in the header.
   - Confirm reset in the dialog -> verify tokens immediately revert to factory Cyber Radio defaults.

---

## 5. Next Steps
- Proceed to **Step 5: Style guard and User APK purity check** (Gradle verification tasks `verifyDesignSystemTokens`, `verifyUserApkClean`, `verifyNoInternetPermission`).
