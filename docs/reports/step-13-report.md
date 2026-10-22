# Step 13 Report: Payload Model and Codecs

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 13 delivers the typed application payload layer in `:modem` (`app.shoutsend.modem.payload`) conforming to Rules 21, 25 (privacy and secret masking in `toString`), and 27.

Key highlights:
- **Sealed Payload Hierarchy:** Created `Payload` interface with `Text`, `Url`, `WiFi`, and `Contact`. All types strictly mask secret contents (e.g. Wi-Fi passwords, contact details) in `toString()` and error descriptions.
- **Canonical Codecs:**
  - `Text` and `Url`: Raw UTF-8 bytes with validation.
  - `WiFi`: Standard Wi-Fi QR string format (`WIFI:T:<WPA|nopass>;S:<ssid>;P:<password>;H:<true|false>;;`) with comprehensive escaping of special characters (`\`, `;`, `,`, `:`, `"`).
  - `Contact`: Binary TLV format with tag 0x01 (Name), 0x02 (Phones, up to 2), 0x03 (Email, up to 1).
- **UrlSafety Analysis:** Heuristic security analyzer detecting user@host deception, IP literals (IPv4/IPv6), Punycode/Unicode homoglyphs, non-standard ports, and excessive length (>200 chars).
- **Fuzzing & Robustness:** Evaluated 10,000 randomized byte arrays against all message decoders; 100% completed without throwing unhandled exceptions.

---

## 2. Implemented Components

### 2.1 Package Map (`app.shoutsend.modem.payload`)
- `WifiSecurity.kt`: Supported security levels (`OPEN`, `WPA2`, `WPA3`).
- `PayloadValidation.kt`: `PayloadValidationError` and `PayloadValidationResult` (no payload content in error strings).
- `Payload.kt`: Sealed model representing `Text`, `Url`, `WiFi`, and `Contact`.
- `UrlSafety.kt`: `UrlSafetyReport` and `UrlSafety.analyze(url)`.
- `WifiQrCodec.kt`: Wi-Fi QR string encoder and parser with backslash escaping.
- `ContactTlvCodec.kt`: Binary Tag-Length-Value contact serializer and parser.
- `PayloadCodec.kt`: Unified codec dispatching between `Payload` and `MessageType` wire bytes.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Test Suite Execution (8 Tests Passing)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.payload.PayloadCodecTest"`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test

BUILD SUCCESSFUL in 6s
```
Test results from XML report:
- `unicodeTextRoundTrip`: Tested emoji (🚀), Hindi (नमस्ते भारत!), French, Japanese, Arabic, and mixed text.
- `textLengthBoundaryValidation`: Validated 1-byte, 280-byte (valid), 281-byte (rejected), and empty strings.
- `wifiEscapingAllSpecialCharacters`: Tested Wi-Fi with all escaped characters (`\`, `;`, `,`, `:`, `"`).
- `wifiSecurityAndLengthValidation`: Tested Open Wi-Fi (no pass), WPA2 (8..63 chars, 64 hex chars), and boundary SSID lengths (1..32 bytes).
- `malformedWifiStringsRejectedCleanly`: Validated clean rejection of truncated or invalid Wi-Fi strings.
- `contactTlvRoundTripAndValidation`: Verified TLV serialization of full contacts, phone limits, and email formats.
- `urlValidationAndSafetyAnalysisTrickyExamples`: Verified tricky examples (`user@evil.example`, `xn--80ak6aa92e.com`, `[::1]`, port 8443, forbidden schemes).
- `fuzzTenThousandRandomByteArraysNeverThrow`: 10,000 trials of random byte sequences safely decoded into typed failure results without throwing.

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

BUILD SUCCESSFUL in 28s
151 actionable tasks: 31 executed, 120 up-to-date
```
