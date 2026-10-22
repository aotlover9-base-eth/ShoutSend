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

## 2026-10-22 - Step 8: Bit Utilities, CRC-16, and Whitening Decisions

### DEC-025: CRC-16/CCITT-FALSE Specification
- **Decision**: Adopted standard CRC-16/CCITT-FALSE with:
  - Generator polynomial: `0x1021` ($x^{16} + x^{12} + x^5 + 1$)
  - Initial value: `0xFFFF`
  - RefIn / RefOut: `false`
  - Final XOR: `0x0000`
- **Verification**: Verified against mandatory known answer: ASCII `"123456789"` yields `0x29B1`.

### DEC-026: PN9 LFSR Spectral Whitening Specification
- **Decision**: Implemented PN9 LFSR with polynomial $x^9 + x^5 + 1$ (mask `0x1FF`).
- **Initial State**: Derived deterministically from frame seed via `((seed and 0x0F) shl 5) xor 0x1FF`.
- **Properties**: Verified on all-zero 60-byte payload across seeds 0..15: maximum identical consecutive symbol run is 3 (limit <= 6), and chi-square goodness of fit against uniform 16-symbol distribution ranges from 6.67 to 12.80.

## 2026-10-22 - Step 9: Reed-Solomon Field and Encoder Decisions

### DEC-027: Galois Field GF(2^8) Parameter Selection
- **Decision**: Implemented GF(256) over primitive polynomial `0x11D` ($x^8 + x^4 + x^3 + x^2 + 1$) with generator element $lpha = 2$.
- **Optimization**: Exponentiation lookup table is doubled to 512 entries, avoiding modulo reduction during multiplications.

### DEC-028: Reed-Solomon Code Profiles and Generator Polynomials
- **Decision**: Three systematic error correction profiles defined:
  - `LIGHT` (ID 0): $RS(60, 54)$ — corrects up to 3 byte errors ($nsym = 6$).
  - `STANDARD` (ID 1): $RS(60, 48)$ — corrects up to 6 byte errors ($nsym = 12$) (Default).
  - `STRONG` (ID 2): $RS(60, 40)$ — corrects up to 10 byte errors ($nsym = 20$).
- **Roots**: Generator polynomial roots are $lpha^0, lpha^1, \dots, lpha^{nsym-1}$ (first consecutive root 0).
- **Shortening**: Full support for shortened codewords ($L < k$) with zero-padding equivalence.

## 2026-10-22 - Step 10: Reed-Solomon Errors-and-Erasures Decoder Decisions

### DEC-029: Errors-and-Erasures Decoder Architecture and Capacity
- **Decision**: Implemented errors-and-erasures decoding using:
  1. Erasure locator polynomial $\Lambda_{erasures}(x) = \prod_{p \in E} (1 - X_p \cdot x)$.
  2. Berlekamp-Massey algorithm initialized from $\Lambda_{erasures}(x)$ stepping through remaining syndromes.
  3. Chien root search with strict degree check ($	ext{roots} == 	ext{deg}(\Lambda)$).
  4. Forney algorithm incorporating the $X_l^{1-b} = X_l$ scaling factor for roots with offset $b=0$.
  5. Mandatory post-correction syndrome re-computation.
- **Capacity**: Corrects any combination satisfying $2v + \mu \le nsym$.
- **Integrity**: Never throws exceptions on malformed input. In 10,000 uncorrectable trials beyond capacity, the measured miscorrection rate was 0.0000% (100.0% honest failures).

## 2026-10-22 - Step 11: Codeword Symbolizer, Erasure Mapping, and Interleaving Rejection

### DEC-030: Rejection of Intra-Codeword Interleaving and Erasure Mapping Strategy
- **Decision**: Intra-codeword interleaving was explicitly considered and rejected.
- **Rationale**: Reed-Solomon operates natively on bytes over GF(256). Physical acoustic echo and multipath interference corrupt contiguous sequences of audio symbols. Because symbols naturally map to adjacent bytes in the un-interleaved stream, a short burst error only corrupts a small number of adjacent bytes (e.g. an $L$-symbol burst damages at most $\lceil L \cdot \text{bitsPerSymbol} / 8 \rceil + 1$ bytes). Interleaving symbols within a single codeword would scatter burst damage across many distinct bytes throughout the entire codeword, multiplying the number of corrupted bytes and exceeding RS correction capacity. Acoustic robustness is achieved through short frames, PN9 whitening, repeat transmissions, and symbol-level erasure marking.
- **Erasure Mapping**: For DTFSK (where 6-bit symbols cross 8-bit byte boundaries), any symbol with confidence below the threshold ($0.5$) marks all bytes it overlaps ($\lfloor 6s/8 \rfloor$ through $\lfloor (6s+5)/8 \rfloor$) as Reed-Solomon erasures. Evaluated 500-message trials proved this boosts recovery rate by up to $+63.6\%$ under noise.

## 2026-10-22 - Step 12: Packet Format, Fragmentation, Reassembly, and Air-Time

### DEC-031: Frame Structure, Fragmentation, Reassembly, and Air-Time Modeling
- **Frame Layout**: Standardized 4-byte header:
  - Byte 0: version (bits 7..6 = 1), type (bits 5..3 = `MessageType`), flags (bits 2..0 = encrypted, compact, reserved).
  - Byte 1: message ID (0..255).
  - Byte 2: fragment index (bits 7..4 = 0..15), fragment count (bits 3..0 = `count - 1` representing 1..16).
  - Byte 3: payload length $L \in [0, k - 6]$.
  - Bytes 4 until $4 + L - 1$: payload slice.
  - Bytes $4 + L$ until $k - 3$: zero padding.
  - Bytes $k - 2$ .. $k - 1$: CRC-16/CCITT-FALSE over bytes $0 \dots 4 + L - 1$ (header and payload).
- **Limits**: `MAX_AIR_BYTES = 320`, `MAX_FRAGMENTS = 16`. Empty payloads are rejected.
- **MessageAssembler**: Injected clock, 60s timeout for incomplete message purging, duplicate frame rejection, stream conflict detection with state reset, and exactly-once delivery semantics.
- **AirTime**: Pure calculation: $T = 0.84\text{s} + (\text{frameCount} \cdot \text{repeats} \cdot (4 + \text{codewordSymbols}) \cdot T_{\text{symbol}})$. Verified against hand-computed test cases.

## 2026-10-23 - Step 13: Payload Model, Codecs, and URL Safety Analyzer

### DEC-032: Sealed Payload Model, QR/TLV Codecs, and Privacy Guards
- **Model**: Sealed interface `Payload` (`Text`, `Url`, `WiFi`, `Contact`).
- **Privacy Guard (Rule 25)**: All `toString()` implementations mask sensitive fields (`WiFi.password` masked as `***`, `Contact` hides detailed phone/email numbers, `Text` and `Url` summarize without raw values in logs). `PayloadValidationError` reasons NEVER contain user input values.
- **Wire Codecs**:
  - `Text`: UTF-8 (max 280 bytes).
  - `Url`: UTF-8 (max 280 bytes, scheme http/https only, control characters rejected, valid host required).
  - `WiFi`: Standard Wi-Fi QR format `WIFI:T:<WPA|nopass>;S:<ssid>;P:<password>;H:<true|false>;;` with full escaping of `\`, `;`, `,`, `:`, `"`.
  - `Contact`: Binary TLV format (tags 0x01 name, 0x02 phone, 0x03 email).
- **UrlSafety**: Pure URL heuristic evaluator checking `userInfo` deception, IPv4/IPv6 literals, Punycode/Unicode homoglyphs, non-standard ports, and URL length > 200 characters.

## 2026-10-23 - Step 14: Payload Compaction and Chooser

### DEC-033: Domain-Specific Compaction and Shortest-Representation Chooser
- **Rationale**: Audio transmissions take ~0.05 seconds per byte. Every byte saved directly reduces chirp duration and acoustic exposure. General-purpose compression libraries (e.g. gzip/brotli) inflate small payloads with metadata headers and dictionaries; dedicated micro-compactors achieve up to 60.7% savings with 0-byte header overhead.
- **URL Compaction**: 16-entry prefix table + 16-entry suffix table with 2-byte dictionary indices.
- **7-Bit Text Packing**: Packs 7-bit ASCII characters without padding bits.
- **Wi-Fi Compaction**: 1-byte header bitfield + 7-bit packed ASCII SSID and password.
- **Contact Compaction**: 4-bit BCD packing for digit-only phone numbers (40% savings) and 7-bit packing for formatted phone numbers.
- **Chooser Guarantees**: `PayloadCompactor` evaluates candidate compact size vs canonical size. Compact is chosen ONLY when strictly shorter (`compactSize < canonicalSize`), setting the `isCompact` frame flag. Payloads where compaction provides no savings (e.g. non-ASCII Hindi text) remain canonical with zero penalty.

## 2026-10-23 - Step 15: Secure Mode Cryptography

### DEC-034: Authenticated Encryption Specification, Security Model, and Trade-offs
- **Rationale**: Anyone in acoustic proximity can capture soundwaves with an everyday microphone. Secure Mode offers end-to-end passphrase protection for users who require confidentiality over an open acoustic channel.
- **Threat Model & Limits**: Because an adversary can record chirps and mount unbounded offline dictionary and brute-force attacks against the 60,000-iteration PBKDF2 hash, short or low-entropy passphrases provide weak security. `PassphraseEvaluator` actively warns users regarding dictionary vulnerability on weak choices.
- **Cryptographic Suite**:
  - KDF: `PBKDF2WithHmacSHA256` with 60,000 iterations and a 4-byte random salt.
  - AEAD: `AES-256-GCM` with a 12-byte random nonce and a 12-byte (96-bit) authentication tag.
  - AAD Binding: Associated data contains `[version (0x01), typeId]`, cryptographically preventing cross-type ciphertext manipulation.
  - Overhead: Exactly 28 bytes (`salt [4] + nonce [12] + tag [12]`).
- **Pipeline Order**: Compaction runs BEFORE encryption, maximizing compression ratio before high-entropy ciphertext generation.
- **Ephemeral State**: All derived key arrays are zeroed in `finally` blocks; passphrases are never persisted.

## 2026-10-23 - Step 16: Tone Synthesizer and Windowing

### DEC-035: Absolute-Phase Oscillators, Hann Overlap Ramps, and Chunk-Independent Synthesis
- **Absolute Oscillator Phase**: Rather than tracking stateful per-symbol oscillator phase accumulators, all audio samples evaluate $s(n) = \sin(2\pi \cdot f \cdot n / F_s)$ using the absolute sample index $n$. This guarantees phase continuity across identical repeated tones, enables stream seeking, and guarantees that chunk rendering produces bit-identical output regardless of chunk size (1, 7, 480, or 10,000 samples).
- **Hann Overlap Ramps**: Adjacent symbols overlap by exactly $R$ samples (where $R \le L/2$). The outgoing symbol fades out with $w_{\text{down}}(i) = \frac{1 + \cos(\pi (i + 0.5) / R)}{2}$ and the incoming symbol fades in with $w_{\text{up}}(i) = \frac{1 - \cos(\pi (i + 0.5) / R)}{2}$. Because $w_{\text{up}}(i) + w_{\text{down}}(i) \equiv 1.0f$, transitions between identical repeated tones exhibit zero amplitude dip ($< 0.05$ dB measured), while transitions between disparate tones are smooth and click-free.
- **Zero-Allocation Hot Path (Rule 22)**: `RampTable` precomputes `rampUp` and `rampDown` arrays once per ramp length and caches them in `RaisedCosineWindow`. `ToneSynth.render` writes directly into caller-provided float arrays with zero heap allocation per sample or symbol.
- **Linear PCM Conversion**: `PcmConverter` performs round-to-nearest conversion with strict clamp to $[-32768, 32767]$ without dither, with symmetric full scale representation (+1.0f -> 32767, -1.0f -> -32768).

## 2026-10-23 - Step 17: Modulator

### DEC-036: Physical Transmission Assembly, Fixed Whitening Seed, and Hardware Silence Margins
- **Fixed Whitening Seed Constant**: `WHITEN_SEED_V1 = 0xA5` is chosen as a fixed non-zero LFSR seed across all transmitted frames. Rationale: Because fragment indices reside inside the encoded and whitened frame header, a receiver cannot know the fragment index prior to de-whitening and decoding the frame header. A single constant seed eliminates this circular dependency.
- **Hardware Silence Margins**: Real speaker drivers (especially on mobile devices) require an analog turn-on settling ramp before audio playback begins, and analog audio output sinks drop the tail when buffers end abruptly. By default, `TransmissionBuilder` prepends 250 ms lead-in silence and appends 150 ms tail silence as zero-amplitude PCM samples. Both values are configurable.
- **AirTime Calibration**: `AirTime.estimate` was extended with optional `leadInSilenceMs` (default 0) and `tailSilenceMs` (default 0) parameters to retain exact backwards-compatibility with Steps 12-15 while matching `Modulator.durationSeconds` within one symbol period across all schemes, speeds, and payload counts.
- **Zero-Allocation Hot Path Verification**: Direct array indexing for ramp tables in `RaisedCosineWindow` (`directTable[rampLength]`) eliminates boxing and lambda overhead on the rendering hot path. Verified that streaming 10 seconds of continuous audio emits 0 bytes of heap allocation in steady state.

## 2026-10-23 - Step 18: Goertzel Detector

### DEC-037: Generalized Goertzel Filter Bank, Coherent Hann Calibration, and Streaming Ring Buffer
- **Generalized Goertzel Coefficients**: Goertzel resonators compute the filter coefficient $2\cos(2\pi f / F_s)$ using the exact target acoustic frequency $f$ rather than rounding to discrete FFT bins. This allows arbitrary tone placements (e.g. 17,450 Hz, 18,350 Hz) and guarantees exact tone tuning across both 48 kHz and 44.1 kHz sample rates.
- **Hann Window & Coherent Gain Normalization**: Precomputed Hann analysis windows $w[n] = 0.5 (1 - \cos(2\pi (n + 0.5)/N))$ suppress spectral leakage from adjacent tones. Because the coherent gain of a Hann window is $\sum w[n] = N/2$, the raw squared resonator magnitude $|X|^2$ is normalized by $\frac{16}{N^2}$ (and $\frac{4}{N^2}$ for unwindowed mode). A pure sine wave of peak amplitude $A$ produces reported power $A^2$ within 1%.
- **Measured Crosstalk Rejection**: Full $16 \times 16$ crosstalk leakage matrices proved that off-diagonal leakage is $\le -56.7$ dB (Robust), $\le -43.4$ dB (Normal), and $\le -31.0$ dB (Fast) for Retro, and $\le -44.5$ dB (Normal) and $\le -48.5$ dB (Fast) for Stealth, far exceeding the $\le -25$ dB / $\le -20$ dB roadmap thresholds.
- **Streaming Ring Buffer (`BlockDetector`)**: Ingestion decouples incoming chunk sizes (e.g. 1024-sample AudioRecord buffers) from the DSP analysis block size ($N$) and hop interval, emitting blocks via `EnergyListener` with 0 heap allocations per block.

## 2026-10-23 - Step 19: Noise Floor Tracker and SNR

### DEC-038: Asymmetric Noise Floor Tracking, Freezing Lock, and Dynamic Thresholding
- **Asymmetric Smoothing Rationale**: Acoustic environments (cafes, streets) feature sudden impulse noises and speech bursts that must not rapidly desensitize the modem. Setting a slow upward time constant ($\tau_{\text{attack}} = 1.5$s) and a fast downward time constant ($\tau_{\text{decay}} = 0.25$s) forces the tracker to follow the true quiet troughs beneath ambient bursts.
- **Acquisition Freezing Lock**: During transmission preamble locking and frame reception, the tracker is explicitly frozen (`freeze()`). Evaluated tests confirmed that an unfrozen tracker would raise the noise floor by $>10\times$ within 3 seconds of loud tone reception, severely degrading sensitivity for subsequent symbols.
- **Detection Thresholding**: Minimum detection power is bounded by $\max(\text{floor} \cdot 10^{\text{margin}/10}, \text{minFloorPower} \cdot 10^{\text{margin}/10})$. Setting `minFloorPower` to $-70$ dBFS ensures that digital silence or near-zero noise cannot cause false triggers on quantization flutter.
- **Confidence Calibration**: Demodulator confidence is formulated as the product of winner-to-runner separation $\frac{E_{\text{win}} - E_{\text{run}}}{E_{\text{win}}}$ and SNR headroom $\frac{\text{SNR} - \text{margin}}{12.0}$. This provides a continuous $[0, 1]$ metric used by Step 21 to mark symbol erasures when confidence drops below $0.5$.

## 2026-10-23 - Step 20: Preamble Detection and Symbol Synchronization

### DEC-039: 4-Phase Quarter-Symbol Scanning, Expected-Energy Sync Correlation, and Power-of-2 Ring Buffer
- **4-Phase Quarter-Symbol Scanning**: The receiver scans continuous streaming audio at quarter-symbol hops (360 samples at 48 kHz). Across four interleaved hop phases, the phase maximizing the alternating S0/S1 contrast over 6 symbol slots is identified.
- **Single-Tone False Lock Rejection**: Preamble candidate requires at least 6 consecutive alternating symbols, with both S0 and S1 detected at least twice. Steady single-tone emissions (S0, S1, or data tones) cannot satisfy this condition and produce zero false locks.
- **Power-of-2 Ring Buffer with Chunked Ingestion**: The audio buffer uses a fixed capacity of 65,536 samples ($2^{16}$) with bitwise mask indexing (`S & 65535L`). `push()` accepts arbitrary sample block sizes and internally chunks processing into $\le 2048$ samples, ensuring audio cannot overwrite unprocessed samples while waiting for sync word completion.
- **Sync Word Correlation Boundary Selection**: During sync verification, candidate boundaries ($T - \text{hopSize}, T, T + \text{hopSize}$) are tested by calculating the cumulative Goertzel energy of expected sync word data tones (`SYNC_WORD_PATTERN`). The boundary maximizing expected-tone energy correlation is selected, achieving sub-hop symbol boundary alignment within $\pm 0.17$ symbol periods across 200 random start offsets and high noise levels.
- **Timing Drift Tracking**: In the `LOCKED` state, `TimingTracker` applies early/late loop energy correction on 4-symbol frame markers, maintaining lock across $\pm 100$ ppm sample rate clock drift.

## 2026-10-23 - Step 31: Vault Storage (Room + Keystore)

### DEC-041: AES-256-GCM Encrypted Room Vault with Graceful Degradation and Secret Masking
- **Hardware-Backed Keystore Encryption (Rule 25)**: Plaintext payloads are never written to disk. Payloads are encrypted with AES-256-GCM (`shoutsend_vault_v1`) producing an initialization vector (12 bytes) and ciphertext with authentication tag before insertion into SQLite.
- **Secret Masking**: `VaultItem.toString()` strictly suppresses payload contents, SSIDs, passwords, and decrypted fields, outputting only message type and metadata.
- **Graceful Unreadable Degradation**: When decryption fails (corrupted data or missing key), the entry is flagged `isUnreadable = true` and `payload = null`, allowing UI feeds to display an unreadable placeholder rather than crashing.
- **Starred Preservation Pruning**: `VaultDao.prune(keepCount)` only deletes unstarred entries older than the top `keepCount` newest entries, guaranteeing starred messages persist indefinitely regardless of history limits.












