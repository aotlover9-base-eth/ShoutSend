# Step 02 Report: Design System Core

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 2 establishes the core token-based design system for ShoutSend, fulfilling Rules 5, 6, 7, 8, 9, and 26. All visual attributes—colors, corner radii, elevation, spacing, strokes, component dimensions, opacities, and motion curves—are modeled as immutable Kotlin data structures accessible through `ChirpTheme`. 

Strict architectural constraints are enforced:
- Exactly one file (`DefaultTokens.kt`) contains raw hex colors and measurement literals.
- All 12 required component wrappers (`CdButton`, `CdIconButton`, `CdCard`, `CdTextField`, `CdDialog`, `CdBottomSheet`, `CdTopBar`, `CdChip`, `CdSwitch`, `CdSnackbarHost`, `CdDivider`, `CdScaffold`) are implemented in `core/designsystem/components` using only token references.
- Previews reside exclusively in `src/dev` to preserve User APK purity.
- Automated static checks verify zero leaks of hex or radii literals across the codebase.

---

## 2. Implemented Architecture

### 2.1 Token Models (`app.shoutsend.core.designsystem`)
- `ColorTokens.kt`: Complete Material 3 color role set plus 4 extended acoustic status slots (`spectrumGlow`, `signalActive`, `signalIdle`, `signalError`).
- `ShapeTokens.kt`: Base radius scale (`extraSmall` to `extraLarge`) and component shapes (`button`, `card`, `dialog`, `bottomSheet`, `textField`, `chip`, `fab`) referencing the radii.
- `TypographyTokens.kt`: Material 3 type scale plus monospace `radioReadoutLarge`, `radioReadoutMedium`, and `radioReadoutSmall`.
- `SpacingTokens.kt`: Fixed 7-step scale (`xxs`: 2dp, `xs`: 4dp, `sm`: 8dp, `md`: 16dp, `lg`: 24dp, `xl`: 32dp, `xxl`: 48dp).
- `StrokeTokens.kt`: Outlines, dividers, and canvas borders (`hairline`: 0.5dp, `thin`: 1dp, `regular`: 2dp, `thick`: 4dp).
- `SizeTokens.kt`: Standard icon sizes (`iconXs` to `iconXl`), minimum touch targets (`touchTargetMin`: 48dp), and heights.
- `ElevationTokens.kt`: Shadow levels (`level0` to `level5`).
- `OpacityTokens.kt`: Opacity alphas (`disabled`, `scrim`, `glow`, `muted`, `subtle`).
- `MotionTokens.kt`: Durations (`durationShort1` to `durationExtraLong2`), easing curves, and spring physics specs.
- `DefaultTokens.kt`: The single hex/literal source file defining Cyber Radio Dark and Cyber Radio Light factory palettes.
- `DesignTokens.kt`: Bundle model providing `ThemeMode` (`SYSTEM`, `DARK`, `LIGHT`) and `dynamicColor` toggle.
- `ChirpTheme.kt`: Composable theme wrapper providing MaterialTheme bridging, dynamic color support for API 31+, and static property accessors.

### 2.2 Component Wrappers (`app.shoutsend.core.designsystem.components`)
- `CdButton.kt`: `CdButton`, `CdTonalButton`, `CdOutlinedButton`, `CdTextButton`
- `CdIconButton.kt`: `CdIconButton`, `CdFilledIconButton`, `CdTonalIconButton`, `CdOutlinedIconButton`
- `CdCard.kt`: `CdCard`, `CdOutlinedCard`, `CdElevatedCard` (clickable and static variants)
- `CdTextField.kt`: `CdTextField`, `CdOutlinedTextField`
- `CdDialog.kt`: `CdAlertDialog`
- `CdBottomSheet.kt`: `CdModalBottomSheet`
- `CdTopBar.kt`: `CdTopAppBar`, `CdCenterAlignedTopAppBar`, `CdLargeTopAppBar`
- `CdChip.kt`: `CdAssistChip`, `CdFilterChip`, `CdInputChip`, `CdSuggestionChip`
- `CdSwitch.kt`: `CdSwitch`
- `CdSnackbarHost.kt`: `CdSnackbarHost`, `CdSnackbar`
- `CdDivider.kt`: `CdHorizontalDivider`, `CdVerticalDivider`
- `CdScaffold.kt`: `CdScaffold`

### 2.3 Developer Previews (`app/src/dev/java/app/shoutsend/core/designsystem/previews`)
- `ButtonsPreview.kt`: Action and icon button variants across dark and light themes.
- `CardsAndFieldsPreview.kt`: Cards, text inputs, toggles, and filter/assist chips.
- `SurfacesAndModalsPreview.kt`: Top navigation bars, dialog layouts, and dividers.

---

## 3. WCAG AA Contrast Compliance

All key foreground/background pairs were evaluated against WCAG 2.1 contrast formulas:

| Palette | Foreground Token | Background Token | Hex Pair | Contrast Ratio | WCAG AA Requirement | Result |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Dark** | `onPrimary` | `primary` | `#00363D` on `#00F0FF` | **9.35 : 1** | >= 4.5 : 1 | PASS (AAA) |
| **Dark** | `onSecondary` | `secondary` | `#003912` on `#00FF66` | **9.72 : 1** | >= 4.5 : 1 | PASS (AAA) |
| **Dark** | `onTertiary` | `tertiary` | `#412D00` on `#FFB300` | **7.31 : 1** | >= 4.5 : 1 | PASS (AAA) |
| **Dark** | `onError` | `error` | `#410002` on `#FF5449` | **5.40 : 1** | >= 4.5 : 1 | PASS (AA) |
| **Dark** | `onBackground`| `background` | `#E1E7F0` on `#0B0E14` | **15.54 : 1** | >= 4.5 : 1 | PASS (AAA) |
| **Dark** | `onSurface` | `surface` | `#E1E7F0` on `#0B0E14` | **15.54 : 1** | >= 4.5 : 1 | PASS (AAA) |
| **Light** | `onPrimary` | `primary` | `#FFFFFF` on `#006874` | **6.50 : 1** | >= 4.5 : 1 | PASS (AA) |
| **Light** | `onSecondary` | `secondary` | `#FFFFFF` on `#006E26` | **6.43 : 1** | >= 4.5 : 1 | PASS (AA) |
| **Light** | `onBackground`| `background` | `#171C26` on `#F7F9FE` | **16.34 : 1** | >= 4.5 : 1 | PASS (AAA) |
| **Light** | `onSurface` | `surface` | `#171C26` on `#F7F9FE` | **16.34 : 1** | >= 4.5 : 1 | PASS (AAA) |

---

## 4. Verification Evidence (Real Outputs)

### 4.1 Unit Test Execution
Command: `./gradlew testDevDebugUnitTest testUserDebugUnitTest`
```text
> Task :app:compileUserDebugKotlin UP-TO-DATE
> Task :app:compileUserDebugUnitTestKotlin
> Task :app:testUserDebugUnitTest
> Task :app:compileDevDebugKotlin
> Task :app:compileDevDebugUnitTestKotlin
> Task :app:testDevDebugUnitTest

BUILD SUCCESSFUL in 17s
44 actionable tasks: 13 executed, 31 up-to-date
```

### 4.2 APK Assembly (Both Flavors)
Command: `./gradlew assembleBothDebug`
```text
> Task :app:assembleDevDebug
> Task :app:assembleUserDebug
> Task :app:assembleBothDebug

BUILD SUCCESSFUL in 11s
72 actionable tasks: 8 executed, 64 up-to-date
```

### 4.3 Rule 6 (Hex Literal Scan)
Command: `grep -rEn "#[0-9A-Fa-f]{6,8}|Color\(0x" app/src`
```text
app/src/main/java/app/shoutsend/core/designsystem/DefaultTokens.kt:24:        primary = Color(0xFF00F0FF),
... [all 76 matches strictly contained in DefaultTokens.kt] ...
app/src/main/java/app/shoutsend/core/designsystem/DefaultTokens.kt:103:        signalError = Color(0xFFBA1A1A)
```
Matches outside `DefaultTokens.kt`: **0**.

### 4.4 Rule 7 (Radii / dp / sp Leak Scan)
Command: Custom AST/Regex scanner across `app/src` excluding `core/designsystem`.
```text
RoundedCornerShape outside core/designsystem matches count: 0
dp/sp outside core/designsystem matches count: 0
```

---

## 5. Artifacts and Documentation
- `docs/DESIGN_SYSTEM.md`: Comprehensive reference, token groups, naming rules, how-to guides, and contrast matrix.
- `docs/PROJECT_MEMORY.md`: Updated current state to reflect Step 2 completion.
- `docs/DECISIONS.md`: Logged DEC-005, DEC-006, DEC-007, and DEC-008.
- `docs/ROADMAP.md`: Marked Step 2 as Done.

---

## 6. Next Steps
- Proceed to **Step 3: Token state and persistence** (`ThemeController`, DataStore preferences, reset-to-defaults).
