# ShoutSend

> Send Wi-Fi passwords, web links, and contacts through the air using pure sound — zero internet, zero Bluetooth, zero pairing.

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/aotlover9-base-eth/ShoutSend/releases/tag/v1.0.0)
[![Platform](https://img.shields.io/badge/platform-Android%2012%2B-green.svg)](https://developer.android.com)
[![Permissions](https://img.shields.io/badge/permissions-Audio%20%2B%20Vibrate%20only-brightgreen.svg)](#privacy-in-one-minute)
[![Internet Permission](https://img.shields.io/badge/INTERNET%20permission-NONE%20(100%25%20offline)-red.svg)](#privacy-in-one-minute)

---

## Table of Contents

- [Half A: The Friendly Tour](#half-a-the-friendly-tour)
  - [What is this?](#what-is-this)
  - [Why would I want this?](#why-would-i-want-this)
  - [How it works in 20 seconds](#how-it-works-in-20-seconds)
  - [Cool things inside](#cool-things-inside)
  - [Honest speed and range](#honest-speed-and-range)
  - [Privacy in one minute](#privacy-in-one-minute)
  - [Good to know and limits](#good-to-know-and-limits)
  - [Get it](#get-it)
  - [Project Status](#project-status)
- [Half B: Under the Hood](#half-b-under-the-hood)
  - [System Architecture](#system-architecture)
  - [Physical Layer Protocol (Protocol v1)](#physical-layer-protocol-protocol-v1)
  - [Authenticated Secure Mode](#authenticated-secure-mode)
  - [Architectural Guards and Verification](#architectural-guards-and-verification)
  - [Build from Source](#build-from-source)
  - [Documentation Index](#documentation-index)
  - [Contributing and Rules Summary](#contributing-and-rules-summary)
  - [License](#license)

---

# Half A: The Friendly Tour

*Screenshots coming soon*

### What is this?

ShoutSend is an Android app that turns text, links, Wi-Fi credentials, and contacts into audible sound or near-ultrasound waves. Your phone plays a musical chirp, and any other phone with ShoutSend open hears it and displays the information right away. No Wi-Fi network, Bluetooth pairing, cloud servers, or cables required.

---

### Why would I want this?

- **Share Wi-Fi with dinner guests**: Don't read a 24-character random password aloud. Tap "Send Wi-Fi" and let your phone chirp it directly into their phone in seconds.
- **Off-grid link sharing**: You're hiking in the mountains or underground in the subway with zero cellular reception. You can still beam an offline guide URL or saved note across.
- **Instant contact swap**: Trade phone numbers and email addresses without typing them into someone's phone or fumbling with camera QR codes that won't focus in low light.
- **Air-gapped security zones**: Share notes between phones in rooms where Wi-Fi, Bluetooth, and cellular radios are forbidden or jam-packed with interference.
- **It just works**: No "Searching for nearby Bluetooth devices...", no PIN confirmations, no pairing codes, and no account logins.

---

### How it works in 20 seconds

Think of it like a walkie-talkie mixed with an old-school dial-up modem, but built for modern smartphones:

1. **You type**: Pick Text, URL, Wi-Fi, or Contact.
2. **You press Transmit**: Your speaker chirps a short sequence of musical tones.
3. **Friend's phone hears it**: ShoutSend captures the audio through the microphone, fixes any background room noise, and displays an instant action card (like "Join Wi-Fi" or "Add Contact").

```
  [ Phone A ]                                      [ Phone B ]
  ┌─────────┐                                      ┌─────────┐
  │ Type or │                                      │ Listens │
  │ Pick    │ ───►  ♫ ♪ ♫ Soundwaves ♫ ♪ ♫  ───►   │ & Shows │
  │ Payload │      (Through the air)               │ Card    │
  └─────────┘                                      └─────────┘
```

---

### Cool things inside

- **Only 2 Permissions**: Needs only `RECORD_AUDIO` (to hear the chirp) and `VIBRATE` (gentle buzz when a message arrives).
- **No Internet Permission At All**: The `android.permission.INTERNET` flag does not even exist in the app's code. It cannot phone home even if you wanted it to.
- **Secure Mode**: Protecting a sensitive password? Seal it with a passphrase using AES-256-GCM encryption. Even someone recording the audio cannot decipher it without the password.
- **Live Waterfall Spectrogram**: Watch the sound waves arrive in real time with a retro cyber-radio waterfall visualization.
- **Encrypted Transmission Vault**: Your transmission history is kept safe on your device using hardware-backed Android Keystore encryption.
- **Tiny 4 MB Download**: Clean native Kotlin code with zero bloatware.

---

### Honest speed and range

> **Honesty Notice:** These numbers are measured from deterministic channel simulations (documented in [`docs/DSP_BENCHMARK.md`](docs/DSP_BENCHMARK.md)). Real-world performance on physical phones depends on your speaker quality, microphone sensitivity, and room background noise.

- **Fast Mode (20 ms tones)**: Delivers approximately **9.0 to 12.5 bytes/second** in simulation. Sends a typical 44-byte link or short text in **~3.5 seconds**. Best when phones are sitting close together (< 0.5 m) on a desk.
- **Normal Mode (30 ms tones, default)**: Delivers **~2.8 bytes/second** with dual repeats for room echo resilience. Sends small payloads (up to 42 bytes) in **~8.5 seconds**, and 44-byte payloads in **~15.9 seconds**. Reaches 100% success across living rooms and quiet rooms up to 2 meters in simulation.
- **Robust Mode (60 ms tones)**: Delivers **~1.4 bytes/second** with heavy error correction. Takes **~30.8 seconds** for 44 bytes, but cuts through loud cafeteria noise and clinking cutlery.
- **Ultrasound (Stealth Mode)**: Uses high frequencies (17.5 kHz – 19.9 kHz) that most humans cannot hear. Works great on close desks, but drops off faster over distance and requires phones that can record above 18 kHz.

---

### Privacy in one minute

- **No accounts**: No sign-ups, email addresses, phone numbers, or analytics.
- **No trackers**: Zero third-party SDKs, ads, or telemetry.
- **What you send stays local**: Data travels through physical sound waves in your room and stops at your walls.
- **Acoustic Reality Check**: Sound waves are broadcast aloud. Anyone sitting near you with an audio recorder could record the chirp. If you are transmitting sensitive credentials or passwords, always enable **Secure Mode** with a passphrase!

---

### Good to know and limits

- **Requires working speakers and mics**: Both phones need a working speaker and microphone.
- **Payload size budget**: Designed for concise payloads — text messages, URLs, Wi-Fi credentials, and contact cards up to **320 bytes**. It is not intended for sending photos, audio files, or videos.
- **Loud noise can slow it down**: In a busy restaurant or loud concert, switch to **Robust Mode** so the error correction can rebuild damaged tones.
- **Hardware variation**: Some budget phones have built-in microphone noise filters that aggressively muffle high-pitch ultrasound tones (> 18 kHz). If Stealth mode struggles on a phone, use the in-app **Calibration tool** or stick to Audible Retro mode.

---

### Get it

ShoutSend is completely independent and not hosted on the Google Play Store.

1. Go to the [Releases page](https://github.com/aotlover9-base-eth/ShoutSend/releases).
2. Download `ShoutSend-v1.0.0.apk`.
3. Open the downloaded APK on your Android phone and tap **Install** (if prompted, enable "Allow installation from this source").

---

### Project Status

- **Version:** `v1.0.0`
- **Roadmap:** All 59 development and hardening steps completed.
- **Test Suite:** 100% automated tests passing across pure JVM DSP modules, Android ViewModels, Room databases, and R8 shrinking.
- **Field Testing:** Comprehensive automated and simulated testing is verified. Physical over-the-air validation across diverse physical phone pairs in varied real-world rooms is active (see [`docs/FIELD_TEST_PLAN.md`](docs/FIELD_TEST_PLAN.md)). Early feedback is warmly welcome!

---
---

# Half B: Under the Hood

This section contains technical documentation for developers, security auditors, and radio hobbyists.

```
┌────────────────────────────────────────────────────────┐
│                        :app                            │
│  - Jetpack Compose UI (Material 3 Cyber Radio tokens)  │
│  - MVVM Architecture (StateFlow & Coroutines)          │
│  - AudioTrack Transmitter & AudioRecord Receiver        │
│  - Room Database with Android Keystore Encryption       │
│  - Product Flavors: 'dev' (DSP Lab) and 'user' (clean) │
└──────────────────────────┬─────────────────────────────┘
                           │ depends on (JVM only)
┌──────────────────────────▼─────────────────────────────┐
│                       :modem                           │
│  - 100% Pure Kotlin JVM (Zero Android dependencies)    │
│  - MFSK16 & DTFSK Tone Synthesis (Continuous Phase)    │
│  - Coherent Hann-Windowed Goertzel Resonator Bank      │
│  - Dynamic Asymmetric Noise Floor Tracking             │
│  - Reed-Solomon GF(256) Errors-and-Erasures Decoder    │
│  - Packet Fragmentation, Reassembly, & CRC-16          │
└────────────────────────────────────────────────────────┘
```

---

### System Architecture

ShoutSend is split into two cleanly separated Gradle modules:

1. **`:modem` (Pure Kotlin JVM)**:
   - Does not import any `android.*` packages (enforced by `:modem:checkModemPurity` task).
   - Zero heap allocations on DSP hot paths (no objects created per sample or per symbol).
   - Houses mathematical models for Reed-Solomon $GF(2^8)$, Goertzel filters, tone synthesis, whitening, and CRC-16.
2. **`:app` (Android Layer & UI)**:
   - Built with Jetpack Compose and Material You principles.
   - Strictly tokenized: zero hardcoded hex colors, corner radii, or dp/sp dimensions outside `DefaultTokens.kt`.
   - Real-time AudioTrack playback and AudioRecord capture running on prioritized background threads.
   - Two flavors: `user` (minimal, production-stripped) and `dev` (includes DSP Lab, synthetic tone injector, and field telemetry logger).

---

### Physical Layer Protocol (Protocol v1)

| Parameter | Audible Retro Band | Ultrasonic Stealth Band |
| :--- | :--- | :--- |
| **Frequency Range** | 1,500 Hz – 3,900 Hz | 17,500 Hz – 19,900 Hz |
| **Modulation Schemes** | MFSK16 (1 tone = 4 bits) / DTFSK (2 tones = 7 bits) | MFSK16 (1 tone = 4 bits) |
| **Tone Spacing ($\Delta f$)** | 150 Hz (MFSK16) / 160 Hz (DTFSK) | 150 Hz |
| **Symbol Durations** | Fast (20 ms), Normal (30 ms), Robust (60 ms) | Fast (20 ms), Normal (30 ms), Robust (60 ms) |
| **Windowing** | Continuous-phase raised cosine ramp (3 ms – 6 ms) | Continuous-phase raised cosine ramp (3 ms – 6 ms) |
| **Preamble & Sync** | Alternating S0/S1 tones + 8-symbol sync word | Alternating S0/S1 tones + 8-symbol sync word |
| **Forward Error Correction** | Systematic Reed-Solomon $GF(2^8)$: RS(60,48) / RS(60,40) | Systematic Reed-Solomon $GF(2^8)$: RS(60,48) / RS(60,40) |
| **Frame Integrity** | CRC-16-CCITT ($x^{16} + x^{12} + x^5 + 1$, seed `0xFFFF`) | CRC-16-CCITT ($x^{16} + x^{12} + x^5 + 1$, seed `0xFFFF`) |
| **Scrambling / Whitening** | PN9 LFSR ($x^9 + x^5 + 1$, seed `0xA5`) | PN9 LFSR ($x^9 + x^5 + 1$, seed `0xA5`) |

---

### Authenticated Secure Mode

When transmitting secrets (passwords, private notes), ShoutSend seals payloads using authenticated encryption:

- **Key Derivation**: PBKDF2WithHmacSHA256 with 60,000 iterations and a cryptographically secure 16-byte random salt per message.
- **Cipher**: AES-256-GCM with a 12-byte random initialization vector (IV) and 16-byte authentication tag (28 bytes wire overhead).
- **Associated Data (AAD)**: Message type and length are bound into the GCM AAD, preventing ciphertext reuse or header spoofing.
- **Memory Zeroing**: Passphrase character arrays and derived key bytes are wiped in memory immediately after encryption/decryption (`java.util.Arrays.fill(bytes, 0.toByte())`).

---

### Architectural Guards and Verification

The build system includes custom automated Gradle verification guards in `buildSrc`:

- **`checkNoHardcodedStyle`**: Scans all Compose UI and test source files to guarantee no raw hex colors, `.dp`, or `.sp` literals exist outside token files.
- **`verifyUserApkClean`**: Inspects DEX classes of `app-user-debug.apk` and `app-user-release.apk` to ensure zero developer tools (`DspLab`, `DeveloperPanel`, `FieldTestLogger`) leak into shipping builds.
- **`verifyPermissions`**: Checks merged Android manifests for both flavors to ensure only `RECORD_AUDIO` and `VIBRATE` are present and that `INTERNET` is permanently blocked.
- **`checkModemPurity`**: Verifies that `:modem` contains zero imports of Android SDK classes.

---

### Build from Source

#### Prerequisites
- JDK 17 (OpenJDK or Oracle)
- Android SDK with Platform 35 and Build Tools 35.0.0

#### Commands

```bash
# Clone the repository
git clone https://github.com/aotlover9-base-eth/ShoutSend.git
cd ShoutSend

# Run full test suite and architectural guards
./gradlew check

# Build Developer and User debug APKs
./gradlew assembleBothDebug

# Build optimized User release APK (R8-minified)
./gradlew assembleUserRelease

# Run lint analysis
./gradlew lintDevDebug lintUserDebug
```

---

### Documentation Index

All architectural decisions, benchmarks, reports, and plans are maintained under [`docs/`](docs/):

- [`docs/RULES.md`](docs/RULES.md): The 28 core architectural rules governing the project.
- [`docs/PROTOCOL.md`](docs/PROTOCOL.md): Complete acoustic physical layer specification (Protocol v1).
- [`docs/DECISIONS.md`](docs/DECISIONS.md): Dated architectural decision records (DEC-001 through DEC-068).
- [`docs/PROJECT_MEMORY.md`](docs/PROJECT_MEMORY.md): Module hierarchy, component map, and historical progress.
- [`docs/DSP_BENCHMARK.md`](docs/DSP_BENCHMARK.md): 42-cell acoustic simulation benchmark results and timings.
- [`docs/FIELD_TEST_PLAN.md`](docs/FIELD_TEST_PLAN.md): Physical device test methodology and owner procedures.
- [`docs/ROADMAP.md`](docs/ROADMAP.md): Complete roadmap matrix (Steps 0 through 59).
- [`docs/RUN_LOG.md`](docs/RUN_LOG.md): Timestamped log of build steps and verification outputs.
- [`docs/RUN_SUMMARY.md`](docs/RUN_SUMMARY.md): Executive run summary across all 61 roadmap steps, test counts, guards, and risks.
- [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md): Tokens, color palettes, and typography specifications.
- [`docs/README_AUDIO_SAMPLES.md`](docs/README_AUDIO_SAMPLES.md): Synthetic reference WAV files.
- [`docs/reports/`](docs/reports/): Detailed engineering reports corresponding to each roadmap step (`step-01-report.md` to `step-59-report.md`, plus `release-v1.0.0-report.md`).

---

### Contributing and Rules Summary

ShoutSend follows strict development invariants:
1. Pure Kotlin JVM for modem DSP; no allocations on hot paths.
2. Dynamic token system; zero hardcoded UI dimensions.
3. Strict offline permissions: never add `android.permission.INTERNET`.
4. Privacy first: never log message payloads or passwords.

---

### License

License: not decided yet.
