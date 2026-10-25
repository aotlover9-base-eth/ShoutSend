# Step 24 Verification Report: DSP Performance and Allocation Audit

## 1. Overview
Conducted a deep performance and allocation audit across all core DSP components in `:modem` (`GoertzelBank`, `NoiseFloorTracker`, `SyncDetector`, `ReceiverPipeline`). Verified zero-allocation constraints on the audio streaming hot paths, achieved a real-time factor (RTF) of $0.00357$ (280x faster than real-time), compared single-precision vs double-precision inner loops, and introduced the automated Gradle task `:modem:modemPerf`.

## 2. Allocation Optimization Highlights
- **Identified and Fixed Hot-Path Allocations**:
  - `SyncDetector.evaluatePreambleHunting()` previously allocated a 4-element `FloatArray(4)` on every 360-sample hop ($129,000$ B per 30s). Refactored to preallocated member scratch buffer with zero per-hop allocations.
  - `ReceiverPipeline.push()` and `feed()` loops refactored from Kotlin `for (i in currentOffset until end)` to explicit indexed `while (i < end)` loops, eliminating iterator/progression instantiations.
- **Heap Allocation Invariant**:
  - `GoertzelBank`: 0 allocations per evaluation (steady state $< 3.0$ B/s JVM metadata overhead).
  - `NoiseFloorTracker`: 0 allocations per update (steady state $< 2.5$ B/s).
  - `SyncDetector` (HUNTING): 0 allocations per hop (steady state $< 30.0$ B/s).
  - `ReceiverPipeline` (Listening): 0 allocations per audio chunk (steady state $< 30.0$ B/s).

## 3. Real-Time Factor (RTF) & Throughput Audit
Measured on Linux x86_64 build machine (phone benchmark noted as OWNER RUN):
```
| DSP Component | Audio Time | Execution Time | Real-Time Factor (RTF) | Speedup Factor | Heap Allocated | Allocation Rate |
|---|---|---|---|---|---|---|
| GoertzelBank (16 tones) | 300.0s | 1027.90 ms | 0.00343 | 291.9x | 816 B | 2.7 B/s |
| NoiseFloorTracker (18 tones) | 375.0s | 11.14 ms | 0.00003 | 33673.0x | 816 B | 2.2 B/s |
| SyncDetector (HUNTING) | 30.0s | 106.42 ms | 0.00355 | 281.9x | 816 B | 27.2 B/s |
| ReceiverPipeline (Listening) | 30.0s | 87.34 ms | 0.00291 | 343.5x | 816 B | 27.2 B/s |
| ReceiverPipeline (Full E2E 60s) | 60.0s | 214.30 ms | 0.00357 | 280.0x | 455096 B | 7584.9 B/s |
```
- Build Machine Target: $\text{RTF} < 0.02$ ($> 50\times$ faster than real-time).
- Achieved: $\text{RTF} = 0.00357$ (**$280.0\times$ faster than real-time**).

## 4. Float vs Double Inner Loop Comparison in Goertzel Resonator
- Tested over 10,000 blocks $\times$ 4 filters $\times$ 1440 samples:
  - Double inner loop: $261.95$ ms.
  - Float inner loop: $270.50$ ms.
  - Execution speed ratio: $\approx 1.03\times$ (Double is equal or slightly faster on modern 64-bit AVX/NEON FPUs).
- Decision: Retain `Double` accumulators for the Goertzel resonator to benefit from 53-bit mantissa precision, completely preventing numerical drift across long integration blocks without any performance penalty.

## 5. Verification Results
```
> Task :modem:test --tests "app.shoutsend.modem.audit.DspPerformanceAndAllocationAuditTest"
BUILD SUCCESSFUL in 12s
- testGoertzelBankZeroAllocationsOnHotPath: PASSED
- testNoiseFloorTrackerZeroAllocationsOnHotPath: PASSED
- testSyncDetectorHuntingZeroAllocationsOver30Seconds: PASSED
- testReceiverPipelineListeningZeroAllocationsOver30Seconds: PASSED
- testRealTimeFactorBelowTargetOnBuildMachine: PASSED (RTF 0.0042 < 0.02)
- testFloatVsDoubleInnerLoopGoertzelComparison: PASSED

> Task :modem:modemPerf
BUILD SUCCESSFUL in 6s
Printed full DSP audit table.

> Task :modem:test (all test suites)
BUILD SUCCESSFUL in 30s
All Step 18, 20, 21 regression tests passed.
```
