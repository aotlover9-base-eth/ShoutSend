# Step 29 Verification Report: Capability Probe and Calibration Engine

## 1. Overview
Implemented the audio capability probe and loopback calibration engine. This step delivers:
1. Static hardware capability inspection (`CapabilityProbe`) evaluating audio sample rates, low-latency and pro-audio features, UNPROCESSED mic availability, and audio effects.
2. Pure Kotlin calibration engine in `:modem` (`TestSequence`, `CalibrationAnalyzer`, `CalibrationThresholds`, `CalibrationRecommender`, `LinkTest`) evaluating 18 tones per band against empirical SNR and in-band variation thresholds.
3. Full-duplex loopback calibration runner (`CalibrationRunner`) with explicit half-duplex exception, hardware route warnings, progress tracking, and cancellation safety.
4. Serialized persistence data model (`CalibrationResult`) storing calibration summaries into the Settings store.

## 2. Implementation Summary
- `AudioStreamSource`: Common streaming interface implemented by both `Modulator` and `TestSequence` for zero-allocation playback through `AcousticTransmitter`.
- `TestSequence`:
  - 1000 ms initial ambient noise silence.
  - Retro: 1000 Hz marker tone (200 ms) + 16 data tones + 2 sync tones (150 ms each, 50 ms separation).
  - 500 ms inter-band transition.
  - Stealth: 16500 Hz marker tone (200 ms) + 16 data tones + 2 sync tones (150 ms each, 50 ms separation).
  - Smooth 5 ms cosine window transitions on all tone bursts.
- `CalibrationAnalyzer` & `CalibrationThresholds`:
  - Ambient noise floor calculation before tone onset.
  - Sliding Goertzel marker detection sustaining $\ge 50$ ms above detection threshold.
  - Goertzel power extraction across all 18 tones per band.
  - Verdict qualifications:
    - GOOD: SNR $\ge 20$ dB, in-band variation $\le 12$ dB.
    - USABLE: SNR $\ge 12$ dB, in-band variation $\le 18$ dB.
    - POOR: SNR $\ge 6$ dB, in-band variation $\le 24$ dB.
    - UNUSABLE: Otherwise or marker not detected.
- `CalibrationRecommender`:
  - Stealth recommended only if Stealth is GOOD.
  - Otherwise Retro: FAST if GOOD, NORMAL if USABLE, ROBUST if POOR or UNUSABLE.
- `LinkTest`:
  - Canonical payload `"ShoutSend Link Quality Test v1"`.
  - Pure function scoring quality $0..100$ and rating EXCELLENT, GOOD, FAIR, POOR.
- `CapabilityProbe`: Inspects audio rates, buffer sizes, features, volume, and route without modifying state.
- `CalibrationRunner`: Runs loopback calibration coordinating receiver and transmitter with explicit half-duplex exception.
- `CalibrationResult`: `kotlinx.serialization` model storing result summaries into `Settings.lastCalibration`.

## 3. Verification Results
```
> Task :modem:test --tests "app.shoutsend.modem.calibration.*"
BUILD SUCCESSFUL in 14s
Tests executed:
- testTestSequenceRendersContinuousSignalWithoutNanOrZero: PASSED
- testAnalyzerDetectsCleanSignalAndRecommendsStealth: PASSED (Retro GOOD, Stealth GOOD, Stealth Normal recommended)
- testAnalyzerWithSimulatedWeakUltrasoundPhoneFromStep22: PASSED (Stealth rolls off >12 dB, Retro recommended)
- testMarkerDetectionWithRandomStartDelays: PASSED (200ms, 650ms, 1200ms delays accurately detected)
- testAllSilentRecordingGivesUnusableWithClearReason: PASSED (UNUSABLE, "Microphone silent")
- testRecommendationTruthTable: PASSED (all 5 priority rules verified)
- testLinkQualityScore: PASSED (flawless >= 90, moderate 60..85, 100% loss = 0)

> Task :app:testDevDebugUnitTest --tests "app.shoutsend.core.calibration.*" --tests "app.shoutsend.core.capability.*"
BUILD SUCCESSFUL in 25s
Tests executed:
- CapabilityReportTest.testCapabilityReportFieldsAndSummaryFormatting: PASSED
- CalibrationResultTest.testJsonSerializationRoundtrip: PASSED
- CalibrationResultTest.testMalformedJsonReturnsNullGracefully: PASSED
- CalibrationRunnerTest.testSuccessfulCalibrationLoopbackAndProgress: PASSED (progress 0.05 -> 1.0, both bands GOOD)
- CalibrationRunnerTest.testHardwareWarningsReported: PASSED (HEADPHONES_CONNECTED, VOLUME_LOW)
- CalibrationRunnerTest.testCancellationStopsReceiverAndClearsTap: PASSED

> Task :app:verifyChirpRules
BUILD SUCCESSFUL in 39s
- checkNoHardcodedStyle: PASSED
- verifyPermissions: PASSED (Strict offline permissions policy)
- checkModemPurity: PASSED (100% pure Kotlin JVM, 0 Android imports in :modem)
- verifyUserApkClean: PASSED (100% clean of dev tools)

OWNER RUN: Calibration on physical device is pending UI implementation (Step 45).
```

## 4. Compliance
- Rule 21: Clean architectural boundary; analysis math is pure Kotlin JVM in `:modem`.
- Rule 22: Zero heap allocations on the audio streaming and Goertzel evaluation loops.
- Rule 23: Non-blocking audio loop free of logging.
- Rule 24: Completely offline, zero internet permissions.
- Rule 25: Device model in capability report is never transmitted acoustically, stored in Vault, or logged.
- Rule 27: Physical acoustic propagation modeling verified using ChannelSimulator scenarios.
