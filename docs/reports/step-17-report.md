# Step 17 Report: Modulator and Physical Transmission Assembly

**Date:** 2026-10-23
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 17 implements the end-to-end physical acoustic modulator in `:modem` (`app.shoutsend.modem.modulator`) conforming to Rules 21, 22 (zero allocations on streaming hot path), 23, 27, and 28. It connects message fragmentation (Step 12), systematic Reed-Solomon encoding (Step 9), symbolization with whitening (Step 11), and tone synthesis with windowing (Step 16) into complete on-air transmissions.

Key components delivered:
- **`OutgoingMessage` & `TransmissionPlan`:** Immutable specification of messages, physical frames, Reed-Solomon codewords, and continuous timeline `SymbolSchedule`.
- **`TransmissionBuilder`:** Assembles the physical transmission layout specified in Section 6 of Protocol v1:
  1. Lead-in silence (configurable, default 250 ms)
  2. Acquisition preamble (16 symbols alternating S0 and S1 at Normal 30ms timing)
  3. Synchronization word (8 fixed symbols `[3, 11, 7, 14, 1, 9, 5, 12]` at Normal 30ms timing)
  4. Mode block (2 symbols repeated once = 4 symbols at Normal 30ms timing)
  5. Physical frames repeated $R$ times (default 2):
     - 4-symbol frame marker `[13, 2, 10, 4]` at active speed
     - Whitened Reed-Solomon codeword symbols at active speed and scheme
  6. Tail silence (configurable, default 150 ms)
- **Fixed Whitening Seed Constant (`WHITEN_SEED_V1 = 0xA5`):** Standardized a constant non-zero LFSR whitening seed across all frames to eliminate receiver circular dependency on fragment index.
- **`Modulator`:** Exposes streaming chunk rendering (`render(startSample, out, offset, count)`) with zero heap allocations in steady state, and monolithic `modulateToPcm16(plan)` for testing and simulation.
- **`WavWriter`:** Test utility in `src/test` writing standard 16-bit mono 48 kHz RIFF/WAVE files to `modem/build/outputs/` (`shoutsend_retro_sample.wav` and `shoutsend_stealth_sample.wav`). Documented in `docs/README_AUDIO_SAMPLES.md`.

---

## 2. Experimental Measurements & Verification

### 2.1 Duration Agreement with AirTime.estimate
Evaluated across 4 diverse profiles (MFSK16 Normal Stealth, DTFSK Fast Retro, DTFSK Robust Stealth, MFSK16 Fast Retro) and payload sizes (1, 42, 84, 120 bytes):
- Measured `Modulator.durationSeconds` strictly matched `AirTime.estimate` within one symbol duration ($3..6$ ms ramp variance $\ll 20..60$ ms symbol duration).

### 2.2 Independent Test-Only Goertzel Decoding Sanity Checks
- **Preamble Region:** Energy concentrated strictly at S0 on even symbols and S1 on odd symbols ($>100\times$ ratio). Suppression on all 16 data tone frequencies exceeded $30$ dB.
- **Sync Word Region:** All 8 symbols decoded identically to `[3, 11, 7, 14, 1, 9, 5, 12]`.
- **Mode Block Region:** Both transmissions (4 symbols total) decoded without parity error, exactly recovering the active scheme, speed, and RS profile ID.
- **Frame Markers:** Decoded identically to `[13, 2, 10, 4]` across all frames.
- **Codewords (MFSK16 & DTFSK):** Full Goertzel tone detection followed by symbol demapping and de-whitening reconstructed 100% of systematic Reed-Solomon codeword bytes byte-for-byte.
- **Repeat Consistency:** Frame repeat 1 and repeat 2 decoded to identical symbol sequences and identical codeword bytes.

### 2.3 Steady-State Heap Allocation Benchmark
Measured with `ThreadMXBean.getThreadAllocatedBytes`:
```text
Rendering 10 seconds of streaming audio (1000 calls of 480 samples @ 48 kHz):
Allocated bytes: 0 bytes
```
*Direct array lookup in `RaisedCosineWindow` (`directTable[rampLength]`) eliminates boxing and lambda overhead on the rendering hot path.*

### 2.4 WAV Sample Output
Generated test files:
- `modem/build/outputs/shoutsend_retro_sample.wav` (466 KB, audible 1.5 - 3.5 kHz MFSK16 modem chirp)
- `modem/build/outputs/shoutsend_stealth_sample.wav` (275 KB, near-ultrasonic 17.6 - 20.0 kHz DTFSK chirp)

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Test Suite Execution (`./gradlew :modem:test`)
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:compileTestKotlin
> Task :modem:test

BUILD SUCCESSFUL in 20s
8 actionable tasks: 5 executed, 3 up-to-date
```

### 3.2 Full Project Check (`./gradlew check`)
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
> Task :app:lintDevDebug
> Task :app:check

BUILD SUCCESSFUL in 24s
131 actionable tasks: 28 executed, 103 up-to-date
```

---

## 4. Next Step
Proceed to **Step 18: Goertzel detector** (sliding Goertzel algorithm, precomputed coefficients, block-based detection, tone energy extraction, frequency accuracy tests).
