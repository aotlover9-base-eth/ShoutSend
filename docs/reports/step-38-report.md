# Step 38 Report: Send Screen, Part 2 (Wi-Fi and Contact Forms)

## 1. What was implemented
- **Wi-Fi Credential Composer (`WifiComposer`)**:
  - Full Wi-Fi network configuration form supporting SSID (1..32 UTF-8 bytes), security dropdown/chips (`WPA2` default, `WPA3`, `OPEN`), masked passphrase input, and hidden network switch.
  - Masked password input field with show/hide eye toggle button and clipboard paste button sanitizing pasted text.
  - Password input field disappears completely when security mode is set to `OPEN`, preventing spurious credentials on open networks.
  - Strictly disabled autofill and keyboard autocorrect suggestions (`KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)`), and explicitly marked password field sensitive for accessibility services (`Modifier.semantics { password() }`).
  - Prominent acoustic privacy cue warning card displayed whenever security != `OPEN`: *"Anyone near you with a recorder could capture this password."* with a one-tap action *"Use Secure mode"* that toggles the secure transmission flag and presents an informative toast/snackbar until Step 46 passphrase UI.
- **Contact Card Composer (`ContactComposer`) & Safe Reader (`ContactPickerReader`)**:
  - Direct integration with system contact picker via `ActivityResultContracts.PickContact` without requesting `READ_CONTACTS` or `ACCESS_FINE_LOCATION` permissions (Rule 24).
  - Reader queries display name, up to 2 phone numbers, and 1 email strictly through the temporary URI grant under `Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)`.
  - Manual entry mode allowing users to type contact details directly without using the system contact picker.
  - Review card *"This is exactly what will be sent"* with individual `CdCheckbox` toggles for Primary Phone, Secondary Phone, and Email address, allowing granular exclusion of individual fields before transmission.
  - Clear notice displayed: *"Contact photo is never transmitted."*
  - Dedicated buttons for re-picking different contacts, switching to manual mode, or clearing contact state.
- **Sensitive Memory Sanitization (Rule 25)**:
  - Implemented `clearSensitiveData()` in `SendViewModel` to wipe Wi-Fi passwords, contact names, phone numbers, and email addresses, and reset password visibility.
  - Automatically invoked via `DisposableEffect(Unit)` when departing the Send screen and immediately upon dispatching acoustic transmission in `startTransmission()`, ensuring sensitive credentials never linger in memory.
- **Design System Component**:
  - Implemented `CdCheckbox` in `app/src/main/java/app/shoutsend/core/designsystem/components/CdCheckbox.kt` wrapping Material 3 Checkbox with tokenized colors (`primary`, `outline`, `onPrimary`, `onSurface`) and opacities.
- **Previews & Tests**:
  - Added dark mode previews for Wi-Fi composer and Contact composer in `app/src/dev/java/app/shoutsend/feature/send/SendScreenPreview.kt`.
  - Expanded unit test suite in `SendViewModelTest.kt` with 11 new tests covering Wi-Fi validation, Open Wi-Fi, password length enforcement, SSID byte limits, contact picker integration, selective field exclusion, manual contact entry, invalid email handling, `clearSensitiveData()` sanitization, transmission memory cleanup, and secure mode toggle.

### Files Created or Modified
- `app/src/main/java/app/shoutsend/core/designsystem/components/CdCheckbox.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/ContactPickerReader.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/WifiComposer.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/ContactComposer.kt` (created)
- `app/src/main/java/app/shoutsend/feature/send/SendScreen.kt` (integrated composers & `DisposableEffect` cleanup)
- `app/src/main/java/app/shoutsend/feature/send/SendViewModel.kt` (added Wi-Fi & contact state, validation, compaction, and memory sanitization)
- `app/src/main/res/values/strings.xml` (added all Wi-Fi and Contact localized strings)
- `app/src/dev/java/app/shoutsend/feature/send/SendScreenPreview.kt` (added Wi-Fi and Contact composer previews)
- `app/src/test/java/app/shoutsend/feature/send/SendViewModelTest.kt` (added Step 38 unit test suite)
- `docs/DECISIONS.md` (recorded DEC-057)
- `docs/PROJECT_MEMORY.md` (updated progress ledger)
- `docs/ROADMAP.md` (marked Step 38 **Done**)

---

## 2. Key Decisions & UI Gate Resolution
- **UI Gate 1 (Wi-Fi Form)**:
  - Form contains SSID, security dropdown/chips (WPA2, WPA3, OPEN), password field with show/hide toggle and Paste button, and Hidden Network switch.
  - Password field is masked by default and disappears completely on Open security. Autofill and suggestions disabled.
- **UI Gate 2 (Contact Picker & Manual Entry)**:
  - Supported `ActivityResultContracts.PickContact` (zero `READ_CONTACTS` permission) querying `Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)`.
  - Manual entry mode provided for name, up to 2 phone numbers, and 1 email.
  - Contact photo is never queried or transmitted.
- **UI Gate 3 (Contact Review Card)**:
  - Both picked and manually entered contacts display *"This is exactly what will be sent"* review card with individual checkboxes to selectively exclude phone numbers or email.
- **UI Gate 4 (Wi-Fi Privacy Cue)**:
  - Displayed whenever security != `OPEN`: *"Anyone near you with a recorder could capture this password."* with action *"Use Secure mode"*.

---

## 3. Rules & Invariants Verified
- **Rule 2 (No Hardcoded Style)**: Verified via `:app:checkNoHardcodedStyle` -> 100% compliant, 0 raw hex/dp/sp/radii literals.
- **Rule 24 (Privacy & Contact Permission)**: Zero `READ_CONTACTS` permission declared in manifest. Query strictly uses picker URI temporary grant.
- **Rule 25 (Sensitive Data Scrubbing)**: Passwords and contact details wiped via `clearSensitiveData()` on screen exit and on transmission start. Never logged.
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

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:testDevDebugUnitTest
> Task :app:testUserDebugUnitTest
> Task :app:assembleDevDebug
> Task :app:assembleUserDebug

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:lintDevDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-devDebug.html

> Task :app:lintUserDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-userDebug.html

BUILD SUCCESSFUL in 2m 34s
121 actionable tasks: 43 executed, 78 up-to-date
```

---

## 5. Security & Privacy Audit
1. **Manifest Inspection**:
   - `android.permission.RECORD_AUDIO`: Present (strictly required for carrier reception).
   - `android.permission.VIBRATE`: Present (strictly required for tactile feedback).
   - `android.permission.INTERNET`: ABSENT (0 internet access).
   - `android.permission.READ_CONTACTS`: ABSENT (0 contact table access).
   - `android.permission.ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: ABSENT (0 location access).
2. **Volatile Memory Invariant**:
   - Sensitive credentials (Wi-Fi passwords, contact names/numbers/emails) are never saved to DataStore or Room database in plain text.
   - Screen departure or transmission initiation automatically clears all sensitive state fields.
3. **Log Sanitization**:
   - Zero SSIDs, passwords, or contact payloads logged to Logcat.

---

## 6. OWNER RUNS Guide
To verify Wi-Fi and Contact sharing on a physical device:
1. **Wi-Fi Composer**:
   - Switch category chip to **Wi-Fi**.
   - Enter your home Wi-Fi SSID and password. Note that the password is masked by default. Tap the eye icon to toggle visibility; tap the paste icon to paste from clipboard.
   - Verify that the acoustic exposure warning card is visible: *"Anyone near you with a recorder could capture this password."*
   - Tap **Use Secure mode** and verify the confirmation snackbar appears.
   - Change security to **Open**; verify the password field and warning card disappear.
2. **Contact Composer**:
   - Switch category chip to **Contact**.
   - Tap **Pick from Contacts** and select a test contact.
   - Verify the review card displays the name and up to 2 phone numbers and email.
   - Uncheck one of the phone numbers or the email and observe the **ByteMeter** update in real-time.
   - Tap **Chirp it**; verify the transmission starts and sensitive contact details are immediately wiped from memory.
