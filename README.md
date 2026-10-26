# ShoutSend

**100% Native Offline Acoustic Data Transfer for Android**

ShoutSend transmits text, URLs, Wi-Fi credentials, and contacts between smartphones exclusively using sound waves.
Zero Wi-Fi. Zero Bluetooth. Zero cellular. Zero cloud servers. Zero pairing.

---

## Key Highlights

- **Pure Soundwave Physical Layer**: Transfers data over audible sound (`Audible Retro`, 1.5 kHz – 3.9 kHz) or near-ultrasound (`Ultrasonic Stealth`, 17.5 kHz – 19.9 kHz).
- **Hardened Error Correction**: Forward error correction via systematic Reed-Solomon $GF(2^8)$ with errors-and-erasures decoding.
- **Real-Time Spectrogram**: Rolling 256-bin acoustic waterfall display with zero heap allocations on the audio render thread.
- **Hardware-Isolated Privacy**:
  - Exactly **two** runtime permissions: `RECORD_AUDIO` and `VIBRATE`.
  - Zero `INTERNET` permission — permanently forbidden and verified in merged manifests.
- **Authenticated Secure Mode**: Military-grade AES-256-GCM encryption with PBKDF2WithHmacSHA256 (60,000 rounds) key derivation and in-place memory sanitization.
- **Encrypted Transmission Vault**: Local SQLite database encrypted at rest using Android Keystore hardware-backed keys.
- **Ultra-Compact Shipping Binary**: Optimized release APK is just **4.0 MB** with R8 optimization and resource shrinking.

---

## Architectural Overview

ShoutSend is structured cleanly into two decoupled Gradle modules:

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

## Physical Layer Protocol (Protocol v1)

| Parameter | Audible Retro | Ultrasonic Stealth |
| :--- | :--- | :--- |
| **Carrier Frequency Band** | 1,500 Hz – 3,900 Hz | 17,500 Hz – 19,900 Hz |
| **Modulation Schemes** | MFSK16 (4 bits/sym) / DTFSK (7 bits/sym) | MFSK16 (4 bits/sym) |
| **Tone Spacing** | 150 Hz / 160 Hz | 150 Hz |
| **Preamble Sequence** | Alternating S0/S1 tones + 8-symbol Barker-like sync | Alternating S0/S1 tones + sync |
| **Error Correction** | Reed-Solomon RS(60, 48) [Normal] / RS(60, 40) [Robust] | RS(60, 48) / RS(60, 40) |
| **Payload Capacity** | 1 to 320 bytes per message | 1 to 320 bytes per message |
| **Air-time Latency** | 2.5s – 8.0s depending on payload and speed | 3.5s – 9.0s |

---

## Build Variants

ShoutSend supports two distinct product flavors along the `audience` dimension:

| Variant | Application ID | Purpose | Characteristics |
| :--- | :--- | :--- | :--- |
| **`dev`** | `app.shoutsend.dev` | Developer & Diagnostics | Includes runtime token editor, DSP Lab (16-bar Goertzel power meters, tone injector, noise simulator), and field telemetry logger. |
| **`user`** | `app.shoutsend` | Production Shipping | 100% clean of all developer panels and debug markers. R8-optimized release artifact (4.0 MB). |

---

## Verification & Build Commands

### Standard Verification Suite
Run full compilation, unit test suites, style guards, and purity verification:
```bash
./gradlew check
```

### Build Release Artifacts
```bash
# Build optimized User release APK
./gradlew assembleUserRelease

# Build Developer debug APK
./gradlew assembleDevDebug
```
Output APKs:
- `app/build/outputs/apk/user/release/app-user-release.apk` (4.0 MB)
- `app/build/outputs/apk/dev/debug/app-dev-debug.apk`

### Run Architectural Guards
```bash
# StyleGuard: Verifies zero hardcoded hex, dp, or sp literals outside tokens
./gradlew checkNoHardcodedStyle

# User APK Purity: Verifies 0 dev markers in DEX
./gradlew verifyUserApkClean

# Permissions Guard: Confirms strictly RECORD_AUDIO and VIBRATE (No INTERNET)
./gradlew verifyPermissions

# Modem Purity: Confirms pure Kotlin JVM with 0 Android imports
./gradlew :modem:checkModemPurity
```

---

## Security & Privacy Statement

1. **Air-Gapped by Design**: ShoutSend contains zero networking libraries, analytics, or tracking SDKs. It cannot connect to the internet even if a network is present.
2. **Confidentiality in the Air**: Encrypted payloads are sealed using AES-256-GCM. Passphrases are never broadcast, never persisted in plain text, and memory buffers are zeroed out immediately after synthesis.
3. **Hardware Storage Encryption**: History records stored in the Transmission Vault are encrypted using SQLCipher / Room backed by a hardware master key residing in AndroidKeyStore.

---

## License

Copyright © 2026 ShoutSend Authors. Proprietary & Confidential.
