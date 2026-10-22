# Step 10 Report: Reed-Solomon Decoder (Errors and Erasures)

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 10 delivers a robust, high-performance Errors-and-Erasures Reed-Solomon decoder (`RsDecoder`) in `:modem` conforming to Rules 21, 22, and 27.

Key highlights:
- **Error & Erasure Capacity:** Corrects any combination of unknown byte errors $v$ and known byte erasures $\mu$ satisfying $2v + \mu \le nsym$.
- **Algorithm Architecture:** 
  1. Syndrome evaluation on generator roots $\alpha^0 \dots \alpha^{nsym-1}$.
  2. Erasure locator polynomial $\Lambda_{erasures}(x)$ construction.
  3. Berlekamp-Massey algorithm initialized directly from $\Lambda_{erasures}(x)$ on syndromes $S_i$.
  4. Chien root search with exact degree-match validation.
  5. Forney algorithm incorporating the $X_l^{1-b} = X_l$ scaling factor for generator roots beginning at $b=0$.
  6. Post-correction syndrome re-computation ensuring zero corruption escapes.
- **Fail-Safe Integrity:** The decoder never throws exceptions on corrupted inputs, returning typed `RsDecodeResult.Failure`. In 10,000 beyond-capacity trials (7 byte errors in $RS(60,48)$), the decoder reported **10,000 honest failures (100.0%)** and **0 miscorrections (0.0000%)**, significantly surpassing the < 1.0% requirement.
- **Decoding Throughput:** Achieved **65,522 codewords/sec** for STANDARD $RS(60,48)$ and **100,932 codewords/sec** for LIGHT $RS(60,54)$ on a single thread.

---

## 2. Implemented Architecture

### 2.1 Result Model (`RsDecodeResult.kt`)
- `RsDecodeResult.Corrected(data, errorCount, erasureCount)`: Returned upon successful correction and post-check verification.
- `RsDecodeResult.Failure(reason)`: Descriptive error result returned on underflow, degree mismatch, zero-denominator Forney singularity, capacity exhaustion, or non-zero post-correction syndromes.

### 2.2 Decoder Engine (`RsDecoder.kt`)
- `RsDecoder.decode(received: ByteArray, erasurePositions: IntArray, profile: RsProfile)`:
  - Validates codeword dimensions and erasure position bounds with deduplication.
  - Returns early with 0 allocations if all syndromes are zero and zero erasures were flagged.
  - Constructs erasure locator $\Lambda_{erasures}(x) = \prod_{p \in E} (1 - X_p \cdot x)$.
  - Executes Berlekamp-Massey starting at step $\mu$ with initial register state $\Lambda_{erasures}(x)$.
  - Performs Chien root search and Forney magnitude calculation.
  - Re-computes all $nsym$ syndromes over the corrected array before returning.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Failure and Miscorrection Statistics (10,000 Trials)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.fec.RsDecoderTest.testOneBeyondLimitFailureAndMiscorrectionRate" --info`
```text
RsDecoderTest > testOneBeyondLimitFailureAndMiscorrectionRate STANDARD_OUT
    Beyond-Capacity (7 errors in RS(60,48)) Results over 10000 trials:
      Honest Failures Detected: 10000 (100.0%)
      Miscorrections: 0 (Rate: 0.0000%)
```

### 3.2 Decoder Speed Benchmark (10,000 Codewords per Profile)
Command: `./gradlew :modem:test --tests "app.shoutsend.modem.fec.RsDecoderTest.benchmarkDecodeTenThousandCodewords" --info`
```text
RsDecoderTest > benchmarkDecodeTenThousandCodewords STANDARD_OUT
    RsDecoder Benchmark [LIGHT]:
      Decoded 10000 codewords in 99.08 ms (100932.44 codewords/sec)
    RsDecoder Benchmark [STANDARD]:
      Decoded 10000 codewords in 152.62 ms (65522.41 codewords/sec)
    RsDecoder Benchmark [STRONG]:
      Decoded 10000 codewords in 340.86 ms (29337.41 codewords/sec)
```

### 3.3 Test Suite Execution
Command: `./gradlew :modem:test`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test

BUILD SUCCESSFUL in 6s
8 actionable tasks: 3 executed, 5 up-to-date
```
Tests passing:
- `testEveryProfileRandomErrorsUpToMaxErrors`: 2,000 random codewords per profile (6,000 total) corrected with 100% accuracy.
- `testErasuresOnlyUpToNsym`: Erasures only up to $nsym$ tested across all profiles and corrected with 100% accuracy.
- `testMixedErrorsAndErasuresAtLimit`: Tested all combinations of $2e + \mu = 12$ in $RS(60,48)$ with 100% recovery.
- `testOneBeyondLimitFailureAndMiscorrectionRate`: 10,000 trials, 0 miscorrections (0.0000% < 1.0%).
- `testErrorInParityBytesOnly`: Corrupted parity bytes exclusively, data restored cleanly.
- `testAllZeroAndAll0xFFMessages`: Boundary edge-case patterns decoded cleanly.
- `testErasuresGivenWhenBytesAreActuallyCorrect`: False-alarm erasure flags handled gracefully without corrupting valid data.

### 3.4 Full Verification Suite
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

BUILD SUCCESSFUL in 46s
131 actionable tasks: 28 executed, 103 up-to-date
```

---

## 4. Checks NOT Run
- None.

## 5. Deviations from Rules
- None.

## 6. OWNER RUNS
- None. Pure Kotlin JVM decoder algorithms.

## 7. Suggested Next Step
- Step 11: Codeword symbolizer and erasure mapping (`Symbolizer`, confidence thresholds, erasure mapping from weak symbol amplitudes).
