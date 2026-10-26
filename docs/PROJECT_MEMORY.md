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
- Step 22 completed: Channel simulator and loopback harness implemented (`AcousticChannelSimulator`, `ChannelScenario`, `EchoRoom`, `FrequencyResponseModel`, `LoopbackHarness`, `BatchRunner`). Parameterized physical impairment pipeline (multipath echo, AWGN, ambient hum/pink noise, biquad transducer filtering, clock drift, dropouts), reproducible seeded runs, and Gradle `:modem:modemBenchmark` task generating `docs/DSP_BENCHMARK.md`. All 7 unit tests passed.
- Step 25 completed: Spectrum analyzer (FFT) for waterfall implemented (`Fft`, `SpectrumAnalyzer`, `WaterfallRow`). In-place Radix-2 Cooley-Tukey FFT with precomputed twiddle factors and bit-reversal tables, Hann window coherent gain scaling ($16/N^2$), ring buffer hop timing (~60 fps), dBFS normalization, max-pooling dynamic bin mapping, tone marker overlays. 9 unit tests passed (>85x real-time speed, zero allocations).
- Step 26 completed: Manifest permissions, microphone permission state modeling, and audio hardware configuration implemented (`AudioConfig`, `MicPermissionState`, `MicPermissionRequestedProvider`, `PermissionRepository`). Declared `RECORD_AUDIO`, `VIBRATE`, and microphone required feature. Merged manifests verified clean of `INTERNET`. Exhaustive 8-case truth table verified for permission state resolution.
- Step 27 completed: AudioTrack transmitter implemented (`PcmSink`, `AudioTrackSink`, `OutputRouteInfo`, `AcousticTransmitter`, `DefaultAcousticTransmitter`). Low-latency streaming AudioTrack, real-time thread priority, zero allocations per audio chunk, throttled progress updates, 10 ms click-free cosine fade-out on cancellation, clean resource release.
- Step 28 completed: AudioRecord receiver implemented (`PcmSource`, `AudioRecordSource`, `SpscRingBuffer`, `ReceiverDiagnostics`, `AcousticReceiver`, `DefaultAcousticReceiver`). Unprocessed -> Voice Recognition -> Mic priority selection, AEC/NS/AGC disabling, lock-free 262k SPSC ring buffer, 2s digital silence detection (`MicrophoneSilent`), 500ms clipping detection (`InputTooLoud`), 60 fps waterfall spectrum channel, idempotent start/stop. 7 unit tests passed.
- Step 29 completed: Capability probe and calibration engine implemented (`AudioStreamSource`, `TestSequence`, `CalibrationAnalyzer`, `CalibrationThresholds`, `BandReport`, `CalibrationRecommender`, `LinkTest`, `CapabilityProbe`, `CalibrationRunner`, `CalibrationResult`). Pure Kotlin calibration analysis in `:modem` measuring ambient noise, marker detection, and 18 tones per band; hardware capability probe; loopback calibration runner with explicit half-duplex exception and hardware warning checks; link quality scorer. 12 unit tests passed.
- Step 31 completed: Encrypted Room vault storage implemented (`VaultEntity`, `VaultDao`, `VaultDatabase`, `AndroidKeystoreCipher`, `FakeKeystoreCipher`, `VaultRepository`). AES-256-GCM hardware keystore encryption, sensitive payload masking in `toString()`, graceful degradation on unreadable rows, starred item preservation pruning. 7 unit tests passed.
- Step 32 completed: Settings store implemented (`Settings`, `SettingsRepository`, `DataStoreSettingsRepository`, `FakeSettingsRepository`, `DataStoreMicPermissionRequestedProvider`). Preferences DataStore with safe enum fallback decoding, strict parameter clamping, dynamic color and theme controller binding, Dev-free user model. 8 unit tests passed.
- Step 30 completed: Send and receive sessions, audio focus, and route monitor implemented (`TransmitState`, `TransmitSession`, `DefaultTransmitSession`, `ReceiveState`, `ReceiveSession`, `DefaultReceiveSession`, `AudioCoordinator`, `DefaultAudioCoordinator`, `AudioFocusController`, `DefaultAudioFocusController`, `AudioRouteMonitor`, `DefaultAudioRouteMonitor`, `SessionLifecycleManager`). Half-duplex mutual exclusion enforced, audio focus handled with transient pause and focus-loss cancellation, route warning classifier (`SPEAKER_OK`, `HEADSET_WARNING`, `BLUETOOTH_WARNING`, `USB_WARNING`), background 5s grace period stop, end-to-end loopback with Step 22 channel simulator verified. 8 unit tests passed.

### Session State Machine Diagrams

#### Transmit Session State Flow
```text
  [Idle]
    │
    ▼ send(payload, profile, passphrase)
[Preparing] ─── (validation / size / coordinator / focus error) ───► [Failed(reason)]
    │
    ▼ lock acquired & focus granted
[Transmitting(progress, elapsed, remaining, frame, totalFrames)]
    │
    ├─── focus loss / playback error ───► [Failed(reason)]
    ├─── cancel() by user ──────────────► [Cancelled]
    ▼
[Completed(durationMs)]
```

#### Receive Session State Flow
```text
  [Idle]
    │
    ▼ start(band, passphraseProvider)
[Starting] ─── (coordinator busy / mic init error) ───► [Failed(reason)]
    │
    ▼ receiver started
[Listening(ambientDbfs)] ◄──── (post-message Cooldown if no message)
    │
    ├─── 2s digital zeros ───► [Failed("MicrophoneSilent")]
    ▼ Preamble detected
[PreambleDetected]
    │
    ▼ sync word verified
[Locked(snrDb)]
    │
    ▼ data frames ingested
[Receiving(received, total, snrDb)] ─── (corrupted / lost) ───► [Failed(reason)]
    │
    ▼ message reassembled
    ├─ Plaintext / auto-decrypted ────► [Success(payload, metadata)]
    ├─ Encrypted & no passphrase ────► [NeedsPassphrase(message)]
    │                                       │
    │                                       ▼ submitPassphrase()
    │                                  [Success] / [Failed("WrongPassphrase")]
    └─ Corrupted body ───────────────► [Failed("PayloadCorrupt")]
```

- Step 23 completed: Benchmark and preset tuning in simulation completed (`BatchRunner`, `docs/DSP_BENCHMARK.md`). 42-cell sweep executed across 7 modem profiles and 6 physical scenarios (Clean, Close Range, Living Room, Quiet Room, Cafe, Weak Phone). Verified 100% success for Retro Normal across all scenarios, confirmed MFSK16 as universal default and DTFSK as optional high-speed mode (10.0 B/s), formalized 95% confidence intervals and UI preset timings. Full `:modem:modemBenchmark` and `:modem:test` passing.
- Step 24 completed: DSP performance and allocation audit completed (`DspPerfRunner`, `:modem:modemPerf`, `DspPerformanceAndAllocationAuditTest`). Eliminated `SyncDetector` per-hop allocation (129 kB / 30s) and progression loops in `ReceiverPipeline`. Proved zero allocations per audio sample ($< 30$ B/s steady-state JVM overhead). Real-time factor measured at RTF = 0.00357 (280x faster than real-time on build machine, well surpassing target 0.02). Goertzel Double vs Float inner loop benchmark confirmed identical speed (Double 261ms vs Float 270ms) retaining 53-bit mantissa accuracy. All earlier regression tests passing.



- Step 33 completed: Modem config provider and dev override seam completed. Implemented `ModemTuning` data class and validation logic in `:modem` with bit-identical SHA-256 transmission waveform generation. Implemented `ModemConfig` and `ModemConfigProvider` in `:app`, with `FixedModemConfigProvider` in `src/user` and `OverridableModemConfigProvider` in `src/dev`. Updated `TransmitSession`, `ReceiveSession`, `CalibrationRunner`, and `DefaultAcousticReceiver` to query active configuration and tuning dynamically at each session start. Verified 100% clean User APK via `verifyUserApkClean` and dev markers. Full test suite passing across dev and user flavors.

- Step 34 completed: ShoutSend components and extra tokens implemented and verified. Expanded design system with 14 specialized color tokens (`signalLow`, `signalMid`, `signalHigh`, `signalPeak`, `noiseFloorLine`, `syncMarker`, `toneGuide`, `glowPrimary`, `glowSecondary`, `success`, `onSuccess`, `warning`, `onWarning`, `scanline`) and `RadioVisualTokens` for physical radio parameters. Implemented `ColormapFactory` with perceptually uniform gamma-corrected photometric interpolation across 4 stops and verified with unit tests. Built reusable cyber radio components (`NeonCard`, `StatusPill`, `SignalMeter`, `LevelMeter`, `ByteMeter`, `ModeSelector`, `SpeedSelector`, `RadioReadout`, `BrandMark`, `ScanlineOverlay`). Added `LocalReduceMotion` reading system animator duration scale. Integrated token roles into `DevPanelViewModel` and created Dark and Light preview galleries in `CyberRadioComponentsPreview.kt`. Passed all architectural guards (`checkNoHardcodedStyle`, `verifyPermissions`, `checkModemPurity`, `verifyUserApkClean`, `verifyChirpRules`) and test suites (`testDevDebugUnitTest`, `testUserDebugUnitTest`).

- Step 35 completed: App shell, adaptive scaffold, and navigation implemented and verified. Implemented type-safe routing using Kotlinx Serialization (`OnboardingRoute`, `SendRoute`, `ReceiveRoute`, `VaultRoute`, `VaultDetailRoute`, `SettingsRoute`, `CalibrationRoute`, `LinkTestRoute`). Built `AppScaffold` with adaptive switching between compact bottom bar (`CdNavigationBar`) and expanded wide-screen navigation rail (`CdNavigationRail`). Added placeholder screens with ViewModels for all destinations consuming design system components (`NeonCard`, `StatusPill`, `SignalMeter`, `LevelMeter`, `ModeSelector`, `SpeedSelector`). Added motion-tokenized transitions (vertical fade-through for tabs, horizontal slide for sub-screens). Added back confirmation dialog (`CdAlertDialog`) during active acoustic sessions. Persisted `lastTab` in DataStore settings with cold launch restoration. Passed all architectural checks (`checkNoHardcodedStyle`, `verifyPermissions`, `checkModemPurity`, `verifyUserApkClean`, `verifyChirpRules`) and test suites (`testDevDebugUnitTest`, `testUserDebugUnitTest`).

- Step 36 completed: Onboarding and permission flow implemented and verified. Created 4-page horizontal pager with morphing indicator and Canvas illustrations (`TalkInSoundIllustration`, `HowItWorksIllustration`, `GoodToKnowIllustration`, `AllowMicrophoneIllustration`). Built reusable `MicPermissionCard` component handling `CanAsk`, `ShouldExplain`, and `PermanentlyDenied` states with app settings deep-linking. Wired permission card into `ReceiveScreen` and `CalibrationScreen`. Externalized all user strings into `strings.xml`. Wired `LifecycleEventEffect(ON_RESUME)` for automatic permission state refresh. Implemented `OnboardingViewModel` with unit test suite covering page navigation, skip, finish, and permission outcomes. Passed all architectural checks (`checkNoHardcodedStyle`, `verifyPermissions`, `checkModemPurity`, `verifyUserApkClean`, `verifyChirpRules`) and test suites (`testDevDebugUnitTest`, `testUserDebugUnitTest`).

- Step 37 completed: Send screen, part 1 (compose, text and URL) implemented and verified. Implemented `SendViewModel` with unidirectional data flow and immutable `SendUiState` holding selected category (`TEXT`, `URL`, `WIFI`, `CONTACT`), text/URL inputs, live `PayloadCompactor` on-air byte calculation, `AirTime.estimate` physical duration, and transmission state. Enforced debounced live updates (~150 ms) during typing with immediate response in text fields. Added `ByteMeter` budget visualization out of 320 bytes with warning and error color transitions. Implemented settings strip with `ModeSelector` and `SpeedSelector`, near-ultrasound Stealth mode disclosure dialog, transmission options bottom sheet for repeats (1x, 2x, 3x) and volume tips, and greyed-out Secure Mode switch. Implemented `ACTION_SEND` (`text/plain`) intent filter in `AndroidManifest.xml` and pure parsing engine `SharedTextParser` cleanly categorizing text vs URL and truncating inputs exceeding 280 UTF-8 bytes along code point boundaries. Created `ClipboardSanitizer` stripping ASCII control characters. Implemented `TransmitScreen` monitoring real-time `TransmitState` with `BackHandler` cancellation intercept. Created `SendScreenPreview` in `src/dev` and comprehensive unit test suite in `SendViewModelTest`. Passed all architectural checks (`checkNoHardcodedStyle`, `verifyPermissions`, `checkModemPurity`, `verifyUserApkClean`) and test suites (`testDevDebugUnitTest`, `testUserDebugUnitTest`).

- Step 38 completed: Send screen, part 2 (Wi-Fi and contact forms) implemented and verified. Implemented `WifiComposer` with SSID (1..32 UTF-8 bytes), security dropdown/chips (WPA2, WPA3, OPEN), password field with show/hide toggle and clipboard paste button, hidden network switch, and prominent acoustic exposure privacy cue card ("Anyone near you with a recorder could capture this password."). Password field disables autofill/suggestions and is marked sensitive for accessibility. Implemented `ContactComposer` using `ActivityResultContracts.PickContact` with zero-permission Data directory querying (`ContactPickerReader`) and manual entry mode. Includes a "This is exactly what will be sent" review card with individual checkboxes per phone number and email to allow selective transmission. Wrote `CdCheckbox` component wrapper. Implemented sensitive memory sanitization (`clearSensitiveData()`) invoked upon leaving the screen (`DisposableEffect`) and immediately upon dispatching transmission. Created dark previews in `SendScreenPreview.kt` and expanded unit test suite in `SendViewModelTest.kt`. All automated checks passing (`:modem:test`, `checkNoHardcodedStyle`, `testDevDebugUnitTest`, `testUserDebugUnitTest`, `verifyPermissions`, `verifyUserApkClean`, `assembleDevDebug`, `assembleUserDebug`, `lintDevDebug`, `lintUserDebug`).

- Step 39 completed: Transmit screen with soundwave rings implemented and verified. Built pure JVM `TransmissionEnvelope` in `:modem` calculating RMS and dominant tone indexes per 20ms window directly from `TransmissionPlan` symbol schedules. Built `RingsCanvas` in `:app` driving concentric ripple waves on a frame clock via `withFrameNanos` using a preallocated 12-ring pool with zero heap allocations per draw frame (Rule 22). Mapped ripple colors across theme band gradient (primary to secondary to tertiary to signalPeak/syncMarker). Implemented `TransmitViewModel` and full `TransmitScreen` with circular progress arc, large `readoutLarge` countdown timer, frame and repeat status counters, mode status pill, single-tap stop button, system back press confirmation dialog, and window keep-screen-on management. Integrated hardware media volume monitoring (< 70%) with a "Turn up" button invoking native system volume panel (`AudioManager.adjustStreamVolume` with `FLAG_SHOW_UI`), and peripheral route warning via `AudioRouteMonitor`. Added automatic history archiving to `VaultRepository` upon completion when `settings.historyEnabled == true`. Expanded dev previews in `SendScreenPreview.kt` and unit test suites in `TransmissionEnvelopeTest.kt` and `TransmitViewModelTest.kt`. All automated checks passing (`:modem:test`, `checkNoHardcodedStyle`, `testDevDebugUnitTest`, `testUserDebugUnitTest`, `verifyPermissions`, `verifyUserApkClean`, `assembleDevDebug`, `assembleUserDebug`, `lintDevDebug`, `lintUserDebug`).

- Step 40 completed: Receive screen and spectrogram waterfall implemented and verified. Built `WaterfallBuffer` managing a 120-row temporal history across 256 frequency bins with zero heap allocations during streaming ingest. Built `WaterfallCanvas` rendering a rolling 256-bin acoustic spectrogram via Jetpack Compose Canvas and native Bitmap drawing with 0 allocations per frame (Rule 22). Added 16 MFSK16 vertical tone guide lines, carrier band boundaries, glowing channel lock highlight with pulsating alpha, scanline overlay texture, frequency scale footer, and an accessible 16-band energy bar chart fallback under `LocalReduceMotion`. Updated `ReceiveViewModel` with half-duplex receive locking (`AudioCoordinator`), decoupled spectrum streaming, hardware diagnostics, and demodulator event handling across Retro and Stealth bands. Created dev previews in `ReceiveScreenPreview.kt` and 11 unit tests in `ReceiveViewModelTest.kt`. Full verification suite passing (`check`, `assembleDevDebug`, `assembleUserDebug`, `lintDevDebug`, `lintUserDebug`).



