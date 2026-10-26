# Step 40 Report: Receive Screen and Spectrogram Waterfall

**Date:** 2026-10-26  
**Status:** Complete & Verified  
**Author:** Antigravity (Leader)  

---

## 1. Executive Summary & What Was Implemented
Step 40 delivers the acoustic receiver screen featuring a live 256-bin spectrogram waterfall, carrier tone guide overlays, preamble lock highlighting, and hardware audio diagnostics.

Key deliverables:
- **`WaterfallBuffer` (`app.shoutsend.feature.receive.WaterfallBuffer`)**:
  - High-performance, zero-allocation ring buffer and backing `Bitmap` maintaining a 120-row temporal history (4.0 seconds at 30 fps) across 256 frequency bins.
  - Zero heap allocations on the streaming hot path (`pushRow` uses `System.arraycopy`).
  - Thread-safe updates mapped through a tokenized 256-stop photometric colormap.
- **`WaterfallCanvas` (`app.shoutsend.feature.receive.WaterfallCanvas`)**:
  - Zero-allocation Jetpack Compose `Canvas` drawing the rolling spectrogram directly into native canvas via preallocated `Rect` and `Paint` primitives (Rule 22).
  - 16 vertical tone marker lines corresponding to the MFSK16 data tones across the active acoustic band.
  - Carrier boundary lines marking the physical transmission channel (1375–3500 Hz for Retro; 17450–20000 Hz for Stealth).
  - Glowing channel highlight with pulsating alpha indicator activated whenever preamble lock is achieved (`isPreambleLocked == true`).
  - Subtle CRT scanline texture overlay (`ScanlineOverlay`) adding retro-futuristic styling without shaders.
  - Frequency scale footer rendering exact frequency markers across the active band.
  - Accessible, power-efficient 16-band energy bar chart fallback when `ChirpTheme.isReducedMotion` is active (UI Gate Answer 4).
- **`ReceiveViewModel` (`app.shoutsend.feature.receive.ReceiveViewModel`)**:
  - Injected with `AcousticReceiver`, `AudioCoordinator`, and `SettingsRepository`.
  - Manages half-duplex receive locking with `AudioCoordinator`, preventing collisions with active transmissions.
  - Collects `AcousticReceiver.spectrumRows` in a decoupled coroutine, updating `waterfallBuffer` and advancing the frame clock without causing parent layout recompositions.
  - Collects hardware diagnostics (`ambientLevelDbfs`, `peakLevelDbfs`, `isClipping`, `isSilent`, `sourceName`, `sampleRateHz`).
  - Collects demodulator events (`Listening`, `PreambleDetected`, `Locked`, `ModeDecoded`, `FrameDecoded`, `MessageComplete`, `Lost`, `Cooldown`).
  - Supports dynamic switching between Retro (audible) and Stealth (ultrasonic) carrier bands.
- **`ReceiveScreen` (`app.shoutsend.feature.receive.ReceiveScreen`)**:
  - Prominent hero layout positioning the live spectrogram waterfall in the top card, with `SignalMeter` and `LevelMeter` immediately below (UI Gate Answer 1).
  - Band selection filter chips (`CdFilterChip`) for Retro and Stealth.
  - Microphone runtime permission gate card (`MicPermissionCard`).
  - Hardware info and warning banners for acoustic clipping or digital silence.
  - Lifecycle integration keeping screen powered (`view.keepScreenOn = true`) during active reception.
- **Dev Previews & Unit Tests**:
  - Created Compose previews in `ReceiveScreenPreview.kt`.
  - Created 11 comprehensive unit tests in `ReceiveViewModelTest.kt` verifying buffer shifts, colormap mapping, lifecycle start/stop, lock rejection, SNR progression, fragmentation counters, payload delivery, and band switching.

---

## 2. UI Gate Alignment & Decisions
The implementation strictly follows the user-selected preferences for Step 40:
1. **Layout**: Prominent hero card in the upper half of the screen with glowing frequency markers, with signal strength and volume meters right below it.
2. **Frequency View**: Active band focus zooming into the active sound channels (Audible 1.0–4.0 kHz or Ultrasonic 16.0–22.0 kHz) so incoming data tones stand out prominently.
3. **Detection Feedback**: Glowing channel highlight pulsing over the carrier band as soon as a preamble pattern is detected.
4. **Reduced Motion**: Replaces scrolling waterfall animation with a clean 16-band frequency bar chart when reduced motion or battery saving is preferred.

---

## 3. Files Created or Modified
- `app/src/main/java/app/shoutsend/feature/receive/WaterfallBuffer.kt` (created)
- `app/src/main/java/app/shoutsend/feature/receive/WaterfallCanvas.kt` (created)
- `app/src/main/java/app/shoutsend/feature/receive/ReceiveViewModel.kt` (updated)
- `app/src/main/java/app/shoutsend/feature/receive/ReceiveScreen.kt` (updated)
- `app/src/main/java/app/shoutsend/MainActivity.kt` (updated constructor injection)
- `app/src/main/res/values/strings.xml` (added receive screen strings)
- `app/src/dev/java/app/shoutsend/feature/receive/ReceiveScreenPreview.kt` (created)
- `app/src/test/java/app/shoutsend/feature/receive/ReceiveViewModelTest.kt` (created)
- `app/src/test/java/app/shoutsend/core/session/SessionIntegrationTest.kt` (strengthened async state assertions)

---

## 4. Verification Suite Results
Command executed:
```bash
./gradlew check assembleDevDebug assembleUserDebug lintDevDebug lintUserDebug
```
- **Outcome:** `BUILD SUCCESSFUL in 3m 25s`
- **Actionable Tasks:** 172 actionable tasks (75 executed, 97 up-to-date)
- **Modem Purity:** `checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports)` -> PASS
- **User APK Purity:** `verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools` -> PASS
- **Token Compliance:** `checkNoHardcodedStyle: All sources comply with tokenization rules` -> PASS
- **Permissions Audit:** `verifyPermissions: Merged manifests comply with strict offline permissions policy` -> PASS
- **Rule Verification:** `verifyChirpRules` -> PASS
- **Unit Tests:** 144 unit tests executed across `:modem` and `:app` -> 100% PASS
- **Lint:** `lintDevDebug` and `lintUserDebug` -> 0 errors

---

## 5. Security & Privacy Audit
1. **Zero Network / Internet Permitted:** Verified `android.permission.INTERNET` remains completely absent from both Dev and User merged manifests.
2. **Audio Input Privacy:** Audio captured by `AudioRecordSource` is piped directly to internal DSP ring buffers in RAM. Zero audio frames are ever written to disk, flash storage, cache, or external logs.
3. **Volatile Invariant:** Ingested audio payloads and spectrogram data reside only in ephemeral memory and are immediately cleared when leaving the Receive screen.

---

## 6. Checks NOT Run
- Over-the-air multi-phone transfer between two physical Android devices with physical room acoustics and ambient television noise.
- Continuous 60-minute thermal/battery drain soak test on low-end hardware during nonstop waterfall streaming.

---

## 7. Deviations from Rules
**None.** All design system rules (Rule 6, Rule 7), purity rules (Rule 21), and zero-allocation constraints (Rule 22) are strictly satisfied.

---

## 8. OWNER RUNS Guide
To manually test and verify Step 40 on a physical Android device:
1. **Launch App & Navigate**:
   - Install `app-dev-debug.apk` on Device B (Receiver).
   - Ensure microphone permission is granted.
   - Tap the **Receive** navigation tab.
2. **Verify Spectrogram Waterfall**:
   - Verify the top hero card displays the live scrolling waterfall.
   - Speak or whistle near the microphone: observe immediate acoustic energy streaks appearing at the top of the waterfall and scrolling smoothly downward.
   - Check the frequency scale footer: verify ticks show "1.0 kHz", "1.5 kHz", "2.5 kHz", "3.5 kHz", "4.0 kHz" for Retro band.
3. **Verify Band Switching**:
   - Tap the **Ultrasonic (Stealth)** chip.
   - Verify the waterfall clears and the frequency scale switches to "16.0 kHz", "17.5 kHz", "19.0 kHz", "20.5 kHz", "22.0 kHz".
   - Tap back to **Audible (Retro)**.
4. **Verify Carrier Lock Highlight**:
   - On Device A (Transmitter), start transmitting a short text message on Retro band.
   - On Device B, watch the waterfall: as soon as the sender's chirp begins, verify that:
     1. The channel highlight illuminates the 1.5–3.5 kHz column with a glowing border.
     2. The "CARRIER LOCKED: MFSK16" pill appears inside the waterfall.
     3. The status pill transitions from `LISTENING` -> `LOCKED` -> `RECEIVING`.
     4. The `SignalMeter` shows active carrier SNR ($> 15$ dB).
5. **Verify Reduced Motion Fallback**:
   - On the device, open Android system Settings -> Accessibility -> Vision -> Turn on "Remove animations" / "Reduced motion".
   - Return to ShoutSend Receive screen: verify the scrolling waterfall automatically switches to a clean 16-bar frequency spectrum meter with zero scrolling motion.
