# Step 11 Report: Codeword Symbolizer and Erasure Mapping

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 11 delivers `CodewordSymbolizer` in `:modem` bridging byte-oriented Reed-Solomon codewords with physical audio symbols (MFSK16 and DTFSK) conforming to Rules 21, 22, 27, and 28.

Key highlights:
- **Symbol Generation & Whitening:** Integrates PN9 whitening seeded by frame ID before converting bytes to 4-bit (MFSK16) or 6-bit (DTFSK) symbols. Exactly 120 symbols for MFSK16 and 80 symbols for DTFSK on 60-byte codewords.
- **Symbol-to-Byte Erasure Mapping:** Tracks symbol bit bounds across byte boundaries. For DTFSK (where 4 symbols span 3 bytes), symbols spanning byte seams flag both touching bytes as erasures whenever symbol confidence falls below threshold.
- **Burst Damage Bounds:** Formally verified that burst errors of length $L \in [1, 12]$ consecutive symbols affect at most $\lceil L \cdot bitsPerSymbol / 8 \rceil + 1$ bytes.
- **End-to-End Erasure Gain:** Evaluated 500-message trials across SER levels ($0\%, 2\%, 5\%, 10\%$). Erasure marking increases recovery rate at $5\%$ SER from $58.8\%$ to **$99.8\%$** ($+41.0\%$) and at $10\%$ SER from $5.2\%$ to **$68.8\%$** ($+63.6\%$).
- **Intra-Codeword Interleaving Architectural Decision:** Interleaving symbols within a single codeword was evaluated and explicitly rejected; RS handles localized contiguous byte corruptions best, while interleaving would disperse burst damage across more bytes, exceeding RS correction bounds.

---

## 2. Implemented Components

### 2.1 `CodewordSymbolizer.kt`
- `encode(codeword: ByteArray, scheme: Scheme, frameSeed: Int): IntArray`:
  - Applies PN9 whitening sequence seeded by `frameSeed`.
  - Serializes whitened stream into `IntArray` of symbol values via `SymbolMapper`.
- `decode(symbols: IntArray, confidences: FloatArray, scheme: Scheme, frameSeed: Int, byteCount: Int, erasureThreshold: Float): SymbolizerResult`:
  - De-serializes symbols to bytes via `SymbolMapper`.
  - Maps low-confidence symbols (`confidence < erasureThreshold`, default `0.5f`) to byte erasure positions.
  - De-whitens reconstructed bytes with identical seeded PN9 sequence.
  - Returns `SymbolizerResult(bytes: ByteArray, erasurePositions: IntArray)`.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Burst Damage Table ($L = 1 \dots 12$ Consecutive Symbols)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.protocol.CodewordSymbolizerTest.burstDamageAnalysisTable"`
```text
### Symbol Burst Impact Table
| Burst Length (L) | MFSK16 Max Bytes | MFSK16 Bound | DTFSK Max Bytes | DTFSK Bound |
| :--- | :--- | :--- | :--- | :--- |
|  1 symbols       |  1 bytes         |  2 bytes      |  2 bytes        |  2 bytes     |
|  2 symbols       |  2 bytes         |  2 bytes      |  3 bytes        |  3 bytes     |
|  3 symbols       |  2 bytes         |  3 bytes      |  3 bytes        |  4 bytes     |
|  4 symbols       |  3 bytes         |  3 bytes      |  4 bytes        |  4 bytes     |
|  5 symbols       |  3 bytes         |  4 bytes      |  5 bytes        |  5 bytes     |
|  6 symbols       |  4 bytes         |  4 bytes      |  6 bytes        |  6 bytes     |
|  7 symbols       |  4 bytes         |  5 bytes      |  6 bytes        |  7 bytes     |
|  8 symbols       |  5 bytes         |  5 bytes      |  7 bytes        |  7 bytes     |
|  9 symbols       |  5 bytes         |  6 bytes      |  8 bytes        |  8 bytes     |
| 10 symbols       |  6 bytes         |  6 bytes      |  9 bytes        |  9 bytes     |
| 11 symbols       |  6 bytes         |  7 bytes      |  9 bytes        | 10 bytes     |
| 12 symbols       |  7 bytes         |  7 bytes      | 10 bytes        | 10 bytes     |
```

### 3.2 End-to-End Recovery Rate with & without Erasure Marking (500 Messages per SER)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.protocol.CodewordSymbolizerTest.endToEndRecoveryRateEvaluation"`
```text
### Recovery Rate Results over 500 messages per SER:
| Symbol Error Rate (SER) | Without Erasures | With Erasures | Improvement |
| :--- | :--- | :--- | :--- |
|  0% SER                  | 100.0%           | 100.0%         | + 0.0%       |
|  2% SER                  |  99.6%           | 100.0%         | + 0.4%       |
|  5% SER                  |  58.8%           |  99.8%         | +41.0%       |
| 10% SER                  |   5.2%           |  68.8%         | +63.6%       |
```

### 3.3 Test Suite Execution
Command: `./gradlew :modem:test`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test

BUILD SUCCESSFUL in 3s
8 actionable tasks: 2 executed, 6 up-to-date
```
Tests passing:
- `symbolCountExactFor60ByteCodewords`: 120 symbols for MFSK16, 80 symbols for DTFSK.
- `cleanRoundTripProducesZeroErasures`: Complete clean encode-decode identity with 0 erasures across all schemes.
- `tableDrivenDtfskSixSymbolWindowMapping`: Verified exact byte coverage for 6-symbol DTFSK window.
- `burstDamageAnalysisTable`: Proved max byte damage conforms to theoretical ceiling for all burst lengths.
- `endToEndRecoveryRateEvaluation`: Demonstrated massive recovery gains from symbol confidence erasure mapping.
