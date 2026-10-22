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
- `:app`: Android application module with multi-flavor setup (`dev` and `user`). Depends on `:modem`.
- `:modem`: Pure Kotlin JVM module (`explicitApi()` enabled) for audio synthesis, DSP, error correction, codecs, and channel simulation. 100% free of Android SDK dependencies.

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
- Step 6 completed: Pure Kotlin JVM `:modem` module scaffolded with `explicitApi()` enabled. Created `AudioSpec` (48kHz & 44.1kHz), test harness (`SeededRandom`, `TonePcm`, `PcmMath`, `PcmBufferBuilder`), and `checkModemPurity` guard in `buildSrc` (proven with failure run). Formalized Protocol v1 in `docs/PROTOCOL.md`. Smoke tests passing in `:modem:test`.
- Step 7 completed: Acoustic profiles and tone plans implemented (`Band`, `Scheme`, `Speed`, `ModemProfile`, `TonePlan`, `Timing`, `SymbolMapper`, `ModeBlockCodec`, `ProtocolConstants`, `ProfileMath`). Verified Nyquist margins, tone spacings, symbol mapping roundtrips, 100% single-bit error detection across 192 mode permutations, zero-side-lobe sync word, and Protocol v1 raw rates.
- Step 8 completed: Bit utilities, CRC-16, and whitening implemented (`BitWriter`, `BitReader`, `Crc16`, `Whitener`, `ByteUtils`). Verified known-answer CRC-16 (0x29B1 on '123456789'), PN9 LFSR ($x^9+x^5+1$) scrambling statistical uniformity (max run = 3 symbols <= 6, chi-square 6.67-12.80), and constant-time equality.
- Step 9 completed: Galois Field $GF(2^8)$ and systematic Reed-Solomon encoder implemented (`Gf256`, `RsEncoder`, `RsProfile`). Precomputed generator polynomials for LIGHT RS(60,54), STANDARD RS(60,48), STRONG RS(60,40). Verified field axioms, 3,000 zero-syndrome codewords, linearity, shortened codewords, and benchmark of 103,341 codewords/sec (5.91 MB/s).
- Step 10 completed: Errors-and-erasures Reed-Solomon decoder implemented (`RsDecoder`, `RsDecodeResult`). Supports arbitrary $2v + \mu \le nsym$ corrections with Berlekamp-Massey initialized from erasure locator, Chien search, and Forney algorithm with $b=0$ factor. Verified 6,000 random codewords, full erasure correction, 10,000 beyond-capacity trials with 0.0000% miscorrections (< 1.0%), and decode speeds up to 100,932 codewords/sec.
- Step 11 completed: Codeword symbolizer and erasure mapping implemented (`CodewordSymbolizer`, `SymbolizerResult`). Formally verified burst damage ceiling ($\le \lceil L \cdot \text{bitsPerSymbol} / 8 \rceil + 1$) for MFSK16 and DTFSK. Rejected intra-codeword interleaving with mathematical justification. Evaluated 500-message trials per SER proving erasure marking improves message recovery by up to $+63.6\%$ under noise.

