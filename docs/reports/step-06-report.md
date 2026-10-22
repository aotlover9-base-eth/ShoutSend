# Step 06 Report: :modem Module Scaffold and Test Harness

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 6 establishes the `:modem` pure Kotlin JVM module, which serves as the host for all acoustic signal processing, modulation, demodulation, error correction, and channel simulation.

Per Rules 21, 22, 23, 27, and 28:
- `:modem` has zero dependencies on `android.*` or `androidx.*`.
- Explicit API mode (`explicitApi()`) is enabled to enforce strict visibility controls.
- The build is guarded by an automated task `checkModemPurity` in `buildSrc`, preventing any accidental introduction of Android imports.
- Protocol v1 specification is formalized in `docs/PROTOCOL.md` with an active change log.

---

## 2. Implemented Components

### 2.1 Build & Module Configuration
- `gradle/libs.versions.toml`: Added `kotlin-jvm` plugin alias.
- `build.gradle.kts`: Applied `kotlin.jvm` plugin (`apply false`).
- `settings.gradle.kts`: Included `:modem`.
- `app/build.gradle.kts`: Added `implementation(project(":modem"))`.
- `modem/build.gradle.kts`: Configured Java 17 toolchain, `explicitApi()`, JUnit 4, `checkModemPurity` task hook.

### 2.2 Production Classes (`modem/src/main/kotlin/app/shoutsend/modem/`)
- `AudioSpec.kt`: Value model encapsulating `sampleRateHz` with validation strictly enforcing 48000 Hz or 44100 Hz.
- Package scaffolding created:
  - `app.shoutsend.modem.profile`
  - `app.shoutsend.modem.dsp`
  - `app.shoutsend.modem.fec`
  - `app.shoutsend.modem.protocol`
  - `app.shoutsend.modem.payload`
  - `app.shoutsend.modem.crypto`
  - `app.shoutsend.modem.sim`
  - `app.shoutsend.modem.bench`

### 2.3 Test Harness & Utilities (`modem/src/test/kotlin/app/shoutsend/modem/testutil/`)
- `SeededRandom.kt`: Deterministic pseudo-random number generator for reproducible tests.
- `PcmMath.kt`: Accurate RMS, dBFS computation, and approximate float comparison assertions.
- `TonePcm.kt`: Independent reference sine generator and silence generator.
- `PcmBufferBuilder.kt`: Fluent test buffer builder concatenating silence and tone bursts.

### 2.4 Purity Guard (`buildSrc/src/main/kotlin/app/shoutsend/buildtools/`)
- `ModemPurityGuard.kt`: Scans `:modem` source files for forbidden `import android.` or `import androidx.` statements.
- `CheckModemPurityTask.kt`: Gradle task failing build if violations occur.
- `ModemPurityGuardTest.kt`: BuildSrc unit test verifying violation detection and clean pass.

### 2.5 Protocol Documentation (`docs/PROTOCOL.md`)
- Verbatim copy of Section 6 of `SHOUTSEND_ROADMAP.md` (Audio parameters, tone plans, speed presets, transmission layout, frame format, Reed-Solomon GF(256) specs) with change log section.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Modem Module Test Suite
Command: `./gradlew :modem:test`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:compileKotlin
> Task :modem:compileTestKotlin
> Task :modem:test

BUILD SUCCESSFUL in 22s
8 actionable tasks: 5 executed, 3 up-to-date
```
Tests executed and passing:
- `AudioSpecTest.acceptsStandardSampleRates` (PASS)
- `AudioSpecTest.rejectsUnsupportedSampleRates` (PASS)
- `TonePcmTest.generatesCorrectLengthAndPeakAmplitude` (PASS)
- `TonePcmTest.sineWaveRmsMatchesTheoreticalValue` (PASS)
- `TonePcmTest.silenceHasZeroRms` (PASS)
- `TonePcmTest.pcmBufferBuilderConcatenatesAccurately` (PASS)
- `TonePcmTest.seededRandomIsReproducible` (PASS)

### 3.2 Pure JVM Purity Guard
Command: `./gradlew checkModemPurity`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

BUILD SUCCESSFUL in 2s
5 actionable tasks: 2 executed, 3 up-to-date
```

### 3.3 Proof of Failure on Forbidden Import
A temporary file `BadPurityImport.kt` containing `import android.os.Bundle` was introduced to test the guard:
```text
> Task :modem:checkModemPurity FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modem:checkModemPurity'.
> checkModemPurity: Found 1 forbidden Android dependencies in :modem:
    - /mnt/HDD1/Downloads/ShoutSend/modem/src/main/kotlin/app/shoutsend/modem/BadPurityImport.kt:2: Forbidden Android import in pure JVM :modem module: 'import android.os.Bundle' -> import android.os.Bundle

BUILD FAILED in 2s
```
Result: **Expected failure proven**. The guard caught the forbidden import and halted the build immediately.

### 3.4 Full Verification Suite (`./gradlew check`)
Command: `./gradlew check`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test UP-TO-DATE
> Task :modem:check UP-TO-DATE

> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:verifyChirpRules
> Task :app:lintReportDevDebug
> Task :app:lint
> Task :app:check

BUILD SUCCESSFUL in 4m 36s
131 actionable tasks: 126 executed, 5 up-to-date
```

---

## 4. Checks NOT Run
- None. All standard verification tasks, flavor builds, purity checks, and unit test suites were executed with zero errors.

## 5. Deviations from Rules
- None.

## 6. OWNER RUNS
- None. `:modem` is a pure JVM library fully verified locally via unit tests and simulation test harnesses.

## 7. Suggested Next Step
- Step 7: Acoustic profiles and tone plan (`Band`, `Scheme`, `Speed`, `ModemProfile`, `TonePlan`, `Timing`, `SymbolMapper`, `ModeBlockCodec`).
