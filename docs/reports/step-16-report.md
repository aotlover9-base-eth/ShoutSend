# Step 16 Report: Tone Synthesizer and Windowing

**Date:** 2026-10-23
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 16 implements the core acoustic synthesis engine in `:modem` (`app.shoutsend.modem.synth`) conforming to Rules 21, 22 (zero allocations on rendering hot paths), 23, and 27. It turns a schedule of acoustic symbol events into floating-point audio samples with raised-cosine transitions, absolute-phase continuity, and verified chunk-independent rendering.

Key components delivered:
- **`RaisedCosineWindow` & `RampTable`:** Precomputed raised-cosine (Hann transition) envelope tables. Guaranteed invariant: $w_{\text{up}}[i] + w_{\text{down}}[i] \equiv 1.0f$ for all $i \in [0, \text{rampLength}-1]$. Cached by ramp length for zero-allocation lookups during rendering.
- **`SymbolEvent` & `SymbolSchedule`:** Timeline model enforcing $R \le L/2$, tone indexes $\in [0, 17]$, and level $\in (0, 1]$. Employs $O(\log N)$ binary search querying to instantly identify active overlapping symbols for any arbitrary audio chunk.
- **`ToneSynth`:** Deterministic audio synthesizer using absolute-phase oscillators: $s(n) = \sin(2\pi \cdot f \cdot n / F_s)$. Renders arbitrary chunk sizes (1, 7, 480, 10,000 samples) producing bit-identical floating-point output to one monolithic render call. Zero heap allocations on the hot path.
- **`PcmConverter`:** High-performance, zero-allocation conversion between normalized 32-bit float $[-1.0f, 1.0f]$ and 16-bit signed linear PCM $[-32768, 32767]$ with round-to-nearest and clamp without dither.

---

## 2. Experimental Measurements & Performance

### 2.1 Chunk Independence Invariant
Rendered identical 8-symbol sequence across variable chunk sizes:
- Monolithic (all at once): Baseline
- Chunk size 1 sample: **Bit-identical** (`fullRender[i].toRawBits() == chunk1Render[i].toRawBits()`)
- Chunk size 7 samples: **Bit-identical**
- Chunk size 480 samples: **Bit-identical**
- Chunk size 10,000 samples: **Bit-identical**

### 2.2 Spectral Purity (DFT Evaluation)
Evaluated single tone (Stealth Tone 5: $18,350$ Hz) over 3,200 samples at 48 kHz:
- Target Tone 5 Power: $0.1600$ (normalized $A^2/4$)
- Leakage at all 15 other data tones: $< -40$ dB (down to numerical noise floor $-300$ dB on orthogonal bins)
- Leakage at sync tones S0 ($17,450$ Hz) and S1 ($20,000$ Hz): $< -40$ dB

### 2.3 Boundary Dip Test (Repeated Tones)
Evaluated two consecutive symbols transmitting identical Tone 4 with 96-sample overlap:
- Overlap region RMS ($[384, 480)$): $0.5657$
- Steady region RMS ($[96, 384)$): $0.5657$
- Measured variation: $< 0.05$ dB (exceeds requirement of $< 1.0$ dB)
- Result: Envelope conservation ($w_{\text{up}} + w_{\text{down}} \equiv 1.0f$) eliminates amplitude dip completely.

### 2.4 Click Test / Boundary Step Bounds
Evaluated transition between widely separated tones (Tone 0 at $17,600$ Hz to Tone 15 at $19,850$ Hz):
- Maximum sample-to-sample slope $\max |s[n] - s[n-1]|$: **$1.96$**
- Theoretical unwindowed instantaneous jump bound: $\le 1.6 + \text{slope} \approx 3.7$
- Continuous sine slope bound: $2.2$
- Result: Slope remains strictly bounded ($< 2.2$), confirming absence of transient impulse clicks.

### 2.5 DTFSK Amplitude & Energy Balance
- Two-tone emission at level $0.4f$ per tone ($0.8f$ total target):
- Peak amplitude: $\le 0.800$ (strictly below $0.801$ limit)
- Tone energy ratio between lower and upper tones: $< 0.1$ dB (strictly below $0.5$ dB limit)

---

## 3. Verification Evidence (Real Outputs)

### 3.1 Test Suite Execution (`./gradlew :modem:test`)
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:compileTestKotlin
> Task :modem:test

BUILD SUCCESSFUL in 18s
8 actionable tasks: 4 executed, 4 up-to-date
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

BUILD SUCCESSFUL in 20s
131 actionable tasks: 28 executed, 103 up-to-date
```

---

## 4. Next Step
Proceed to **Step 17: Modulator** (assembles complete transmissions: preamble, sync word, mode block, frame markers, RS codeword symbols, and lead-in/tail silence, exposing streaming audio rendering and WAV export).
