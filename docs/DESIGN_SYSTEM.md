# ShoutSend Design System (v2 Material 3 Expressive)

## 1. Architecture & Core Philosophy
ShoutSend UI is built strictly on a tokenized design system conforming to **Material 3 Expressive** (`androidx.compose.material3:material3:1.4.0-alpha08`). It replaces the dark legacy "cyber-radio" visual language with an expressive, tactile, and human-centric design.

### Non-Negotiable Rules
- **Rule 6 (Single Hex File):** Exactly ONE file in the entire repository may contain raw hex color values or `Color(0x...)`: [`DefaultTokens.kt`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/DefaultTokens.kt).
- **Rule 7 (No Radii or Measurement Literals):** Numeric `dp`, `sp`, and millisecond literals are forbidden in UI code outside `DefaultTokens.kt`. All margins, paddings, corner radii, strokes, icon sizes, and durations must reference design tokens.
- **Rule 8 (Single Design System):** All application UI consumes tokens via [`ShoutSendTheme`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/ShoutSendTheme.kt) (and its backward-compatible alias `ChirpTheme`).
- **Rule 9 (Consistent Component Wrappers):** Screens and features must never call unstyled Material 3 composables directly. They must use the corresponding [`Shout*`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components) wrapper (or legacy `Cd*` alias).

---

## 2. Token Groups

All tokens are modeled as immutable Kotlin data classes in `app.shoutsend.core.designsystem`:

| Token Group | Interface / Class | Responsibilities |
| --- | --- | --- |
| **ColorTokens** | [`ColorTokens.kt`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/ColorTokens.kt) | Material 3 Expressive ColorScheme wrapper + harmonized ExtendedColors (success, warning) + dynamic waterfall palette. |
| **ShapeTokens** | [`ShapeTokens.kt`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/ShapeTokens.kt) | M3 Expressive shape scale (`none`, `extraSmall`, `small`, `medium`, `large`, `extraLarge`, `full`) + component shapes + `ShoutShapes` hero shapes (`Cookie9Sided`, `Sunny`, `Clover4Leaf`, `Pill`, `SoftBurst`, `Heart`, `Flower`). |
| **TypographyTokens** | [`TypographyTokens.kt`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/TypographyTokens.kt) | Standard M3 type scale without monospace fonts (`displayLarge` to `labelSmall`). |
| **SpacingTokens** | `SpacingTokens.kt` | Strict 7-step scale: `xxs` (2dp), `xs` (4dp), `sm` (8dp), `md` (16dp), `lg` (24dp), `xl` (32dp), `xxl` (48dp). |
| **StrokeTokens** | `StrokeTokens.kt` | Outlines, dividers, and canvas borders: `hairline` (0.5dp), `thin` (1dp), `regular` (2dp), `thick` (4dp). |
| **SizeTokens** | `SizeTokens.kt` | Icon dimensions (`iconXs` to `iconXl`), minimum touch targets (`touchTargetMin` = 48dp), and heights (`buttonHeight`, `textFieldHeight`, `topBarHeight`, `navBarHeight`). |
| **ElevationTokens** | `ElevationTokens.kt` | Shadow and tonal elevation: `level0` (0dp) through `level5` (12dp). |
| **OpacityTokens** | `OpacityTokens.kt` | Transparency multipliers: `disabled` (0.38f), `scrim` (0.60f), `muted` (0.60f), `subtle` (0.12f). |
| **MotionTokens** | `MotionTokens.kt` | Expressive durations (`durationShort1` to `durationExtraLong2`), easing curves (`easingStandard`, `easingEmphasized`), and expressive spring specs. |
| **RadioVisualTokens** | `RadioVisualTokens.kt` | Acoustic visualization tokens for rings, pulses, and waterfall canvases. |

---

## 3. Dynamic Color & Seed Presets

The system uses `materialkolor:2.0.2` to generate complete, accessible Material 3 color schemes dynamically from a single seed color or system dynamic color (Android 12+ wallpaper extraction).

### Factory Presets
- **Violet (Default):** `#6750A4` — Deep purple / expressive primary seed.
- **Coral:** `#BA1A1A` — Energetic warm crimson.
- **Forest:** `#386A20` — Organic earth green.
- **Ocean:** `#006874` — Vibrant maritime cyan.
- **Amber:** `#7A5900` — Warm golden amber.

### Extended Harmonized Colors
- **Success:** Semantic green (`0xFF1B6D24L` seed) harmonized with primary color.
- **Warning:** Semantic amber (`0xFF7D5700L` seed) harmonized with primary color.

### Waterfall Palette & Colormap
The spectrogram waterfall palette is dynamically derived from `colorScheme`:
- Low intensity: `surfaceContainerLowest`
- Mid intensity: `primary`
- High intensity: `tertiary`
Interpolation uses linear gamma correction to prevent dark midtone dips and ensure perceptual luminance progression across dark and light themes.

---

## 4. Hero Shapes & Expressive Morphing

`ShoutShapes` provides distinctive, organic shapes created using Material 3 Expressive `RoundedPolygon`:
1. `Cookie9Sided`: 9-sided cookie polygon with smooth rounded corners.
2. `Sunny`: 8-pointed warm sunshine star polygon.
3. `Clover4Leaf`: 4-lobed expressive clover polygon.
4. `Pill`: Smooth stadium/pill polygon.
5. `SoftBurst`: 12-pointed burst polygon.
6. `Heart`: Expressive heart polygon.
7. `Flower`: 6-petaled bloom polygon.

`MorphingShape` allows smooth transitions between any two hero shapes driven by an animation progress float `[0f..1f]` using Compose `Morph`.

---

## 5. Component Wrappers

All components are centralized in `app.shoutsend.core.designsystem.components.*`:

- **Buttons:** [`ShoutButton`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutButton.kt) (`Filled`, `Tonal`, `Outlined`, `Text`), [`ShoutIconButton`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutIconButton.kt), [`ShoutFab`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutFab.kt), [`ShoutButtonGroup`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutButtonGroup.kt).
- **Cards & Lists:** [`ShoutCard`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutCard.kt) (`Filled`, `Outlined`, `Elevated`), [`ShoutListItem`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutListItem.kt).
- **Inputs & Toggles:** [`ShoutTextField`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutTextField.kt) (`Outlined`, `Filled`), [`ShoutSwitch`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutSwitch.kt), [`ShoutChip`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutChip.kt).
- **Overlays & Dialogs:** [`ShoutBottomSheet`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutBottomSheet.kt), [`ShoutDialog`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutDialog.kt).
- **Navigation & Bars:** [`ShoutTopAppBar`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutTopAppBar.kt), [`ShoutNavigationBar`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutNavigationBar.kt), [`ShoutSnackbarHost`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutSnackbarHost.kt).
- **Progress & Feedback:** [`ShoutLoadingIndicator`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutLoadingIndicator.kt) (expressive morphing indicator), [`ShoutProgress`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutProgress.kt) (wavy expressive linear progress), [`ShoutEmptyState`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ShoutEmptyState.kt).
- **Acoustic Feedback:** [`BrandMark`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/BrandMark.kt), [`LevelMeter`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/LevelMeter.kt), [`SignalMeter`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/SignalMeter.kt), [`ByteMeter`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/ByteMeter.kt), [`StatusPill`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/StatusPill.kt), [`SpeedSelector`](file:///mnt/HDD1/Downloads/ShoutSend/app/src/main/java/app/shoutsend/core/designsystem/components/SpeedSelector.kt).

*Note: Legacy `Cd*` wrappers are retained as forwarding aliases to `Shout*` components for seamless transition across steps R3-R6.*

---

## 6. Developer Panel (Dev Flavor Only)

Located in `app/src/dev/java/app/shoutsend/core/devtools/DeveloperPanel.kt`, the panel provides live tuning:
- **Theme & Colors:** Dynamic color switch, theme mode (System/Dark/Light), preset seed color swatches, hue & saturation sliders, and contrast level slider.
- **Shapes & Hero:** Hero shape selector with live preview, individual corner radius sliders (`extraSmall`, `small`, `medium`, `large`, `extraLarge`), and JSON export/import.
- **Reset:** "Reset to Defaults" button to instantly restore factory tokens.
Isolated strictly to the `dev` flavor, validated via `verifyUserApkClean`.

---

## 7. How-To Guide

### How to Access Tokens in Compose
```kotlin
@Composable
fun MyFeature() {
    Column(
        modifier = Modifier
            .background(ShoutSendTheme.colors.surface)
            .padding(ShoutSendTheme.spacing.md)
    ) {
        Text(
            text = "Acoustic Transfer",
            style = ShoutSendTheme.typography.titleLarge,
            color = ShoutSendTheme.colors.onSurface
        )
        ShoutButton(
            text = "Send",
            onClick = { /* ... */ }
        )
    }
}
```
