# Step 15 Report: Secure Mode Crypto

**Date:** 2026-10-23
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 15 delivers authenticated encryption for acoustic transmissions in `:modem` (`app.shoutsend.modem.crypto`) conforming to Rules 21, 25 (strict privacy and key zeroing), 27, and 28. No third-party crypto libraries were added; implementation relies exclusively on standard JDK `javax.crypto` primitives.

Key highlights:
- **KDF & Cryptographic Primitives:** Key derivation uses `PBKDF2WithHmacSHA256` with 60,000 iterations and a 4-byte random salt to generate a 256-bit AES key. Encryption utilizes `AES/GCM/NoPadding` with a 12-byte random nonce and a 12-byte (96-bit) authentication tag.
- **Associated Authenticated Data (AAD):** Protocol Version 1 byte and `MessageType` ID byte are bound into the GCM authentication tag, preventing cross-type replay and tampering attacks.
- **Wire Layout & Overhead:** Exact on-air overhead of 28 bytes (`salt [4B] + nonce [12B] + ciphertext + tag [12B]`). Compaction (Step 14) executes strictly BEFORE encryption.
- **Passphrase Quality Evaluation:** Pure evaluation function rating passphrases (WEAK, OK, STRONG) with clear security warnings regarding offline dictionary attacks on recorded acoustic signals.
- **Zero-Wipe Guarantee:** All intermediate byte arrays and key buffers are zero-wiped (`.fill(0)`) in `finally` blocks immediately upon cipher execution. Passphrases are never stored or logged.

---

## 2. Experimental Measurements & Performance

### 2.1 KDF Execution Benchmark
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.crypto.SecureBoxTest.kdfBenchmarkTiming"`
```text
### PBKDF2 (60,000 iterations) Single-Operation Duration: 61.30 ms (Host Build Machine)
```
*Note: Real mobile device execution time is an OWNER RUN during Phase F hardening.*

### 2.2 Plain Mode vs. Secure Mode Transmission Impact (12-char Wi-Fi Password)
Measured using canonical `AirTime.estimate` (DTFSK Fast at 20ms and MFSK16 Normal at 30ms, 2 repeat passes):
```text
### Secure Mode vs Plain Mode Comparison (12-char Wi-Fi Password):
| Mode        | Payload Bytes | On-Air Frames | AirTime (DTFSK Fast) | AirTime (MFSK16 Normal) |
| :---        | :---          | :---          | :---                 | :---                    |
| Plain       |  23 bytes      |  1 frames     |  4.20s                |  8.28s                  |
| Secure (Enc)|  51 bytes      |  2 frames     |  7.56s                | 15.72s                  |
```
*Analysis: The 28-byte cryptographic overhead pushes the 23-byte payload across the 42-byte frame boundary to 51 bytes, requiring an additional physical frame and adding 3.36s (DTFSK Fast) or 7.44s (MFSK16 Normal) of audio duration.*

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Test Suite Execution (8 Tests Passing)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.crypto.SecureBoxTest"`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test

BUILD SUCCESSFUL in 16s
```
Tests passing:
- `roundTripAllPayloadTypes`: Proved decryption identity across Text, URL, Wi-Fi, and Contact payloads.
- `wrongPassphraseFailsAuthentication`: Incorrect passphrases cleanly rejected via `WrongPassphraseOrTampered`.
- `singleFlippedBitAnywhereFailsAuthentication`: Proved 1-bit tampering at every offset fails GCM tag authentication.
- `differentMessagesGetDistinctNoncesAndSalts`: Verified nonces and salts are unique per transmission.
- `associatedDataBindsMessageType`: Verified cross-type ciphertext manipulation fails authentication.
- `passphraseStrengthEvaluation`: Verified length (<8 chars) and variety scoring.
- `kdfBenchmarkTiming`: Verified PBKDF2 speed (61.30 ms).
- `fullEndToEndIntegrationWithSteps12To14`: Full pipeline (`Payload` -> `compact` -> `seal` -> `fragment` -> `encode` -> `decode` -> `assemble` -> `open` -> `expand` -> `Payload`) verified 100%.

### 3.2 Standard Verification (SV) Block
Command: `./gradlew check assembleDevDebug assembleUserDebug`
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:assembleUserDebug
> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :app:verifyChirpRules
> Task :app:assembleDevDebug

BUILD SUCCESSFUL in 38s
151 actionable tasks: 32 executed, 119 up-to-date
```
