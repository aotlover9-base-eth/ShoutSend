# Step 42 Report: Instant Action Cards

**Date:** 2026-10-26  
**Status:** Complete & Verified  
**Author:** Antigravity (Leader)  

---

## 1. Executive Summary & What Was Implemented
Step 42 implements rich, permission-minimal Instant Action Cards embedded directly inside the received payload card:
- **`PayloadActionHelper` (`app.shoutsend.feature.receive.PayloadActionHelper`)**:
  - Encapsulates safe intent generation and clipboard management with zero invasive permissions.
  - **Wi-Fi**: Generates `Settings.ACTION_WIFI_SETTINGS` intent. Copies Wi-Fi password to clipboard with `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+ (guarded for API 31–32) and schedules an auto-clear coroutine after 60 seconds.
  - **Contact**: Generates `Intent.ACTION_INSERT` with `ContactsContract.RawContacts.CONTENT_TYPE` pre-filled with name, primary phone, secondary phone, and email without requesting contact write permissions (`WRITE_CONTACTS`).
  - **Dialer & SMS**: Sanitizes telephone numbers (`filter { it.isDigit() || it == '+' || it == '*' || it == '#' }`) and generates `Intent.ACTION_DIAL` (opens dialer, never auto-calls, 0 permissions) and `Intent.ACTION_SENDTO` with `smsto:` scheme (0 SMS permissions).
  - **URL Security**: Structured URL parser (`parseUrl`) extracting scheme, host/domain, and path. Enforces the security rule that received links are never automatically opened. Highlights the domain/host prominently in bold primary color, and displays an insecure HTTP warning pill (`AcousticStatus.ERROR`) when `scheme != "https"`.
  - **Share & Copy**: Generates system chooser `Intent.ACTION_SEND` (`text/plain`) and clipboard copy operations.
- **`ReceivedPayloadCard` (`app.shoutsend.feature.receive.ReceivedPayloadCard`)**:
  - Embedded instant action buttons positioned directly within the received message card.
  - **Wi-Fi Card**: Shows SSID, Security type, and masked password with a show/hide toggle. Includes a primary `"Connect to Wi-Fi"` button that copies the sensitive password, provides feedback notice, and opens Wi-Fi settings, plus a secondary `"Copy Password"` button.
  - **Contact Card**: Shows display name, phone numbers, and email. Each phone number row provides individual `"Call"` (`ACTION_DIAL`) and `"Text"` (`ACTION_SENDTO`) buttons. Includes a primary `"Save Contact"` button and a secondary `"Share"` button.
  - **URL Card**: Visual breakdown of URL with highlighted domain, warning badge on unencrypted HTTP links, caution notice ("Acoustic link received. Verify domain before opening."), user-confirmed `"Open Link"` button (`ACTION_VIEW`), `"Share"` button, and `"Copy"` button.
  - **Text Card**: Monospace body preview, primary `"Share"` button, and secondary `"Copy"` button.
  - Strict compliance with minimum touch targets (48dp, `ChirpTheme.sizes.touchTargetMin`) and accessible semantic content descriptions on all action buttons and icon buttons.
- **Unit Tests & Previews**:
  - 10 unit tests in `PayloadActionHelperTest.kt` verifying HTTPS/HTTP parsing, URL parameter handling, phone sanitization, and contact summary formatting.
  - Compose previews for all 4 action card payload variations in `ReceiveScreenPreview.kt`.

---

## 2. UI Gate Alignment & Decisions
The implementation strictly follows the user-selected preferences for Step 42:
1. **Wi-Fi Connection Flow**: Copies password to clipboard marked as sensitive (`EXTRA_IS_SENSITIVE`) on API 33+ with a 60-second auto-clear timer, and opens Android's native Wi-Fi settings (`ACTION_WIFI_SETTINGS`). No extra permissions requested. SSIDs and passwords are never logged.
2. **Contact Card Flow**: Pre-fills the native Contacts app insert screen (`ACTION_INSERT`) with name, phones, and email without requesting contact write permissions. Provides individual Call (`ACTION_DIAL`) and Text (`ACTION_SENDTO`) buttons next to each phone number.
3. **Web Links & Text Flow**: Strict security rule: links arriving over acoustic airwaves are never opened automatically. The full URL is displayed with domain highlighted, HTTP links display an insecure warning badge, and the browser is launched only after explicit tap on `"Open Link"`. Text messages offer instant Share and Copy actions.
4. **Layout & Accessibility**: Instant action buttons are embedded directly inside `ReceivedPayloadCard`. All buttons conform to `ChirpTheme.sizes.touchTargetMin` (48dp) and include semantic content descriptions.

---

## 3. Files Created or Modified
- `app/src/main/java/app/shoutsend/feature/receive/PayloadActionHelper.kt` (created)
- `app/src/main/java/app/shoutsend/feature/receive/ReceivedPayloadCard.kt` (updated with instant action buttons, domain highlight, password toggle)
- `app/src/main/res/values/strings.xml` (added Step 42 instant action string resources)
- `app/src/dev/java/app/shoutsend/feature/receive/ReceiveScreenPreview.kt` (added previews for Wi-Fi, Contact, and HTTP warning cards)
- `app/src/test/java/app/shoutsend/feature/receive/PayloadActionHelperTest.kt` (created with 10 unit tests)
- `app/build.gradle.kts` (configured `testOptions.unitTests.isReturnDefaultValues = true`)

---

## 4. Verification Suite Results
Command executed:
```bash
./gradlew check assembleDevDebug assembleUserDebug lintDevDebug lintUserDebug
```
- **Outcome:** `BUILD SUCCESSFUL in 2m 19s`
- **Actionable Tasks:** 172 actionable tasks (67 executed, 105 up-to-date)
- **Modem Purity:** `checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports)` -> PASS
- **User APK Purity:** `verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools` -> PASS
- **Token Compliance:** `checkNoHardcodedStyle: All sources comply with tokenization rules` -> PASS
- **Permissions Audit:** `verifyPermissions: Merged manifests comply with strict offline permissions policy` -> PASS (Only `RECORD_AUDIO` and `VIBRATE`; zero `INTERNET`, `CALL_PHONE`, `SEND_SMS`, `WRITE_CONTACTS`)
- **Rule Verification:** `verifyChirpRules` -> PASS
- **Unit Tests:** 100% PASS across `:modem` and `:app`
- **Lint:** `lintDevDebug` and `lintUserDebug` -> 0 errors

---

## 5. Security & Privacy Audit
1. **Zero External Permissions**: Action cards use standard Android system intents (`ACTION_DIAL`, `ACTION_SENDTO`, `ACTION_INSERT`, `ACTION_WIFI_SETTINGS`, `ACTION_VIEW`, `ACTION_SEND`). Zero dangerous permissions added to manifest.
2. **Clipboard Sanitization & Auto-Clear**: Copied Wi-Fi passwords are flagged as sensitive (`ClipDescription.EXTRA_IS_SENSITIVE`) on API 33+ preventing accidental exposure in keyboard clipboard suggestion strips, and auto-cleared after 60 seconds.
3. **No Auto-Execution**: Audio-received URLs and phone numbers are never opened or called automatically without explicit user tap.

---

## 6. Checks NOT Run
- Real physical device clipboard auto-clear timing verification under OS sleep mode.
- Physical device test of vendor-customized third-party Contacts apps.

---

## 7. Deviations from Rules
**None.** All design system rules (Rule 6, Rule 7), purity rules (Rule 21), and zero-allocation constraints (Rule 22) are strictly satisfied.

---

## 8. OWNER RUNS Guide
To manually verify Step 42 instant action cards:
1. **Launch App**: Open ShoutSend.
2. **Receive Wi-Fi Credential**:
   - Receive or preview a Wi-Fi payload.
   - Tap "Show" to reveal password; tap "Hide" to mask it.
   - Tap "Connect to Wi-Fi": verify notice appears ("Password copied..."), Android Wi-Fi settings open, and clipboard has password.
3. **Receive Contact Card**:
   - Receive or preview a contact payload with phone numbers and email.
   - Tap "Call" next to a phone number: verify phone dialer opens with number pre-filled (does not call automatically).
   - Tap "Text": verify SMS messaging app opens pre-filled.
   - Tap "Save Contact": verify system Contacts app opens with fields pre-filled.
4. **Receive URL**:
   - Receive an HTTP link: verify "Not secure (HTTP)" warning badge is displayed.
   - Verify link domain is bolded and highlighted.
   - Tap "Open Link": verify external browser opens the URL.
