# Step 05 Report: Style Guard and User APK Purity Check

**Date:** 2026-10-22
**Status:** Complete & Verified
**Author:** Antigravity

---

## 1. Executive Summary
Step 5 formalizes the architectural constraints of ShoutSend into automated, deterministic Gradle verification tasks implemented in pure Kotlin inside `buildSrc`. 

Per Rules 6, 7, 8, 13 to 15, 24, and 26, the build now automatically fails if:
1. Any hex color, named color constant, numeric dp/sp literal, or shape constructor appears outside `core/designsystem/DefaultTokens.kt`.
2. Any developer tooling classes, markers, or activities leak into the User APK DEX or AndroidManifest.
3. Any unapproved Android permissions appear in merged manifests (enforcing strict offline operation with zero internet access).

All guards are verified with unit tests in `buildSrc`, and `verifyChirpRules` is bound into Gradle's standard `check` lifecycle.

---

## 2. Implemented Architecture

### 2.1 BuildSrc Checkers (`buildSrc/src/main/kotlin/app/shoutsend/buildtools/`)
- `StyleGuard.kt`: Recursively analyzes Kotlin source files and XML resources. Flags:
  - Hex colors (`#RRGGBB`, `#AARRGGBB`, `Color(0x...)`) outside `DefaultTokens.kt`.
  - Named color constants (`Color.Red`, `Color.Black`, `Color.White`, etc.).
  - Shape constructors (`RoundedCornerShape(`, `CornerSize(`, `CutCornerShape(`) with numeric literals outside `core/designsystem`.
  - Numeric `dp` and `sp` literals outside `core/designsystem`.
  - Inline hex styling in XML (`android:background`, `android:textColor`).
  - Supports structured ignore comments and external allowlists (`config/style-allowlist.txt`).
- `ApkPurityGuard.kt`: Inspects unzipped APK DEX files and merged `AndroidManifest.xml` against markers in `config/dev-markers.txt`. Ensures User APK contains 0 developer classes (`DevPanel`, `TokenEditor`, `DspLab`, etc.).
- `PermissionsGuard.kt`: Parses merged `AndroidManifest.xml` files. Enforces that only approved permissions (`RECORD_AUDIO`, `VIBRATE`, and AndroidX dynamic receiver permission) exist. Enforces hard failure on `INTERNET` and `ACCESS_NETWORK_STATE`.

### 2.2 Gradle Tasks (`buildSrc/src/main/kotlin/app/shoutsend/buildtools/tasks/`)
- `CheckNoHardcodedStyleTask`: Scans `app/src` against `config/style-allowlist.txt`.
- `VerifyUserApkCleanTask`: Validates `app-user-debug.apk` purity.
- `VerifyPermissionsTask`: Validates merged manifests for dev and user variants.
- `verifyChirpRules`: Umbrella verification task depending on all three guards.
- Bound to `check` via `tasks.named("check") { dependsOn(verifyChirpRules) }`.

---

## 3. Verification Evidence (Real Outputs)

### 3.1 BuildSrc Unit Tests
Command: `./gradlew :buildSrc:test --rerun-tasks`
```text
> Task :buildSrc:checkKotlinGradlePluginConfigurationErrors
> Task :buildSrc:pluginDescriptors
> Task :buildSrc:compileKotlin
> Task :buildSrc:classes UP-TO-DATE
> Task :buildSrc:jar
> Task :buildSrc:pluginUnderTestMetadata
> Task :buildSrc:compileTestKotlin
> Task :buildSrc:testClasses UP-TO-DATE
> Task :buildSrc:test

BUILD SUCCESSFUL in 9s
7 actionable tasks: 7 executed
```
Passed tests:
- `StyleGuardTest`: catches hex literals, catches dp literals, catches named colors, respects allowlist, allows tokens.
- `ApkPurityGuardTest`: passes clean APK, detects forbidden markers in DEX.
- `PermissionsGuardTest`: allows clean manifests, hard-fails on INTERNET permission.

### 3.2 Individual Architectural Guard Verification
Commands:
`./gradlew checkNoHardcodedStyle`
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.
BUILD SUCCESSFUL in 2s
```

`./gradlew verifyUserApkClean`
```text
> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.
BUILD SUCCESSFUL in 1s
```

`./gradlew verifyPermissions`
```text
> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.
BUILD SUCCESSFUL in 1s
```

### 3.3 Proof of Failure on Developer APK
To satisfy Step 5 requirement 3 ("A check that has never been seen to fail does not count"), `ApkPurityGuard` was tested directly against `app-dev-debug.apk`:
```text
Exception in thread "main" java.lang.IllegalStateException: APK purity check failed! Found 6 forbidden developer markers in app-dev-debug.apk:
  - DevPanelViewModel
  - DeveloperPanel
  - FloatingDevHandle
  - app/shoutsend/core/devtools/
  - DevDataStoreTokenOverridesRepository
  - TokenOverridesSerializer
```
Result: **Expected failure proven**. The guard reliably catches dev classes when present.

### 3.4 Full Verification Suite (`./gradlew check`)
Command: `./gradlew check`
```text
> Task :app:checkNoHardcodedStyle
checkNoHardcodedStyle: All sources comply with tokenization rules.

> Task :app:verifyPermissions
verifyPermissions: Merged manifests comply with strict offline permissions policy.

> Task :app:verifyUserApkClean
verifyUserApkClean: User APK app-user-debug.apk is 100% clean of dev tools.

> Task :app:verifyChirpRules
> Task :app:lintReportDevDebug
> Task :app:lint
> Task :app:check

BUILD SUCCESSFUL in 35s
126 actionable tasks: 33 executed, 93 up-to-date
```

---

## 4. Exceptions and Allowlist Policy
Exceptions to style rules are strictly regulated and tracked in `config/style-allowlist.txt`.
Current allowed entries:
- `app/src/main/res/values/ic_launcher_background.xml`: Android adaptive launcher vector asset (exempted per Rule 6 clarification).
Any future additions require documentation in `docs/DECISIONS.md`.
