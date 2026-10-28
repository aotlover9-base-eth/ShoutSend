PROJECT: ShoutSend v2. Existing native Android app (Kotlin, Compose, MVVM, flavors dev/user, :modem pure Kotlin module). v2 = complete Material 3 Expressive UI redesign + real file sharing (sound-triggered Wi-Fi Direct handoff). The modem and protocol v1 framing stay as they are.

ALL 28 RULES IN docs/RULES.md STILL APPLY, WITH THESE AMENDMENTS (record each in docs/DECISIONS.md, dated, "owner approved v2"):

Rule 3 (amended): Material 3 Expressive (Material You). Use MaterialExpressiveTheme (or MaterialTheme + MotionScheme.expressive() if the wrapper is unavailable), MaterialShapes, shape morphing, LoadingIndicator, ButtonGroup, expressive motion springs. Use real Material 3 components. No look-alike custom components when an M3 component exists.

Rule 6 (amended): Dynamic color (wallpaper) is ON by default on all supported devices. The fallback scheme is generated from ONE seed color stored in DefaultTokens.kt. That file stays the only place with hex values.

Rule 24 (replaced): The permission set is exactly:
  RECORD_AUDIO, VIBRATE,
  INTERNET            (local sockets on the Wi-Fi Direct link only, never the internet),
  ACCESS_WIFI_STATE, CHANGE_WIFI_STATE,
  NEARBY_WIFI_DEVICES (android:usesPermissionFlags="neverForLocation"),
  ACCESS_FINE_LOCATION (android:maxSdkVersion="32", needed for Wi-Fi Direct on Android 12 only).
  No other permission. No storage permission (use the Photo Picker, SAF and MediaStore). No foreground service in v2.

NEW RULES
29. No network clients. No HTTP library, no URL.openConnection, no HttpURLConnection, no DNS lookups, no analytics. Sockets may only connect to or listen on the Wi-Fi Direct group (192.168.49.0/24). A Gradle guard enforces this (Step R1).
30. Files never touch app logs. Never log file names, sizes linked to names, URIs, passphrases, invite contents or session keys, in any build type.
31. Every file transfer is authenticated and encrypted end to end with a one-time session key (Step R8). A transfer that fails authentication or integrity is discarded, never partially saved as "complete".
32. ANTI-SLOP UI RULES (enforced by review and, where possible, by checkNoHardcodedStyle):
   a. Icons come ONLY from Material Symbols Rounded (vector drawables in res/drawable, prefix ic_, filled variant for selected states). No emoji, no Unicode glyph icons (▲ ▼ ■ ● ℹ 🔒 📋 etc.) anywhere in UI code.
   b. Sentence case everywhere. No ALL CAPS labels, no .uppercase() on UI text, no monospace font in user-facing UI. (The dev-only DSP Lab may use monospace.)
   c. No glows, neon, scanlines, drop-shadow "bloom", or gradient backgrounds. Depth comes from M3 tonal surfaces (surfaceContainer roles) and elevation tokens.
   d. Human copy: "Listening for nearby phones", "Sent!", "Ask them to open ShoutSend". Never jargon like "SYNC LOCK", "SNR 12.4 dB", "FRAME 2/4", "SOURCE: MIC" on main screens. Technical numbers live only in an optional "Signal details" bottom sheet.
   e. One primary action per screen. No cards inside cards. No more than 2 type sizes in one card.
   f. Every interactive element is at least 48 dp, has a content description or a visible label, and gives feedback (ripple, state change, haptic where it matters).
   g. Every screen has designed empty, loading, error and success states. No blank screens, no raw exception text.
   h. Light and dark both look intentional. Test both with dynamic color ON and OFF.

STANDARD VERIFICATION (SV) for every v2 step: the SV block in docs/RULES.md, plus:
  ./gradlew checkNoNetworkClients        (exists from Step R1)
  Merged manifest check for BOTH flavors: list every permission and confirm it matches Rule 24 (replaced) exactly.
  If an emulator or device is available: install devDebug, take screenshots of every screen this step touched, in light and dark (adb exec-out screencap -p > docs/screenshots/rNN-<screen>-<light|dark>.png), and list them in the report. If none is available, write "screenshots not verified" in the report.

DOCUMENTATION (every v2 step): update docs/PROJECT_MEMORY.md, docs/DECISIONS.md, docs/ROADMAP.md (add a "v2" section with R0 to R11 status), docs/DESIGN_SYSTEM.md when tokens or components change, docs/PROTOCOL.md when the protocol changes, and write docs/reports/v2-step-RNN-report.md with the REPORT TEMPLATE from docs/RULES.md plus a "Screenshots" section.

SESSION START: read docs/RULES.md, docs/V2_BRIEF.md, docs/PROJECT_MEMORY.md, docs/DECISIONS.md and docs/ROADMAP.md. Implement ONLY the current step. When it is done, write the report and STOP.
