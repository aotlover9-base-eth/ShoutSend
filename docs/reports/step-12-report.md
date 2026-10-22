# Step 12 Report: Packet Format, Fragmentation, and Reassembly

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 12 delivers the transport packetization layer in `:modem` (`app.shoutsend.modem.packet`) conforming to Protocol v1, Rules 21, 22, 27, and 28.

Key achievements:
- **Frame Framing & Header Protocol:** Formally implemented 4-byte frame header (version 1, 3-bit message type, 3-bit flags, 8-bit message ID, 4-bit fragment index, 4-bit fragment count, 8-bit payload length) and CRC-16/CCITT-FALSE suffix over header and payload.
- **Framing Encoders & Decoders:** `FrameEncoder` packages payload slices into exact $k$-byte codewords padded with zeros; `FrameDecoder` rigorously checks version mismatch, CRC error, payload length bounds, and fragment index/count validity, returning typed `FrameDecodeResult`.
- **Fragmentation & Limits:** `Fragmenter` enforces strict bounds (`MAX_AIR_BYTES = 320`, `MAX_FRAGMENTS = 16`, non-empty payloads) and supports injected random message IDs.
- **Robust Receiver Message Assembler:** `MessageAssembler` performs multi-frame reconstruction with deterministic duplicate suppression, stream conflict detection (auto-reset on conflicting message ID properties), injectable clock timeout expiry (default 60 seconds), and immutable `AssemblyProgress` snapshots for UI consumption.
- **Pure Air-Time Duration Estimator:** `AirTime.estimate` computes physical transmission air-time in seconds across profiles, timing presets, and repeat passes. Hand-calculated test vectors verified exact to $10^{-6}$ seconds.

---

## 2. Implemented Architecture

### 2.1 Package Map (`app.shoutsend.modem.packet`)
- `MessageType.kt`: Enum defining payload types `TEXT` (0), `URL` (1), `WIFI` (2), `CONTACT` (3); IDs 4..7 reserved.
- `MessageFlags.kt`: 3-bit flags for encryption (`isEncrypted`), Brotli/Huffman compaction (`isCompact`), and extension (`isReserved`).
- `FrameHeader.kt`: 4-byte header structure with invariant verification.
- `Frame.kt`: Frame entity pairing `FrameHeader` with `ByteArray` payload slice.
- `FrameEncoder.kt`: Serializes `Frame` into $k$-byte array with big-endian CRC-16 suffix over header + payload.
- `FrameDecoder.kt`: Deserializes $k$-byte array, returning `Success(Frame)` or typed `Failure(FrameDecodeError)`.
- `Fragmenter.kt`: Slices messages into frames with max 42 bytes payload (for STANDARD $k=48$).
- `MessageAssembler.kt`: Reassembles fragments into `ReceivedMessage`, manages timeout, and deduplicates frames.
- `AirTime.kt`: Pure mathematical estimator of acoustic on-air transmission time.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Packet Protocol Test Suite (11 Tests Passing)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.packet.PacketProtocolTest"`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test

BUILD SUCCESSFUL in 3s
```
Test results from XML report:
- `airTimeHandCalculatedExamples`: Hand-computed test cases (8.28s, 5.88s, 7.56s) verified exact.
- `fragmentationAndAssemblyForCanonicalPayloadSizes`: Canonical sizes (1, 41, 42, 43, 84, 320 bytes) verified.
- `size321AndEmptyPayloadsRejected`: Empty payloads and 321-byte payloads rejected with `IllegalArgumentException`.
- `framesArrivingInRandomOrderReassembleCleanly`: 8-fragment payload shuffled with seeded RNG assembled cleanly.
- `duplicateFramesAreIgnoredAndEmittedExactlyOnce`: Redundant frames ignored; `Complete` emitted exactly once.
- `missingFragmentArrivingLaterCompletesSuccessfully`: Fragments arriving out of order with gaps assembled cleanly.
- `corruptedCrcFrameRejectedByDecoder`: Corrupted payload bytes rejected with `FrameDecodeError.BadCrc`.
- `frameDecoderValidationErrors`: Verified `VersionMismatch`, `BadLength`, and `BadFragmentField`.
- `conflictingMessageIdStreamDetectedAndReset`: Conflicting stream triggers conflict counter and state reset.
- `timeoutExpiryCleansPendingMessage`: Injected clock test proves expired fragments are purged after 60s.
- `sameMessageSentTwiceWithDifferentMessageIdsDeliveredTwice`: Sequential messages delivered independently.

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

BUILD SUCCESSFUL in 43s
151 actionable tasks: 48 executed, 103 up-to-date
```

### 3.3 Lint Execution
Command: `./gradlew lintDevDebug lintUserDebug`
```text
> Task :app:lintDevDebug
> Task :app:lintUserDebug
Wrote HTML report to app/build/reports/lint-results-userDebug.html

BUILD SUCCESSFUL in 18s
57 actionable tasks: 11 executed, 46 up-to-date
```

### 3.4 Permissions Audit
Merged manifests inspect zero permissions (`RECORD_AUDIO` and `VIBRATE` arrive in Step 26; 0 `INTERNET` permissions).
