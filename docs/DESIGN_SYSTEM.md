# ShoutSend Design System

## 1. Architecture & Core Philosophy
ShoutSend UI is built strictly on a tokenized design system conforming to Material 3 (Material You).

### Non-Negotiable Rules
- **Rule 6 (Single Hex File):** Exactly ONE file in the entire repository may contain raw hex color values or `Color(0x...)`: `app/src/main/java/app/shoutsend/core/designsystem/DefaultTokens.kt`.
- **Rule 7 (No Radii or Measurement Literals):** Numeric `dp`, `sp`, and millisecond literals are forbidden in UI code outside `DefaultTokens.kt`. All margins, paddings, corner radii, strokes, icon sizes, and durations must reference design tokens.
- **Rule 8 (Single Design System):** All application UI consumes tokens via `ChirpTheme` and wrappers in `app.shoutsend.core.designsystem.components.*`.
- **Rule 9 (Consistent Component Wrappers):** Screens and features must never call unstyled Material 3 composables (`Button`, `Card`, `TextField`, `Dialog`, etc.) directly. They must use the corresponding `Cd*` wrapper.

---

## 2. Token Groups

All tokens are modeled as immutable Kotlin data classes in `app.shoutsend.core.designsystem`:

| Token Group | Interface / Class | Responsibilities |
| --- | --- | --- |
| **ColorTokens** | `ColorTokens.kt` | Material 3 color roles + extended radio slots (`spectrumGlow`, `signalActive`, `signalIdle`, `signalError`). |
| **ShapeTokens** | `ShapeTokens.kt` | Corner-radius scale (`extraSmall` to `extraLarge`) and component shapes (`button`, `card`, `dialog`, `bottomSheet`, `textField`, `chip`, `fab`). Component shapes reference the radius tokens. |
| **TypographyTokens** | `TypographyTokens.kt` | M3 type scale (`displayLarge` to `labelSmall`) plus monospace `radioReadoutLarge`, `radioReadoutMedium`, `radioReadoutSmall`. |
| **SpacingTokens** | `SpacingTokens.kt` | Strict 7-step scale: `xxs` (2dp), `xs` (4dp), `sm` (8dp), `md` (16dp), `lg` (24dp), `xl` (32dp), `xxl` (48dp). |
| **StrokeTokens** | `StrokeTokens.kt` | Outlines, dividers, and canvas borders: `hairline` (0.5dp), `thin` (1dp), `regular` (2dp), `thick` (4dp). |
| **SizeTokens** | `SizeTokens.kt` | Icon dimensions (`iconXs` to `iconXl`), minimum touch targets (`touchTargetMin` = 48dp), and heights (`buttonHeight`, `textFieldHeight`, `topBarHeight`, `navBarHeight`). |
| **ElevationTokens** | `ElevationTokens.kt` | Shadow and tonal elevation: `level0` (0dp) through `level5` (12dp). |
| **OpacityTokens** | `OpacityTokens.kt` | Transparency multipliers: `disabled` (0.38f), `scrim` (0.60f), `glow` (0.75f), `muted` (0.60f), `subtle` (0.12f). |
| **MotionTokens** | `MotionTokens.kt` | Durations (`durationShort1` to `durationExtraLong2`), easing curves (`easingStandard`, `easingStandardDecelerate`, `easingStandardAccelerate`), and spring specs (`springPress`, `springEnter`, `springExit`). |

---

## 3. Palettes & Contrast (WCAG AA)

ShoutSend provides two factory token palettes defined in `DefaultTokens.kt`:
1. **Cyber Radio Dark (Default):** Deep blue-gray surfaces (`#0B0E14`), neon cyan primary (`#00F0FF`), neon green secondary (`#00FF66`), amber tertiary (`#FFB300`), warm red error (`#FF5449`), high-contrast dark error label (`#410002`).
2. **Cyber Radio Light:** Crisp light blue-gray surfaces (`#F7F9FE`), deep cyan primary (`#006874`), dark green secondary (`#006E26`), dark gold tertiary (`#7A5900`), crimson error (`#BA1A1A`).

### Key Contrast Ratios (WCAG AA requires >= 4.5:1)
- **Cyber Radio Dark**:
  - `onPrimary` on `primary`: **9.35 : 1** (Passes AAA)
  - `onSecondary` on `secondary`: **9.72 : 1** (Passes AAA)
  - `onTertiary` on `tertiary`: **7.31 : 1** (Passes AAA)
  - `onError` on `error`: **5.40 : 1** (Passes AA)
  - `onBackground` on `background`: **15.54 : 1** (Passes AAA)
  - `onSurface` on `surface`: **15.54 : 1** (Passes AAA)
- **Cyber Radio Light**:
  - `onPrimary` on `primary`: **6.50 : 1** (Passes AA)
  - `onSecondary` on `secondary`: **6.43 : 1** (Passes AA)
  - `onBackground` on `background`: **16.34 : 1** (Passes AAA)
  - `onSurface` on `surface`: **16.34 : 1** (Passes AAA)

---

## 4. Component Wrappers

All wrappers live in `app.shoutsend.core.designsystem.components.*` and forward directly to Material 3 primitives while locking defaults to `ChirpTheme` tokens:

- **Buttons:** `CdButton`, `CdTonalButton`, `CdOutlinedButton`, `CdTextButton`
- **Icon Buttons:** `CdIconButton`, `CdFilledIconButton`, `CdTonalIconButton`, `CdOutlinedIconButton`
- **Cards:** `CdCard`, `CdOutlinedCard`, `CdElevatedCard` (both static and clickable variants)
- **Input Fields:** `CdTextField`, `CdOutlinedTextField`
- **Dialogs:** `CdAlertDialog`
- **Bottom Sheets:** `CdModalBottomSheet`
- **Top Bars:** `CdTopAppBar`, `CdCenterAlignedTopAppBar`, `CdLargeTopAppBar`
- **Chips:** `CdAssistChip`, `CdFilterChip`, `CdInputChip`, `CdSuggestionChip`
- **Toggles:** `CdSwitch`
- **Feedback:** `CdSnackbarHost`, `CdSnackbar`
- **Dividers:** `CdHorizontalDivider`, `CdVerticalDivider`
- **Layout:** `CdScaffold`

---

## 5. How-To Guides

### How to Access Tokens in Compose
Use the static accessors on `ChirpTheme`:
```kotlin
Column(
    modifier = Modifier
        .background(ChirpTheme.colors.surface)
        .padding(ChirpTheme.spacing.md)
) {
    Text(
        text = "18.5 kHz Carrier",
        style = ChirpTheme.typography.radioReadoutMedium,
        color = ChirpTheme.colors.primary
    )
}
```

### How to Add a New Token
1. If adding a new role/slot, declare the property in the respective token data class (e.g., `ColorTokens.kt`, `SizeTokens.kt`).
2. Add the factory value to both `CyberRadioDark` / `CyberRadioLight` and the token instances in `DefaultTokens.kt`. This is the ONLY place where raw numeric or hex literals may be written.
3. Update `DesignSystemTest.kt` to assert that the token is present and non-null.

### How to Add a New Component Wrapper
1. Create `Cd<Name>.kt` in `app/src/main/java/app/shoutsend/core/designsystem/components/`.
2. Wrap the corresponding Material 3 component.
3. Set default arguments to reference `ChirpTheme` tokens (`ChirpTheme.colors`, `ChirpTheme.shapes`, `ChirpTheme.spacing`, `ChirpTheme.strokes`).
4. Ensure zero raw literals exist in the file (`.dp`, `.sp`, `Color(0x...)`, `RoundedCornerShape`).
5. Create Compose previews in `app/src/dev/java/app/shoutsend/core/designsystem/previews/` demonstrating Dark and Light themes.

---

## 6. Token Overrides & Persistence Architecture

In the developer flavor (`dev`), tokens can be modified live at runtime and persisted across application restarts. The production user flavor (`user`) binds a zero-overhead no-op repository, guaranteeing complete source purity.

### Architecture Flow

```text
 Base DesignTokens (Cyber Radio / Dynamic)
          │
          ▼
   [ThemeController] ◄── (overrides: Flow<TokenOverrides>) ── [TokenOverridesRepository]
          │                                                              │
          │                                                 ┌────────────┴────────────┐
          ▼ (StateFlow<DesignTokens>)                       │ (dev flavor)            │ (user flavor)
    [ChirpTheme]                                            ▼                         ▼
          │                                       [DevDataStoreRepository]   [NoOpRepository]
          ▼                                                 │                         │
     Compose UI                                    [Preferences DataStore]       (No Storage)
                                                            │
                                                     (300ms debounce)
```

### Override Characteristics
1. **Sparse Storage:** Only explicitly modified tokens are recorded in `TokenOverrides`. Unmodified tokens inherit from factory defaults, allowing baseline tokens to update without corrupting user customizations.
2. **Instant Recomposition (Rule 11):** Calling `controller.setColor()` or `controller.setRadius()` mutates the in-memory state immediately, updating `ThemeController.tokens` within the current frame.
3. **Debounced Disk Writes:** DataStore JSON writes are debounced by 300 ms to collapse rapid slider or color picker drags into a single persistence operation.
4. **Resilience & Validation:** Malformed or corrupt JSON payloads fall back gracefully to factory defaults with a non-sensitive log warning. Unknown token keys and out-of-range radii are strictly rejected.
5. **Instant Reset:** `resetAll()`, `resetColor(role)`, and `resetRadius(name)` purge overrides and restore baseline tokens immediately.

---

## 7. Developer Panel & Tooling (Dev Flavor Only)

The Developer Panel lives exclusively in `src/dev` (`app.shoutsend.core.devtools`) and allows real-time interactive tuning of all design tokens.

### Panel Features
1. **Floating Edge Handle:** A draggable pill on the right screen edge (`FloatingDevHandle`) attached through the `DevTools` seam.
2. **Colors Tab:** Lists all 35 token roles organized into logical categories (Primary, Secondary, Tertiary, Error, Surfaces, Outlines, Extended Signal). Each item provides:
   - Interactive Color Swatch.
   - Expandable H, S, B sliders (0–360° Hue, 0–100% Saturation, 0–100% Brightness).
   - Hex text input field with real-time validation.
   - Individual "Reset" button when modified from factory default.
3. **Shapes & Radii Tab:** Sliders for each corner radius token (`extraSmall` to `extraLarge`) and live preview components (`CdButton`, `CdCard`, `CdTextField`).
4. **Escape Hatch Readability Guard:** The panel contains a fixed "Reset All" button styled with un-overridable baseline tokens (`DefaultTokens.CyberRadioDark.errorContainer` and `onErrorContainer`). Even if a developer configures low-contrast or illegible colors (e.g. black text on black background), the escape hatch remains legible and operable.
5. **JSON Export & Import:** Full clipboard integration to export the active sparse `TokenOverrides` JSON or paste an imported token set with schema validation.
