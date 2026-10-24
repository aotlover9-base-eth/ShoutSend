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

## 6. Permissions & Privacy Indicator
- Strictly `android.permission.RECORD_AUDIO` and `android.permission.VIBRATE`. Zero `INTERNET`.
- Feature: `<uses-feature android:name="android.hardware.microphone" android:required="true" />`.
- **Android 12+ Privacy Indicator (Green Dot)**: When ShoutSend actively listens on the Receive screen or runs acoustic calibration, Android 12+ displays a system privacy green dot indicator in the top status bar. This confirms active foreground microphone capture. ShoutSend never runs background services or listens with screen off. Onboarding (Step 36) explicitly educates the user about this expected indicator.


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
- Step 12 completed: Packet format, fragmentation, reassembly, and air-time estimation implemented (`MessageType`, `MessageFlags`, `FrameHeader`, `Frame`, `FrameEncoder`, `FrameDecoder`, `Fragmenter`, `MessageAssembler`, `AirTime`). Verified canonical payload sizes (1, 41, 42, 43, 84, 320 bytes), rejection of empty and >320 byte payloads, out-of-order reassembly, duplicate suppression, conflict auto-reset, timeout pruning, and hand-calculated air-time estimations.
- Step 13 completed: Payload model and canonical codecs implemented (`Payload`, `PayloadCodec`, `WifiSecurity`, `WifiQrCodec`, `ContactTlvCodec`, `UrlSafety`). Verified unicode roundtrips (emoji, Hindi, French, Japanese, Arabic), full Wi-Fi QR escaping (`\`, `;`, `,`, `:`, `"`), TLV binary contact serialization, URL safety analysis (deceptive userInfo, IPv4/IPv6, Punycode, non-standard port), and 10,000-trial random byte fuzzing with zero uncaught exceptions.
- Step 14 completed: Payload compaction and shortest-size chooser implemented (`SevenBitPacker`, `UrlCompactor`, `WifiCompactor`, `ContactCompactor`, `PayloadCompactor`). Demonstrated $51.0\% - 60.7\%$ savings on web URLs and up to $54.3\%$ savings on Wi-Fi credentials (reducing on-air frame count from 2 frames to 1 frame). Validated 10,000-trial fuzz roundtrips and clean error handling on malformed inputs.
- Step 15 completed: Secure mode cryptography implemented (`SecureBox`, `SecureOpenResult`, `PassphraseEvaluator`). Authenticated encryption via PBKDF2WithHmacSHA256 (60,000 rounds, 61.3ms/op) and AES-256-GCM with 28-byte wire overhead. Verified tamper detection (1-bit flip anywhere fails), AAD type binding, full pipeline integration across Steps 12-14, and ephemeral key zeroing.
- Step 16 completed: Tone synthesizer and windowing implemented (`RaisedCosineWindow`, `SymbolEvent`, `SymbolSchedule`, `ToneSynth`, `PcmConverter`). Verified absolute-phase continuous oscillators, bit-identical rendering across arbitrary chunk sizes (1, 7, 480, 10,000 samples), spectral purity (>40 dB suppression on unselected tones), zero click artifacts (bounded slope < 2.2f), seamless repeated-tone transition (< 0.05 dB RMS variation at boundary), DTFSK equal energy balance, and PCM clipping.
- Step 17 completed: Modulator and physical transmission assembler implemented (`OutgoingMessage`, `TransmissionPlan`, `TransmissionBuilder`, `Modulator`, `WavWriter`). Formalized `WHITEN_SEED_V1 = 0xA5` constant. Verified air-time duration synchronization within one symbol period, energy concentration in preamble S0/S1, sync word Goertzel pattern match, mode block roundtrip, frame marker detection, full Reed-Solomon codeword Goertzel decoding (MFSK16 and DTFSK), repeat consistency, steady-state 0-allocation streaming, and generated audible Retro (`shoutsend_retro_sample.wav`) and Stealth (`shoutsend_stealth_sample.wav`) WAVE files.
- Step 18 completed: Goertzel detector and streaming front-end implemented (`GoertzelBank`, `EnergyListener`, `BlockDetector`). Generalized second-order Goertzel resonators with coherent Hann window power calibration ($A^2$ within 1%). Verified 60 dB amplitude linearity (-60 to -6 dBFS), frequency tolerance (+/- 30 Hz at 89.0% power retention), zero-allocation streaming ring buffer, and complete 16x16 crosstalk leakage matrices across Retro and Stealth (worst leakage -31.0 dB to -62.8 dB, exceeding bounds). Micro-benchmark measured 15.4M samples/sec (321.8x realtime audio).
- Step 19 completed: Noise floor tracker and SNR estimator implemented (`NoiseTrackerParams`, `NoiseFloorTracker`). Verified asymmetric exponential smoothing (decay 0.25s faster than attack 1.5s), white noise floor stability (<3% relative standard deviation), freezing lock preventing signal contamination, digital silence clamping at -70 dBFS, dynamic thresholding with 6 dB margin, and calibrated confidence metric. Zero allocations on update path.
- Step 20 completed: Preamble detection, fine symbol synchronization, and symbol clock tracking implemented (`SyncState`, `SyncListener`, `TimingTracker`, `SyncDetector`). 4-phase scanning at quarter-symbol hops, single-tone rejection, power-of-2 ring buffer with chunked ingestion preventing overflow, sub-hop fine timing refinement via parabolic interpolation, and expected-energy sync word correlation. Verified 100% lock rate across 200 random start offsets (timing error <= quarter symbol), 0 false locks across 3,000 pure noise hops and steady single tones, speaker wake-up clipping survival (5 cut off symbols), and timing tracker lock maintenance across +/- 100 ppm clock drift. Zero allocations on audio push hot paths.
- Step 21 completed: Demodulator and receive state machine implemented (`ReceiverPipeline`, `ReceiveEvent`, `PayloadDecoder`). Streaming 960-sample chunks into 1M-sample circular ring buffer, continuous sample-time state machine, mode block 2-repeat energy summing, symbol confidence calculation for RS erasures, AES-GCM decryption with passphrase, 300 ms inter-message cooldown guard, and back-to-back transmission delivery with zero sample latency.
- Step 25 completed: Spectrum analyzer (FFT) for waterfall implemented (`Fft`, `SpectrumAnalyzer`, `WaterfallRow`). In-place Radix-2 Cooley-Tukey FFT with precomputed twiddle factors and bit-reversal tables, Hann window coherent gain scaling ($16/N^2$), ring buffer hop timing (~60 fps), dBFS normalization, max-pooling dynamic bin mapping, tone marker overlays. 9 unit tests passed (>85x real-time speed, zero allocations).
- Step 26 completed: Manifest permissions, microphone permission state modeling, and audio hardware configuration implemented (`AudioConfig`, `MicPermissionState`, `MicPermissionRequestedProvider`, `PermissionRepository`). Declared `RECORD_AUDIO`, `VIBRATE`, and microphone required feature. Merged manifests verified clean of `INTERNET`. Exhaustive 8-case truth table verified for permission state resolution.
- Step 27 completed: AudioTrack transmitter implemented (`PcmSink`, `AudioTrackSink`, `OutputRouteInfo`, `AcousticTransmitter`, `DefaultAcousticTransmitter`). Low-latency streaming AudioTrack, real-time thread priority, zero allocations per audio chunk, throttled progress updates, 10 ms click-free cosine fade-out on cancellation, clean resource release.
- Step 31 completed: Encrypted Room vault storage implemented (`VaultEntity`, `VaultDao`, `VaultDatabase`, `AndroidKeystoreCipher`, `FakeKeystoreCipher`, `VaultRepository`). AES-256-GCM hardware keystore encryption, sensitive payload masking in `toString()`, graceful degradation on unreadable rows, starred item preservation pruning. 7 unit tests passed.
- Step 32 completed: Settings store implemented (`Settings`, `SettingsRepository`, `DataStoreSettingsRepository`, `FakeSettingsRepository`, `DataStoreMicPermissionRequestedProvider`). Preferences DataStore with safe enum fallback decoding, strict parameter clamping, dynamic color and theme controller binding, Dev-free user model. 8 unit tests passed.
