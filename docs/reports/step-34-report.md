# Step 34 Report: ShoutSend Cyber Radio Components and Extra Tokens

**Date**: 2026-10-25  
**Author**: Antigravity Assistant  
**Status**: Completed  
**Branch**: `main`

---

## 1. Executive Summary

Step 34 expands the ShoutSend tokenized design system and introduces the complete suite of specialized "Cyber Radio" UI components. Building directly on the Material 3 foundation established in Step 2, these components furnish the sonic and spectral interface required by all subsequent screens (Send, Receive, Waterfall, History, and Calibration). 

In strict adherence to **Rule 6** (single hex file) and **Rule 7** (no raw measurement or color literals outside tokens), every component consumes dimensions, colors, and motion durations exclusively from `ChirpTheme`. To guarantee high frame rates across low-end Android hardware, glow effects are rendered using multi-pass translucent outlines rather than hardware-taxing blur shaders. Color interpolation for the acoustic waterfall colormap is computed in squared photometric space ($\sqrt{\text{interp}(C_1^2, C_2^2)}$) to prevent mud-banding. System-wide accessibility preferences for reduced motion are honored via `LocalReduceMotion`.

---

## 2. Key Architectural Deliverables

### A. Extended Color Tokens & Default Palettes
- **Acoustic & Colormap Tokens (`ColorTokens.kt`, `DefaultTokens.kt`)**:
  - `signalLow`: Waterfall floor / quiet ambient baseline (`#0D1B2A` dark / `#E0E8F0` light).
  - `signalMid`: Moderate spectral energy (`#0077B6` dark / `#0288D1` light).
  - `signalHigh`: High spectral energy / active carrier tones (`#00F0FF` dark / `#006874` light).
  - `signalPeak`: Saturated signal / Goertzel peak detection (`#FFFFFF` dark / `#004D40` light).
  - `noiseFloorLine`: Dynamic noise tracker line (`#FFB300` dark / `#E65100` light).
  - `syncMarker`: Preamble chirp and frame lock indicator (`#FFFF007F` dark / `#C2185B` light).
  - `toneGuide`: Subdued frequency bin grid divisions (`#1E2D3D` dark / `#CFD8DC` light).
- **Glow, Feedback, and Texture Tokens**:
  - `glowPrimary` & `glowSecondary`: Neon perimeter halo translucent strokes (`#00F0FF`, `#00FF66`).
  - `success` & `onSuccess`: Transfer complete state (`#00FF66`, `#003912`).
  - `warning` & `onWarning`: Near frame limit warning (`#FFB300`, `#412D00`).
  - `scanline`: CRT horizontal raster texture (`0x0A00F0FF`, zero alpha disables rendering).

### B. Physical Radio Visual Tokens (`RadioVisualTokens.kt`)
- `glowRadius`: 12.dp halo perimeter radius.
- `glowAlpha`: 0.24f translucency multiplier.
- `ringStrokeWidths`: 1.5.dp stroke for acoustic wave ripples.
- `ringCount`: 4 concentric rings.
- `ringSpawnIntervalMs`: 650 ms expansion period.
- `waterfallRowsPerSecond`: 30 rows/sec FFT update cadence.
- `waterfallHistorySeconds`: 4.0 seconds temporal spectrogram depth.
- `colormapResolution`: 256 LUT entries.
- `meterSegmentCount`: 12 discrete LED bars.

### C. Gamma-Corrected Colormap Generator (`ColormapFactory.kt`)
- Generates a 256-entry ARGB lookup table interpolating across 4 stops (`signalLow` -> `signalMid` -> `signalHigh` -> `signalPeak`).
- Photometric interpolation is performed in gamma-expanded space ($\sqrt{\text{interp}(C_1^2, C_2^2)}$) to ensure smooth transitions between cyan and green without middle-band desaturation.
- Unit-tested via `ColormapFactoryTest.kt` verifying exact stop boundaries and monotonic perceptual brightness.

### D. Cyber Radio Component Catalog
- **`NeonCard`**: Container card featuring a sharp perimeter outline with an outer translucent halo drawn via `drawBehind` (zero shader blur overhead).
- **`StatusPill`**: Capsule status pill (`IDLE`, `LISTENING`, `LOCKED`, `RECEIVING`, `SUCCESS`, `ERROR`) with an animated pulsing indicator dot.
- **`SignalMeter`**: 12-segment vertical bar meter with color transitions from idle to active, high, and peak, accompanied by monospace SNR dB readout.
- **`LevelMeter`**: Continuous horizontal dBFS meter (-70 dBFS to 0 dBFS) with peak-hold needle and automatic analog clip alert ($\ge -3$ dBFS).
- **`ByteMeter`**: 320-byte payload budget bar displaying byte consumption and estimated on-air transmission time.
- **`ModeSelector`**: Segmented control for switching acoustic bands: Retro (1.5–3.5 kHz) vs Stealth (17.6–19.8 kHz).
- **`SpeedSelector`**: 3-way preset selector for transmission speed (Robust ~16.5s, Normal ~7.2s, Fast ~3.8s).
- **`RadioReadout`**: Large, medium, and small numeric frequency and metric displays formatted in monospace typography.
- **`BrandMark`**: Concentric acoustic wavefront emblem drawn with Compose Canvas primitives.
- **`ScanlineOverlay`**: Full-screen subtle CRT scanline overlay with zero-overhead bypass when disabled.

### E. Developer Panel & Compose Previews
- Updated `DevPanelViewModel.kt` to register new color role groups (`"Acoustic & Waterfall"` and `"Glow & Status"`) and wired dynamic token overrides.
- Created `CyberRadioComponentsPreview.kt` in `app/src/dev/` showcasing all components across Dark and Light Cyber Radio themes.

---

## 3. Verification & Quality Gates

The implementation was verified against all architectural and quality gates:

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
BUILD SUCCESSFUL in 18s

> Task :app:testDevDebugUnitTest
BUILD SUCCESSFUL in 38s (68 tests passing)

> Task :app:testUserDebugUnitTest
BUILD SUCCESSFUL in 38s (68 tests passing)
```

---

## 4. Architectural Rules Compliance
- **Rule 6 (Single Hex File)**: Verified. Raw color hex values exist only in `DefaultTokens.kt`.
- **Rule 7 (No Radii or Measurement Literals)**: Verified. All dimensions and paddings reference tokens.
- **Rule 8 (Single Design System)**: Verified. All components consume tokens via `ChirpTheme`.
- **Rule 15 (Dev/User Separation)**: Verified. Dev previews and dev panel modifications remain strictly in `src/dev`.
- **Rule 17 (Pure JVM Modem)**: Verified. `:modem` contains 0 Android imports.
- **Rule 26 (Offline / Zero Internet)**: Verified. Permissions strictly limited to `RECORD_AUDIO` and `VIBRATE`.
