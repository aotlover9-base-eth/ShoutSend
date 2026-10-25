# Step 30 Verification Report: Send and Receive Sessions, Audio Focus, Routes

## 1. Overview
Implemented `TransmitSession` and `ReceiveSession` domain use-case coordinators bridging ViewModels to lower-level audio hardware. This step establishes:
1. `TransmitState` and `ReceiveState` sealed hierarchies mapping physical layer events to reactive state machines.
2. `AudioCoordinator` providing mutex-like half-duplex mutual exclusion between speaker playback and microphone capture, with the single exception of loopback calibration.
3. `AudioFocusController` managing transient media audio focus (`AUDIOFOCUS_GAIN_TRANSIENT`), handling external focus loss (`Failed("AudioFocusLost")`), and observing background media playback (`isMusicActive`).
4. `AudioRouteMonitor` detecting dynamic hardware route changes via `AudioDeviceCallback`, classifying transducer pathways (`SPEAKER_OK`, `HEADSET_WARNING`, `BLUETOOTH_WARNING`, `USB_WARNING`).
5. `SessionLifecycleManager` stopping active reception when backgrounded beyond the configurable grace period (default 5s) to eliminate background microphone capture.
6. Comprehensive test suite verifying transitions, focus loss, route warnings, mutual exclusion, wire size limits, and end-to-end loopback with the Step 22 channel simulator.

## 2. Implementation Summary
- `TransmitState`: Sealed hierarchy (`Idle`, `Preparing`, `Transmitting`, `Completed`, `Cancelled`, `Failed`).
- `ReceiveState`: Sealed hierarchy (`Idle`, `Starting`, `Listening`, `PreambleDetected`, `Locked`, `Receiving`, `Success`, `NeedsPassphrase`, `Failed`).
- `AudioCoordinator`:
  - Tracks `SessionMode` (`IDLE`, `TRANSMITTING`, `RECEIVING`, `CALIBRATING`).
  - Guarantees half-duplex mutual exclusion: starting transmission halts receiver; starting reception fails if transmitter is busy.
  - Exposes `keepScreenOn: StateFlow<Boolean>` active during preparing, transmitting, starting, listening, and receiving.
- `AudioFocusController`:
  - Requests transient focus with `USAGE_MEDIA` and `CONTENT_TYPE_MUSIC`.
  - Cancels active transmission with `Failed("AudioFocusLost")` upon audio focus loss.
  - Reports `isMusicActive` warning flag for the receiver.
- `AudioRouteMonitor`:
  - Exposes `routes: StateFlow<AudioRouteInfo>` and `outputClassification` / `inputClassification`.
  - Warns about headset attachment during transmission and band-limited Bluetooth microphone during reception.
- `TransmitSession`:
  - Validates and compacts domain payloads (Steps 13, 14).
  - Encrypts via `SecureBox` PBKDF2/AES-GCM if passphrase provided (Step 15).
  - Enforces 320-byte wire limit with typed `Failed("PayloadTooLarge")`.
  - Builds `Modulator`, manages audio focus, streams via `AcousticTransmitter`, and cleanly handles `cancel()`.
- `ReceiveSession`:
  - Consumes `ReceiveEvent` stream from `AcousticReceiver`, updating `ReceiveState`.
  - Decrypts secure transmissions via `passphraseProvider` or post-receipt `submitPassphrase()`.
  - Guards terminal states (`Success`, `NeedsPassphrase`, `Failed`) against trailing channel silence (`MicrophoneSilent`) and inter-message cooldowns (`Cooldown`).
  - Exposes diagnostics and 60 fps waterfall spectrum channel.
- `SessionLifecycleManager`:
  - Automatically cancels listening after configurable background grace period (default 5s).
  - Resets timer on foreground return.

## 3. Verification Results
```
> Task :app:testDevDebugUnitTest --tests "app.shoutsend.core.session.*"
BUILD SUCCESSFUL in 24s
Tests executed:
- testTransmitStateTransitionsAndCompletion: PASSED
- testTransmitPayloadTooLargeFailsImmediately: PASSED (Failed("PayloadTooLarge"))
- testTransmitFocusLossCancelsWithFailed: PASSED (Failed("AudioFocusLost"), coordinator returns IDLE)
- testAudioCoordinatorEnforcesHalfDuplexMutualExclusion: PASSED (receiver stopped on transmit lock)
- testReceiveStateTransitionsToSuccess: PASSED (Listening -> PreambleDetected -> Locked -> Receiving -> Success)
- testReceiveEncryptedMessageWithPassphraseProvider: PASSED (PBKDF2/AES-GCM decryption verified)
- testSessionLifecycleManagerStopsReceiveAfterGracePeriod: PASSED (grace period expired -> Idle)
- testEndToEndSecureSendAndReceiveLoopWithStep22ChannelSimulator: PASSED (simulated acoustic channel end-to-end loopback with encryption)

> Task :app:verifyChirpRules
BUILD SUCCESSFUL in 16s
- checkNoHardcodedStyle: All sources comply with tokenization rules.
- verifyPermissions: Merged manifests comply with strict offline permissions policy.
- checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).
- verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:check & :modem:check (full project quality gate)
BUILD SUCCESSFUL in 2m 21s (143 actionable tasks)
```

## 4. Owner Verification Scenario
To verify session behavior on physical hardware:
1. Two physical Android devices running `app-dev-debug.apk` with `RECORD_AUDIO` granted.
2. Launch Device A on Receive screen (state shows `Listening`, green microphone dot appears in status bar).
3. Connect wired headset to Device A; observe route warning banner indicating headset microphone in use.
4. On Device B, compose a text message with encryption passphrase and press send.
5. Device B transitions `Preparing` -> `Transmitting` with smooth progress bar; Device A transitions `PreambleDetected` -> `Locked` -> `Receiving` -> `NeedsPassphrase`.
6. Enter matching passphrase on Device A; verify state transitions to `Success` and displays decrypted payload.
