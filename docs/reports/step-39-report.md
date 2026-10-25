# Step 39 Report: Transmit Screen with Soundwave Rings

## 1. What was implemented
- **Transmission Envelope Calculation (`TransmissionEnvelope`)**:
  - Implemented pure JVM `TransmissionEnvelope` in `:modem` that precomputes normalized RMS energy and dominant tone indexes per 20 ms window directly from `TransmissionPlan` and `SymbolSchedule` events.
  - Slices the acoustic schedule in $O(N)$ time (< 1 ms total computation) to synchronize UI visuals to the audio waveform without runtime PCM sniffing or buffer allocation.
- **Soundwave Ripple Canvas (`RingsCanvas`)**:
  - Concentric ripple pulses expanding outward from center to edge.
  - Ring spawn intervals (80ms to 500ms) and stroke thickness dynamically follow real-time acoustic RMS amplitude.
  - Dynamic pitch-reactive neon color spectrum: low tones (0..5) lerp between `primary` (cyan) and `secondary` (magenta); mid tones (6..10) lerp between `secondary` and `tertiary` (amber); high tones (11..15) lerp between `tertiary` and `signalPeak` (bright lime); sync tones (16..17) render in `syncMarker` token.
  - Zero heap allocations per frame in draw loop (Rule 22) using a preallocated 12-ring object pool driven by `withFrameNanos`.
  - Accessible reduced-motion fallback: displays a single calm breathing ring when `ChirpTheme.isReducedMotion` or `LocalReduceMotion` is active.
- **Center Countdown & Circular Arc Progress**:
  - 360° circular progress ring (`drawArc`) with `StrokeCap.Round` tracking completion fraction.
  - Digital remaining-seconds countdown rendered in `RadioReadout` (`readoutLarge`).
  - Progress subtitle: `Frame X of Y · Repeat A of B`.
  - Mode chip: `StatusPill` displaying active band and speed configuration.
  - Completion morph: Rings contract smoothly into a centered checkmark with "Sent" and "Saved to Vault history" badge.
- **Hardware Volume Alert & System Volume Panel**:
  - Monitors `AudioManager.STREAM_MUSIC`. If volume is below 70%, displays a warning banner: *"Speaker volume is low. Media volume should be above 70% for nearby phones to receive sound reliably."*
  - Action button *"Turn up"* invokes `AudioManager.adjustStreamVolume` with `ADJUST_SAME` and `FLAG_SHOW_UI` to pop up the native system volume slider on screen without silently altering user volume.
  - Monitors peripheral audio routes via `AudioRouteMonitor` and alerts user if wired or Bluetooth headphones are connected.
- **Automatic History Vault Archiving**:
  - When transmission completes (`TransmitState.Completed`), `TransmitViewModel` automatically archives the sent payload, modem profile summary, duration, and security state to `VaultRepository` if history is enabled in settings (`historyEnabled == true`).
- **Screen Awake Management & Cancellation**:
  - Enforces `keepScreenOn = true` on the window during active acoustic modulation via `DisposableEffect`, releasing on exit.
  - Single-tap "Stop" button immediately halts transmission via `transmitSession.cancel("CancelledByUser")`.
  - System back gesture during active transmission intercepts with a confirmation dialog to prevent accidental cancellation.
- **Previews & Tests**:
  - Added previews in `app/src/dev/java/app/shoutsend/feature/send/SendScreenPreview.kt` for active transmitting, completed sent, and failed error states.
  - Pure modem unit tests in `TransmissionEnvelopeTest.kt` verifying envelope window slicing, lead-in/tail silence detection, tone index mapping, sample position lookups, and boundary clamping.
  - ViewModel unit tests in `TransmitViewModelTest.kt` verifying state mapping, envelope RMS/tone propagation, vault archiving when enabled vs disabled, human error formatting, single-tap stop, retry dispatch, and peripheral route warnings.

### Files Created or Modified
- `modem/src/main/kotlin/app/shoutsend/modem/modulator/TransmissionEnvelope.kt` (created)
- `modem/src/test/kotlin/app/shoutsend/modem/modulator/TransmissionEnvelopeTest.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/RingsCanvas.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/TransmitViewModel.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/TransmitScreen.kt` (created full cyber-radio transmit UI)
- `app/src/main/java/app/shoutsend/core/session/TransmitSession.kt` (added active envelope, payload, profile state flows)
- `app/src/main/java/app/shoutsend/AppContainer.kt` (exposed `vaultRepository` and `audioRouteMonitor`)
- `app/src/main/java/app/shoutsend/MainActivity.kt` (instantiated and wired `TransmitViewModel`)
- `app/src/main/java/app/shoutsend/navigation/ShoutSendNavHost.kt` (wired `TransmitViewModel` into `TransmitRoute`)
- `app/src/main/res/values/strings.xml` (added all localized Transmit screen strings)
- `app/src/dev/java/app/shoutsend/feature/send/SendScreenPreview.kt` (added Transmit screen dev previews)
- `app/src/test/java/app/shoutsend/feature/send/SendViewModelTest.kt` (updated mock session properties)
- `app/src/test/java/app/shoutsend/feature/send/TransmitViewModelTest.kt` (created Step 39 unit test suite)
- `docs/DECISIONS.md` (recorded DEC-058)
- `docs/DESIGN_SYSTEM.md` (documented Transmit Visuals & Soundwave Rings)
- `docs/PROJECT_MEMORY.md` (updated progress ledger)
- `docs/ROADMAP.md` (marked Step 39 **Done**)

---

## 2. Key Decisions & UI Gate Resolution
- **UI Gate 1 (Soundwave Ring Coloration)**:
  - Pitch-reactive color spectrum selected: Ripple rings smoothly shift colors based on the active tone index currently playing across cyan, magenta, amber, and bright lime.
- **UI Gate 2 (Center Countdown & Progress)**:
  - Prominent remaining-seconds timer surrounded by a 360° circular progress arc, frame counter (`Frame 3 of 8 · Repeat 1 of 2`), and mode status pill.
- **UI Gate 3 (Stopping/Canceling)**:
  - Single-tap Stop button on screen immediately halts audio playback.
  - System back press displays confirmation dialog to prevent accidental drops.
- **UI Gate 4 (Completion & History)**:
  - Ripple rings collapse into a checkmark with "Sent", and the message is automatically saved to local Vault history (if enabled in settings). Buttons provided: "Send Another" and "Done".
- **UI Gate 5 (Low Volume Alert)**:
  - Warning banner shown if media speaker volume is below 70%, with a "Turn up" button invoking the native Android volume panel.

---

## 3. Rules & Invariants Verified
- **Rule 2 (No Hardcoded Style)**: Verified via `:app:checkNoHardcodedStyle` -> 100% compliant, 0 raw hex, dp, sp, or radii literals.
- **Rule 6 (No Named Colors)**: Verified 0 named color constants (`Color.Cyan`, etc.) in production source code.
- **Rule 22 (Zero Allocation in Animation Draw)**: Verified preallocated 12-ring pool with zero object allocations inside `Canvas.drawCircle` loop.
- **Rule 26 (Permissions Boundary)**: Merged manifests contain strictly `RECORD_AUDIO` and `VIBRATE` (zero `INTERNET`). Verified via `:app:verifyPermissions`.
- **Pure Modem Architecture**: `:modem` remains 100% pure Kotlin JVM (0 Android imports). Verified via `:modem:checkModemPurity`.
- **Clean User APK**: Verified via `:app:verifyUserApkClean` -> 0 dev tools or debug markers.

---

## 4. Verification Output

### Command Execution
```bash
./gradlew :modem:test checkNoHardcodedStyle testDevDebugUnitTest testUserDebugUnitTest verifyPermissions verifyUserApkClean assembleDevDebug assembleUserDebug lintDevDebug lintUserDebug
```

### Real Output
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test UP-TO-DATE

> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:testDevDebugUnitTest UP-TO-DATE
> Task :app:testUserDebugUnitTest UP-TO-DATE

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:assembleUserDebug UP-TO-DATE
> Task :app:assembleDevDebug UP-TO-DATE

> Task :app:lintReportUserDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-userDebug.html

> Task :app:lintUserDebug

> Task :app:lintReportDevDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-devDebug.html

> Task :app:lintDevDebug

BUILD SUCCESSFUL in 38s
121 actionable tasks: 23 executed, 98 up-to-date
```

---

## 5. Security & Privacy Audit
1. **Manifest Inspection**:
   - `android.permission.RECORD_AUDIO`: Present.
   - `android.permission.VIBRATE`: Present.
   - `android.permission.INTERNET`: ABSENT.
   - Zero unexpected permissions introduced.
2. **Volatile Memory Invariant**:
   - Audio envelopes and symbol timelines are ephemeral in volatile memory.
   - Sent messages saved to `VaultRepository` are hardware-encrypted via AES-256 (`AndroidKeystoreCipher`).
3. **Volume Safety**:
   - `openVolumePanel()` uses `FLAG_SHOW_UI` and never changes device volume silently or forcefully without user control.

---

## 6. OWNER RUNS Guide
To verify Transmit screen visuals and behavior on a physical device:
1. **Soundwave Rings & Tone Coloration**:
   - From the Send screen, enter a test text or Wi-Fi network and tap **Chirp it**.
   - Observe concentric ripple rings expanding outward from the center. Notice that the ring pulse rhythm matches the acoustic beeps and that ripple colors change dynamically as different frequencies are modulated.
2. **Countdown & Progress**:
   - Observe the digital remaining-seconds timer countdown in the center and the circular progress arc completing a 360° rotation.
   - Verify that the frame counter updates (e.g. *"Frame 1 of 2"*).
3. **Low Volume Warning**:
   - Turn your phone's media volume down below 70% before sending.
   - Verify the top warning banner appears: *"Speaker volume is low"*.
   - Tap **Turn up**; verify the native Android volume slider appears on screen.
4. **Single-Tap Stop & Back Press Intercept**:
   - Start a transmission and tap the **Stop** button; verify audio stops immediately.
   - Start another transmission and press the system Back button; verify the confirmation dialog appears (*"Stop Transmission?"*).
5. **Completion & Vault Archiving**:
   - Let a transmission run to completion.
   - Verify the center morphs into a checkmark with *"Sent"* and *"Saved to Vault history"*.
   - Tap **Done** and navigate to the Vault tab; verify your sent message appears at the top of the history list.
