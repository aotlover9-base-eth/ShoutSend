# Step R0 Report: Baseline Audit and v2 Docs

**Date:** 2026-10-28  
**Step:** R0 (Baseline audit and v2 docs)  
**Status:** Complete & Verified (Zero production code changes)  
**Author:** Antigravity  

---

## 1. What was implemented

Files created or updated in this step:
- **`docs/V2_BRIEF.md`**: Created unchanged from Section 3 of `SHOUTSEND_V2_REDESIGN.md`. Documents the project brief, amended rules (Rule 3, Rule 6, Rule 24), new rules (Rules 29–32), standard verification requirements, and anti-slop guidelines.
- **`docs/V2_PLAN.md`**: Full copy of `/home/aotlover9/Downloads/SHOUTSEND_V2_REDESIGN.md` saved into the repository.
- **`docs/V2_AUDIT.md`**: Comprehensive baseline audit detailing:
  - Real SDK levels (`compileSdk 35`, `targetSdk 35`, `minSdk 31`) and dependency versions (AGP 8.5.2, Kotlin 2.0.0, Compose BOM 2024.06.00 / M3 1.2.1).
  - Explicit documentation discrepancies in `docs/RUN_SUMMARY.md` (e.g. Min API 26 vs 31, Cauchy matrix vs Berlekamp-Massey GF(256) with n=60).
  - Complete screen & route anti-slop inventory detailing exact file:line locations of emoji glyphs, ALL CAPS, monospace, neon, and raw technical numbers.
  - ViewModel instantiation patterns (`remember` in `MainActivity.setContent` lacking rotation safety and leaking context).
  - Unwired dead code (`ReceiveSession`, `DefaultReceiveSession`, `ReceiveState`, `SessionLifecycleManager`, unused `Cd*` wrappers).
  - Cyber Radio design system artifacts (`NeonCard`, `ScanlineOverlay`, `RadioReadout`, `BrandMark`, `syncMarker`, `glowPrimary`, `toneGuide`).
- **`docs/ROADMAP.md`**: Added `v2: Material You Redesign + Real File Sharing` roadmap matrix with steps R0 through R11, marking R0 as **Done**.
- **`docs/DECISIONS.md`**: Appended `DEC-069` documenting owner-approved v2 architecture, rule amendments (Rules 3, 6, 24), and new rules (Rules 29–32).
- **`docs/PROJECT_MEMORY.md`**: Appended Step R0 completion summary and v2 baseline record.

*Note: Zero production code changes were made in this step.*

---

## 2. Command Results (Standard Verification Baseline)

### 1. `:modem:test`
Command:
```bash
./gradlew :modem:test
```
Real final lines:
```text
> Task :modem:checkModemPurity
checkModemPurity: :modem is 100% pure Kotlin JVM (0 Android imports).

> Task :modem:test UP-TO-DATE

BUILD SUCCESSFUL in 43s
8 actionable tasks: 2 executed, 6 up-to-date
```

### 2. `assembleDevDebug assembleUserDebug`
Command:
```bash
./gradlew assembleDevDebug assembleUserDebug
```
Real final lines:
```text
> Task :app:packageUserDebug UP-TO-DATE
> Task :app:createUserDebugApkListingFileRedirect UP-TO-DATE
> Task :app:assembleUserDebug UP-TO-DATE
> Task :app:mergeExtDexDevDebug
> Task :app:packageDevDebug
> Task :app:createDevDebugApkListingFileRedirect
> Task :app:assembleDevDebug

BUILD SUCCESSFUL in 1m 48s
82 actionable tasks: 20 executed, 62 up-to-date
```

### 3. `lintDevDebug lintUserDebug`
Command:
```bash
./gradlew lintDevDebug lintUserDebug
```
Real final lines:
```text
> Task :app:lintReportDevDebug UP-TO-DATE
> Task :app:lintDevDebug
> Task :app:lintAnalyzeUserDebug
> Task :app:lintAnalyzeUserDebugAndroidTest
> Task :app:lintAnalyzeUserDebugUnitTest
> Task :app:lintReportUserDebug
Wrote HTML report to file:///mnt/HDD1/Downloads/ShoutSend/app/build/reports/lint-results-userDebug.html
> Task :app:lintUserDebug

BUILD SUCCESSFUL in 3m 50s
61 actionable tasks: 11 executed, 50 up-to-date
```

### 4. `testDevDebugUnitTest testUserDebugUnitTest`
Command:
```bash
./gradlew testDevDebugUnitTest testUserDebugUnitTest
```
Real final lines:
```text
> Task :app:compileDevDebugUnitTestKotlin UP-TO-DATE
> Task :app:testDevDebugUnitTest UP-TO-DATE
> Task :app:compileUserDebugUnitTestKotlin UP-TO-DATE
> Task :app:testUserDebugUnitTest UP-TO-DATE

BUILD SUCCESSFUL in 9s
56 actionable tasks: 1 executed, 55 up-to-date
```

### 5. `checkNoHardcodedStyle` and `verifyUserApkClean`
Command:
```bash
./gradlew checkNoHardcodedStyle verifyUserApkClean
```
Real final lines:
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

BUILD SUCCESSFUL in 6s
46 actionable tasks: 3 executed, 43 up-to-date
```

### 6. Merged Manifest Permission Check (Both Flavors)
Command:
```bash
aapt2 dump permissions app/build/outputs/apk/dev/debug/app-dev-debug.apk
aapt2 dump permissions app/build/outputs/apk/user/debug/app-user-debug.apk
```
Real output:
```text
package: app.shoutsend.dev
uses-permission: name='android.permission.RECORD_AUDIO'
uses-permission: name='android.permission.VIBRATE'
permission: app.shoutsend.dev.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='app.shoutsend.dev.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
---
package: app.shoutsend
uses-permission: name='android.permission.RECORD_AUDIO'
uses-permission: name='android.permission.VIBRATE'
permission: app.shoutsend.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='app.shoutsend.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```
-> Confirmed: Only `RECORD_AUDIO` and `VIBRATE` are present in merged manifests. Zero `INTERNET` permission. Matches v1 Rule 24.

---

## 3. Screenshots
- **Status:** Screenshots not verified.
- **Reason:** No Android emulator or physical device is connected (`adb devices` reported 0 attached devices).

---

## 4. Checks NOT Run
- `checkNoNetworkClients`: Not run because this Gradle guard task is scheduled to be created in Step R1.
- Rotation survival test on emulator: Not verified because no emulator or device was attached.

---

## 5. Deviations from the prompt or rules
- None. Step R0 executed strictly as specified with zero production code changes.

---

## 6. OWNER RUNS
- None for this baseline documentation step.

---

## 7. Known issues and warnings
- All baseline Gradle checks pass cleanly with 0 errors.
- As documented in `docs/V2_AUDIT.md`, `config/style-allowlist.txt` will need to allowlist existing v1 violations (emoji, monospace, uppercase) when `checkNoHardcodedStyle` is extended in Step R1, to be systematically removed in steps R3 through R6.

---

## 8. Open questions for the owner
- Baseline audit complete and verified. Ready to proceed with Step R1 upon review.
