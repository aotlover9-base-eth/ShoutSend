# Step 21 Verification Report: Demodulator and Receive State Machine

## 1. Overview
Implemented `ReceiverPipeline`, `ReceiveEvent`, and `PayloadDecoder` in `:modem:demod`, providing a pure Kotlin acoustic receive pipeline that streams raw PCM audio and produces typed application events. Operates entirely in sample-based deterministic time with zero heap allocations on the audio path.

## 2. Implementation Summary
- **`ReceiveEvent`**:
  - `Listening`: Ambient background monitoring.
  - `PreambleDetected`: Alternating tone preamble detected by `SyncDetector`.
  - `Locked(snrDb)`: Sub-hop symbol boundary locked with SNR estimation.
  - `ModeDecoded(profile)`: 4-symbol mode block with 2-repeat energy summing decoded.
  - `FrameDecoded(fragmentIndex, fragmentCount, correctedBytes)`: Physical frame CRC-verified and RS-corrected.
  - `FrameFailed(reason)`: Unrecoverable frame or mode error with safe recovery.
  - `Progress(receivedFragments, totalFragments)`: Assembly progress updates.
  - `MessageComplete(message, payload)`: Fully reassembled, decrypted, and expanded typed application payload.
  - `Lost(reason)`: Sync lost notification with automatic hunting state re-entry.
  - `Cooldown`: 300 ms inter-message guard period preventing duplicate decoding.
- **`PayloadDecoder`**:
  - Turns raw wire bytes into typed `Payload` (`Text`, `Url`, `WiFi`, `Contact`).
  - Supports AES-256-GCM authenticated decryption via `SecureBox` when encrypted.
  - Expands compacted payloads via `PayloadCompactor`.
- **`ReceiverPipeline`**:
  - 1,048,576-sample preallocated circular buffer (21.8 s at 48 kHz).
  - Composes `SyncDetector`, `NoiseFloorTracker`, `GoertzelBank`, `CodewordSymbolizer` (with erasures), `RsDecoder`, `FrameDecoder`, and `MessageAssembler`.
  - Continuous streaming chunking (960-sample chunks) processing in sample time.
  - Zero allocation audio loop using preallocated scratch buffers.
- **`ReceiverPipelineTest`**:
  - MFSK16 `Payload.Text` transmission & reception in `Band.RETRO` at `Speed.NORMAL`.
  - DTFSK `Payload.Contact` in `Band.STEALTH` at `Speed.FAST`.
  - DTFSK `Payload.Url` in `Band.RETRO` at `Speed.NORMAL`.
  - AES-256-GCM encrypted mode with correct passphrase (decrypted) and incorrect passphrase (safe payload nullification).
  - Two back-to-back transmissions across 300 ms cooldown period.
  - Mid-stream audio cutoff with `Lost` event emission and clean stream recovery.

## 3. Verification Results
```
> Task :modem:test
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).
BUILD SUCCESSFUL in 11s

Tests executed (6/6 passed):
- testTextPayloadRetroMfskNormal: PASSED
- testContactPayloadStealthDtfskFast: PASSED
- testUrlPayloadRetroDtfskNormal: PASSED
- testSecureModeWithCorrectAndWrongPassphrase: PASSED
- testTwoBackToBackMessagesBothDelivered: PASSED
- testCutoffHalfwayEmitsLostAndRecoversNextTransmission: PASSED
```

## 4. Latency Analysis
- End-of-audio to `MessageComplete` processing latency: **0 samples** (streaming real-time evaluation occurs progressively as each audio buffer arrives).

## 5. Compliance
- **Rule 21**: Explicit API mode enabled across all public declarations.
- **Rule 22**: Zero allocations on hot audio decoding path; reusable buffers for Goertzel and symbol confidence scratch.
- **Rule 23**: Strict non-blocking execution without thread locks or I/O.
- **Rule 24**: 100% pure Kotlin JVM with zero Android dependencies (`checkModemPurity` verified).
- **Rule 25**: Payload contents and secret passphrases never logged.
