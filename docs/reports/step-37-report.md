# Step 37 Report: Send Screen, Part 1 (Compose, Text, URL, and Live Estimation)

## 1. What was implemented
- **Live Compaction & Duration Estimation**: Integrated `PayloadCompactor.compact` and `AirTime.estimate` into `SendViewModel` with a ~150 ms debounce on typing so UI input responds with zero latency while wire bytes and physical transmission duration update reactively.
- **`SendScreen` Composables**:
  - Filter chips row for payload categories: `Text`, `URL`, `Wi-Fi`, `Contact` (`CdFilterChip`).
  - Text composer: Multi-line `CdOutlinedTextField` with trailing paste icon button reading system clipboard on tap.
  - URL composer: Single-line `CdOutlinedTextField` with URI keyboard type and trailing paste button.
  - Coming-next placeholders: Informative cyber cards for Wi-Fi and Contact payloads.
  - Live `ByteMeter`: 320-byte physical frame budget tracker transitioning to `warning` and `signalError` when over limit, plus dedicated "Too long for sound: shorten it" guidance banner.
  - Settings strip: `ModeSelector`, `SpeedSelector`, and greyed-out Secure Mode switch.
  - Transmission options modal bottom sheet (`CdModalBottomSheet`) with repeats picker (1x, 2x, 3x) and speaker volume guidance (70%–90%).
  - Stealth frequency disclosure dialog (`CdAlertDialog`) enforcing explicit user acknowledgement before switching to near-ultrasound carrier frequencies (17.5–21.5 kHz).
  - Extended "Chirp it" primary action button (`CdButton`) showing live duration estimate in label and disabled when invalid.
  - IME padding and scroll state preserving access to buttons when keyboard is active.
- **Intent Sharing (`ACTION_SEND`)**:
  - Added `<intent-filter>` for `android.intent.action.SEND` with `mimeType="text/plain"` in `AndroidManifest.xml`.
  - Implemented pure function `SharedTextParser.parse`: categorizes text vs URLs and cleanly truncates content exceeding 280 UTF-8 bytes along code point boundaries without corrupting multi-byte code points or surrogate pairs.
  - Added `ClipboardSanitizer` stripping ASCII control characters (0..31 and 127) while preserving essential newlines for multiline text notes.
- **Placeholder `TransmitScreen` & `TransmitRoute`**:
  - Registered `@Serializable data object TransmitRoute` in `Routes.kt`.
  - Implemented `TransmitScreen` monitoring real-time `TransmitState` (`Preparing`, `Transmitting`, `Completed`, `Cancelled`, `Failed`), frame numbers, elapsed / remaining seconds, progress bar, and cancellation confirmation dialog intercepting `BackHandler`.
- **Settings Persistence**:
  - Persisted `lastSendType` to DataStore settings, restoring user's chosen composer category across cold app launches.
- **Previews & Tests**:
  - Created dark previews in `app/src/dev/java/app/shoutsend/feature/send/SendScreenPreview.kt`.
  - Comprehensive unit test suite in `SendViewModelTest.kt` verifying shared text parsing, UTF-8 truncation integrity, clipboard sanitization, live compaction calculation, URL validation, Stealth mode warning flow, profile persistence, and transmission delegation.

### Files Created or Modified
- `app/src/main/AndroidManifest.xml` (added `ACTION_SEND` intent filter)
- `app/src/main/java/app/shoutsend/AppContainer.kt` (wired `transmitSession`, `acousticReceiver`, `audioCoordinator`)
- `app/src/main/java/app/shoutsend/MainActivity.kt` (handled `ACTION_SEND` intents in `onCreate`/`onNewIntent` and navigated to `SendRoute`)
- `app/src/main/java/app/shoutsend/core/data/settings/Settings.kt` (added `lastSendType`)
- `app/src/main/java/app/shoutsend/core/data/settings/SettingsRepository.kt` (added `setLastSendType`)
- `app/src/main/java/app/shoutsend/feature/send/ClipboardSanitizer.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/SharedTextParser.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/SendScreen.kt` (created full composer)
- `app/src/main/java/app/shoutsend/feature/send/SendViewModel.kt` (created full ViewModel)
- `app/src/main/java/app/shoutsend/feature/send/TransmitScreen.kt` (created placeholder transmit screen)
- `app/src/main/java/app/shoutsend/navigation/Routes.kt` (added `SendRoute` params and `TransmitRoute`)
- `app/src/main/java/app/shoutsend/navigation/ShoutSendNavHost.kt` (wired routes)
- `app/src/main/res/values/strings.xml` (added all Send and Transmit strings)
- `app/src/dev/java/app/shoutsend/core/modem/ModemConfigProviderFactory.kt` (created)
- `app/src/dev/java/app/shoutsend/feature/send/SendScreenPreview.kt` (created)
- `app/src/user/java/app/shoutsend/core/modem/ModemConfigProviderFactory.kt` (created)
- `app/src/test/java/app/shoutsend/feature/send/SendViewModelTest.kt` (created 15 unit tests)
- `app/src/test/java/app/shoutsend/navigation/NavigationStateTest.kt` (updated)
- `app/src/testDev/java/app/shoutsend/core/modem/OverridableModemConfigProviderTest.kt` (updated fake)
- `app/src/testUser/java/app/shoutsend/core/modem/FixedModemConfigProviderTest.kt` (updated fake)
- `docs/DECISIONS.md` (recorded DEC-056)
- `docs/PROJECT_MEMORY.md` (updated progress log)
- `docs/ROADMAP.md` (marked Step 37 Done)

---

## 2. Command Results

### `:modem:test`
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).
> Task :modem:compileTestKotlin UP-TO-DATE
> Task :modem:test
BUILD SUCCESSFUL in 6s
```

### `assembleDevDebug` & `assembleUserDebug`
```text
> Task :app:assembleUserDebug UP-TO-DATE
> Task :app:mergeProjectDexDevDebug
> Task :app:stripDevDebugDebugSymbols
> Task :app:mergeExtDexDevDebug
> Task :app:packageDevDebug
> Task :app:createDevDebugApkListingFileRedirect UP-TO-DATE
> Task :app:assembleDevDebug
BUILD SUCCESSFUL in 21s
```

### `lintDevDebug` & `lintUserDebug`
```text
> Task :app:lintReportDevDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-devDebug.html
> Task :app:lintDevDebug
> Task :app:lintReportUserDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-userDebug.html
> Task :app:lintUserDebug
BUILD SUCCESSFUL in 2m 30s
```

### `testDevDebugUnitTest` & `testUserDebugUnitTest`
```text
> Task :app:testDevDebugUnitTest
> Task :app:testUserDebugUnitTest
BUILD SUCCESSFUL in 30s
```

### `checkNoHardcodedStyle`
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.
BUILD SUCCESSFUL in 13s
```

### `verifyUserApkClean`
```text
> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.
BUILD SUCCESSFUL in 27s
```

### `verifyPermissions` & Merged Manifest Verification
```text
> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.
```
- Permissions in Dev APK: `RECORD_AUDIO`, `VIBRATE` (plus internal package dynamic receiver)
- Permissions in User APK: `RECORD_AUDIO`, `VIBRATE` (plus internal package dynamic receiver)
- `android.permission.INTERNET`: ABSENT in both flavors (100% offline).

---

## 3. Checks NOT run and why
- Field acoustic broadcast on physical devices (agents cannot hear; marked as OWNER RUNS).

---

## 4. Deviations from the prompt or rules
None. All design requirements, tokenization constraints, permissions rules, and architecture guidelines strictly maintained.

---

## 5. OWNER RUNS
- Open ShoutSend on a physical Android device.
- In Send tab, type text into the message composer: observe the `ByteMeter` updating live (~150 ms debounce).
- Switch to URL tab: type valid and malformed URLs; observe error messages and button states.
- Open external browser (e.g. Chrome) and share a link to ShoutSend via system share sheet: verify URL tab opens prefilled.
- Tap "Chirp it" to hear transmission soundwave playing through the device speaker.

---

## 6. Known issues and warnings
- None. All 98 unit tests in `:app` and all unit tests in `:modem` pass cleanly.

---

## 7. Open questions for the owner
1. For Step 38 Wi-Fi form: should the password field default to hidden with visibility toggle? (Recommended: Yes).
2. For Contact picker: should manual entry allow multiple phone numbers or up to 2? (Recommended: Up to 2, matching Protocol v1).

---

## 8. Suggested next step
- Advance to **Step 38: Send screen, part 2 (Wi-Fi and contact forms)**.
