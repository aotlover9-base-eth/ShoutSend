# Step 08 Report: Bit Utilities, CRC-16, and Whitening

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 8 delivers the foundational bit-manipulation, error-detection, and whitening components for the ShoutSend acoustic modem protocol, implementing Rules 21, 22, and 27.

Key components:
- **Bit-Level Serialization:** `BitWriter` and `BitReader` stream arbitrary bit widths MSB-first with zero per-bit allocation overhead.
- **Error Detection:** Table-driven `Crc16` (CRC-16/CCITT-FALSE) with known-answer validation.
- **Spectral Whitening:** `Whitener` using a PN9 LFSR ($x^9 + x^5 + 1$) to eliminate repetitive tone runs and achieve a uniform symbol distribution.
- **Cryptographic & Diagnostic Utilities:** `ByteUtils` providing constant-time array comparison and test hex dumping.

---

## 2. Implemented Architecture

### 2.1 Bit Serialization (`BitWriter.kt`, `BitReader.kt`)
- `BitWriter`: Growable byte array buffer packing bits MSB first. Supports writing 0 to 32 bits, individual bits, single bytes, and byte arrays.
- `BitReader`: Reads bits MSB first from a backing byte array. Emits typed `BitStreamException` on underflow.

### 2.2 CRC-16/CCITT-FALSE (`Crc16.kt`)
- Parameters:
  - Polynomial: `0x1021` ($x^{16} + x^{12} + x^5 + 1$)
  - Initial Value: `0xFFFF`
  - RefIn / RefOut: `false` (no reflection)
  - Final XOR: `0x0000`
- Table-driven 256-word precomputed lookup table.
- Supports incremental updates (`update`) and one-shot calculation (`calculate`).

### 2.3 PN9 Scrambler (`Whitener.kt`)
- Polynomial: $x^9 + x^5 + 1$ (LFSR mask `0x1FF`).
- Seed Mapping: Non-zero 9-bit initial state derived from integer seed: `((seed and 0x0F) shl 5) xor 0x1FF`.
- Involutory: Since whitening is an XOR keystream operation, `whiten(whiten(bytes, seed), seed) == bytes`.
- Allocation-free in-place method: `whitenInPlace(bytes, seed, offset, length)`.

### 2.4 Diagnostic & Security Utilities (`ByteUtils.kt`)
- `bytesToHex`: For unit testing and diagnostic output only (never called on user payload logs per Rule 25).
- `constantTimeEquals`: Bitwise XOR accumulator preventing timing side-channel attacks during packet authentication.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Known-Answer Test & Whitener Statistics
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.protocol.Crc16Test" --tests "app.shoutsend.modem.protocol.WhitenerTest" --info`
```text
Crc16Test > knownAnswerTest123456789 STANDARD_OUT
    CRC Known Answer Test: '123456789' -> 0x29B1

WhitenerTest > statisticalTestZeroBufferRunsAndUniformity STANDARD_OUT
    PN9 Whitener Statistical Results:
      Overall measured maximum identical run: 3 symbols (limit <= 6)
      Chi-square stats: [12.00, 11.47, 12.27, 10.13, 12.80, 6.67, 12.27, 12.53, 10.13, 12.53, 10.40, 12.53, 9.60, 12.80, 11.73, 11.47]
```
- **CRC-16 Known Answer Test**: ASCII `"123456789"` matched exactly `0x29B1` (PASS).
- **Whitener Run Length**: Across all 16 seeds (0..15) on an all-zero 60-byte payload (120 MFSK16 symbols), the maximum consecutive identical symbol run was **3 symbols** (well below the required threshold $\le 6$).
- **Uniformity**: Chi-square values against uniform distribution across 16 symbols ($df = 15$) ranged between **6.67 and 12.80**, far below the critical value 25.00 ($p = 0.05$).

### 3.2 Modem Test Suite
Command: `./gradlew :modem:test`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test

BUILD SUCCESSFUL in 9s
8 actionable tasks: 3 executed, 5 up-to-date
```
Tests passing:
- `BitStreamTest`: roundTripRandomBitWidths (PASS), throwsOnReadPastEnd (PASS)
- `Crc16Test`: knownAnswerTest123456789 (PASS), emptyInputGivesInitialValue (PASS), incrementalMatchesOneShot (PASS)
- `WhitenerTest`: whiteningIsItsOwnExactInverse (PASS), inPlaceMatchesCopying (PASS), statisticalTestZeroBufferRunsAndUniformity (PASS)
- `ByteUtilsTest`: bytesToHexCorrectness (PASS), constantTimeEqualsCorrectness (PASS)
- All previous Step 6 & 7 tests (PASS)

### 3.3 Full Verification Suite
Command: `./gradlew check`
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:verifyChirpRules
> Task :app:lint
> Task :app:check

BUILD SUCCESSFUL in 35s
131 actionable tasks: 28 executed, 103 up-to-date
```

---

## 4. Checks NOT Run
- None.

## 5. Deviations from Rules
- None.

## 6. OWNER RUNS
- None. Pure Kotlin JVM components.

## 7. Suggested Next Step
- Step 9: Reed-Solomon field and encoder (`GaloisField`, `RsEncoder`, generator polynomials, syndrome calculation).
