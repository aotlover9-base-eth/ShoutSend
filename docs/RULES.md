PROJECT: ShoutSend, a native Android app (Kotlin, Jetpack Compose, Material 3, MVVM) that sends text, URLs, Wi-Fi credentials and contacts between phones using sound (MFSK / dual-tone FSK, Goertzel decoding, Reed-Solomon error correction). No Wi-Fi, Bluetooth, internet or pairing.

THE 20 RULES
1. Native Android, Kotlin only.
2. MVVM (ViewModel + StateFlow, unidirectional data flow).
3. Jetpack Compose with Material You (Material 3) principles. (Amended in v2, see below)
4. Modern Kotlin and Compose practices. Avoid unnecessary dependencies and overengineering. Justify every dependency in docs/DECISIONS.md (purpose, license, size, minSdk).
5. UI/UX is the main priority.
6. No hardcoded hex colors. Dynamic color-token system. Exactly one file, DefaultTokens.kt, may hold the factory-default seed values. (Amended in v2, see below)
7. No hardcoded corner radii. Configurable shape tokens.
8. One centralized design system (colors, shapes, typography, spacing, motion, strokes, sizes, component styles).
9. Consistent UI across all screens: dialogs, bottom sheets, buttons, cards, input fields, all from the shared wrappers.
10. A developer panel to change colors and corner radii dynamically.
11. Panel changes reflect across the entire app instantly, without restart.
12. Customized tokens persist across restarts, with a reset-to-defaults option.
13. Two APKs, Developer and User, via Gradle product flavors (dimension audience: dev, user).
14. Easy switching between Developer and User builds.
15. Developer-only tools live only in src/dev. The User APK contains none of them.
16. Ask the owner questions before developing. If something is missing or ambiguous, stop and list your questions.
17. Discuss UI design with the owner before implementing any UI. Implement UI only if the step prompt contains owner-confirmed design decisions.
18. After every step compile and verify. Proceed only if the build succeeds.
19. Run the relevant checks and report actual results. Never claim success without verification. Write "not verified" for anything you did not run.
20. After every step update the Markdown memory and write a report for Claude.

SHOUTSEND RULES
21. The :modem module is pure Kotlin JVM. It never imports android.*. The app depends on :modem, never the reverse.
22. Audio hot paths (synthesis, Goertzel, FFT, demodulation) allocate nothing per sample or per symbol, take no locks, do no logging and do no I/O. Preallocate buffers.
23. Agents cannot hear. Never claim acoustic performance without channel-simulator numbers, and mark real-device behavior as OWNER RUNS.
24. Permissions are exactly RECORD_AUDIO and VIBRATE. INTERNET must never appear in the merged manifest of either flavor. Adding any permission needs the owner's approval, recorded in DECISIONS.md. (Replaced in v2, see below)
25. Privacy: never log message payloads, SSIDs or passwords, in any build type. Secrets at rest are encrypted with an Android Keystore key.
26. All colors, radii, spacing, strokes, sizes and motion values used by UI code come from tokens. Canvas drawing uses tokens too.
27. Tests are deterministic: seeded random generators, injectable clocks, no sleeping on real time.
28. Protocol v1 is defined in the roadmap Section 6 and docs/PROTOCOL.md. Any change to it must be recorded in DECISIONS.md with the reason and the simulator numbers behind it.

---

## V2 AMENDMENTS & NEW RULES (Owner Approved v2)

### Rule Amendments
- **Rule 3 (amended in v2)**: Material 3 Expressive (Material You). Use `MaterialExpressiveTheme` (or `MaterialTheme` + `MotionScheme.expressive()` if the wrapper is unavailable), `MaterialShapes`, shape morphing, `LoadingIndicator`, `ButtonGroup`, expressive motion springs. Use real Material 3 components. No look-alike custom components when an M3 component exists.
- **Rule 6 (amended in v2)**: Dynamic color (wallpaper) is ON by default on all supported devices. The fallback scheme is generated from ONE seed color stored in `DefaultTokens.kt`. That file stays the only place with hex values.
- **Rule 24 (replaced in v2)**: The permission set is exactly:
  - `RECORD_AUDIO`
  - `VIBRATE`
  - `INTERNET` (local sockets on the Wi-Fi Direct link only, never the internet)
  - `ACCESS_WIFI_STATE`
  - `CHANGE_WIFI_STATE`
  - `NEARBY_WIFI_DEVICES` (`android:usesPermissionFlags="neverForLocation"`)
  - `ACCESS_FINE_LOCATION` (`android:maxSdkVersion="32"`, needed for Wi-Fi Direct on Android 12 only)
  - No other permission. No storage permission (use the Photo Picker, SAF and MediaStore). No foreground service in v2.

### New Rules (Rules 29 – 32)
29. **No network clients**. No HTTP library, no `URL.openConnection`, no `HttpURLConnection`, no DNS lookups, no analytics. Sockets may only connect to or listen on the Wi-Fi Direct group (192.168.49.0/24). A Gradle guard enforces this (`checkNoNetworkClients`).
30. **Files never touch app logs**. Never log file names, sizes linked to names, URIs, passphrases, invite contents or session keys, in any build type.
31. **Every file transfer is authenticated and encrypted end to end with a one-time session key** (Step R8). A transfer that fails authentication or integrity is discarded, never partially saved as "complete".
32. **ANTI-SLOP UI RULES** (enforced by review and by `checkNoHardcodedStyle`):
   - **a.** Icons come ONLY from Material Symbols Rounded (vector drawables in `res/drawable`, prefix `ic_`, filled variant for selected states). No emoji, no Unicode glyph icons (`▲` `▼` `■` `●` `◆` `ℹ` `🔒` `📋` `✓` `✔` `✕` `✖` etc.) anywhere in UI code.
   - **b.** Sentence case everywhere. No ALL CAPS labels, no `.uppercase()` on UI text, no monospace font in user-facing UI. (The dev-only DSP Lab may use monospace.)
   - **c.** No glows, neon, scanlines, drop-shadow "bloom", or gradient backgrounds. Depth comes from M3 tonal surfaces (`surfaceContainer` roles) and elevation tokens.
   - **d.** Human copy: "Listening for nearby phones", "Sent!", "Ask them to open ShoutSend". Never jargon like "SYNC LOCK", "SNR 12.4 dB", "FRAME 2/4", "SOURCE: MIC" on main screens. Technical numbers live only in an optional "Signal details" bottom sheet.
   - **e.** One primary action per screen. No cards inside cards. No more than 2 type sizes in one card.
   - **f.** Every interactive element is at least 48 dp, has a content description or a visible label, and gives feedback (ripple, state change, haptic where it matters).
   - **g.** Every screen has designed empty, loading, error and success states. No blank screens, no raw exception text.
   - **h.** Light and dark both look intentional. Test both with dynamic color ON and OFF.

---

STANDARD VERIFICATION (SV): run and paste the real final lines of each. Skip a command only if its target does not exist yet, and say so.
  ./gradlew :modem:test
  ./gradlew assembleDevDebug
  ./gradlew assembleUserDebug
  ./gradlew lintDevDebug lintUserDebug
  ./gradlew testDevDebugUnitTest testUserDebugUnitTest
  ./gradlew checkNoHardcodedStyle
  ./gradlew verifyUserApkClean
  ./gradlew checkNoNetworkClients
  Merged manifest permission check for both flavors: list the permissions and confirm it matches Rule 24 (replaced) exactly.
If a command fails: stop, fix, re-run. Do not continue to the next step or claim success while anything fails.

DOCUMENTATION (every step)
  1. Update docs/PROJECT_MEMORY.md (current state, module and package map, flavors, tokens, protocol status).
  2. Update docs/DECISIONS.md (dated entry for every decision and every new dependency).
  3. Update docs/ROADMAP.md (step status).
  4. Write docs/reports/step-NN-report.md using the report template.

REPORT TEMPLATE
  - Step number and title
  - What was implemented (files created or changed)
  - Command results: each command with the final lines of its real output
  - Checks NOT run and why
  - Deviations from the prompt or rules, and why
  - OWNER RUNS: anything that needs real phones, speakers, microphones or a room
  - Known issues and warnings
  - Open questions for the owner
  - Suggested next step

SESSION START: read docs/RULES.md, docs/PROJECT_MEMORY.md, docs/DECISIONS.md and docs/ROADMAP.md before doing anything. Implement only the current step. When it is done, write the report and STOP. Do not start the next step.
