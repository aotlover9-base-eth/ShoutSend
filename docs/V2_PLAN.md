# ShoutSend v2: Material You Redesign + Real File Sharing

Antigravity instruction file. Paste **Section 3 (V2 Brief) together with Step R0** first. After that, paste one step at a time. After each step, the owner pastes `docs/reports/v2-step-RNN-report.md` back to Claude for review before the next step.

---

## 1. Why v2 exists (read this, Antigravity)

v1.0.0 has a working modem: `:modem` (MFSK/DTFSK, Goertzel, Reed-Solomon, CRC, SecureBox), AudioTrack transmitter, AudioRecord receiver, Room vault, DataStore settings, dev/user flavors. **Keep all of that. Do not rewrite the modem.**

v1 failed on two things:

1. **It is not usable for what people want.** Users want to send *files* (photos, PDFs, documents). v1 can only send text, URLs, Wi-Fi and contacts, capped at 320 bytes.
2. **The UI is "AI slop".** Examples from the current code:
   - Emoji and Unicode glyphs used as icons: `"▲" "▼" "■"` for navigation (`navigation/Routes.kt`), `"🔒" "📋" "ℹ"` inside screens.
   - Hand-picked neon palette (`#00F0FF`, `#00FF66`, `#FF007F`) with "glow", "scanline", "syncMarker" color roles. This is not Material You.
   - `NeonCard`, `ScanlineOverlay`, `RadioReadout`, ALL-CAPS monospace readouts like `SOURCE: MIC`, raw dBFS/SNR numbers shown to normal users.
   - Dynamic color is **off** by default.
   - ViewModels are created with `remember { }` inside `MainActivity` instead of `viewModel(factory = …)`. They never get `onCleared()`, and all state is lost on rotation.
   - `ReceiveSession` / `SessionLifecycleManager` exist but are never wired (dead code).

Physics constraint (do not argue with it): sound carries about **10 to 25 bytes per second**. A 2 MB photo over sound would take more than a day. So v2 uses a **hybrid**: sound is the magic "tap-free pairing", and big files travel over **Wi-Fi Direct**, phone to phone, with no internet and no router.

---

## 2. What v2 must deliver

| Area | Outcome |
|---|---|
| Design | Strict **Material 3 Expressive** (Material You). Dynamic color ON by default. Playful: shape morphing, springy motion, big rounded shapes, real Material Symbols icons. Zero neon, zero emoji icons. |
| Layout | **Home hub + History**: bottom bar with 2 destinations; Settings in the top app bar. |
| Small payloads | Text, Link, Wi-Fi, Contact over sound (unchanged modem). Plus **tiny files** (fits the existing 320-byte air limit after compression) over sound. |
| Real files | Any size, multiple files. Sound chirps a one-time **Wi-Fi Direct invite**. The receiver taps Accept and the file streams over Wi-Fi Direct, encrypted, at MB/s. Saved to `Downloads/ShoutSend`. |
| Share sheet | "Share to ShoutSend" from Gallery, Files and other apps (text **and** files). |
| Quality | Rotation-safe ViewModels, lifecycle-correct listening, accessibility, edge-to-edge, predictive back. |

---

## 3. V2 BRIEF (paste once with Step R0; Step R0 saves it as `docs/V2_BRIEF.md`)

```text
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
```

---

## 4. Roadmap at a glance

| Step | Title | Touches UI |
|---|---|---|
| R0 | Baseline audit and v2 docs (no code changes) | No |
| R1 | Foundation: dependency upgrade, proper ViewModels, permission and network guards | No |
| R2 | New Material 3 Expressive design system and dev panel update | Components only |
| R3 | App shell and Home hub | Yes |
| R4 | Composer sheets: Text, Link, Wi-Fi, Contact | Yes |
| R5 | Sending and receiving moments, result cards | Yes |
| R6 | History, Settings, Onboarding, Calibration redesign | Yes |
| R7 | Protocol additions: FILE and HANDOFF payloads (`:modem`) | No |
| R8 | Wi-Fi Direct transfer engine (`core/transfer`) | No |
| R9 | File sharing UI and share-sheet intake | Yes |
| R10 | Polish: motion, haptics, accessibility, large screens | Yes |
| R11 | Release v2.0.0, README, owner test plan | No |

The UI design decisions are already confirmed by the owner and baked into each step (Rule 17 is satisfied). Do not invent different layouts.

---

## 5. Step prompts

---

### Step R0: Baseline audit and v2 docs

```text
# STEP R0: Baseline audit and v2 docs

## Project context
ShoutSend v1.0.0 is complete (see docs/RUN_SUMMARY.md). v2 starts now. Save the V2 BRIEF I pasted above as docs/V2_BRIEF.md, unchanged. Also save this entire instruction file as docs/V2_PLAN.md.

## Goal of this step
Get a truthful baseline before changing anything. NO production code changes in this step.

## Tasks
1. Save docs/V2_BRIEF.md and docs/V2_PLAN.md.
2. Run the full SV from docs/RULES.md on the current code and paste the real final lines. If anything fails, report it. Do not fix it yet.
3. Write docs/V2_AUDIT.md with:
   a. The real minSdk, targetSdk and compileSdk from app/build.gradle.kts, and the real AGP, Kotlin, Compose BOM and material3 versions. Note every place where docs/RUN_SUMMARY.md or README.md disagree with the code (for example, RUN_SUMMARY says "Min API 26" and "RS(63,k) Cauchy matrix". Check both against the code).
   b. A screen inventory: every route, its file, what it shows, and every place it uses emoji or Unicode glyphs as icons, ALL CAPS, monospace, .uppercase(), glow, neon, scanline, or raw technical numbers. Give file:line for each.
   c. Every ViewModel and how it is constructed today.
   d. Dead code: classes never referenced from production code (start with ReceiveSession and SessionLifecycleManager).
   e. Which design-system components and color roles exist only for the "Cyber Radio" look (NeonCard, ScanlineOverlay, RadioReadout, glow*, scanline, syncMarker, …).
4. Add a "v2" section to docs/ROADMAP.md listing R0 to R11 as todo (R0 done at the end of this step).

## Out of scope
Any code change.

## Verification
SV from docs/RULES.md (baseline). Report results honestly, including failures.

## Documentation
docs/V2_BRIEF.md, docs/V2_PLAN.md, docs/V2_AUDIT.md, docs/ROADMAP.md, docs/reports/v2-step-R00-report.md.
```

---

### Step R1: Foundation

```text
# STEP R1: Foundation: dependency upgrade, proper ViewModels, permission and network guards

## Project context
Read docs/RULES.md, docs/V2_BRIEF.md, docs/V2_AUDIT.md first.

## Goal of this step
Make the base solid before the redesign. No visual changes in this step.

## Tasks
1. Dependency upgrade (record every version in DECISIONS.md):
   - Upgrade AGP, Kotlin, KSP, the Compose BOM, material3, navigation, lifecycle, activity, Room and DataStore to the latest stable versions that work together.
   - material3 must include the Expressive APIs (MaterialExpressiveTheme or MotionScheme.expressive(), MaterialShapes, LoadingIndicator, ButtonGroup). If the latest stable lacks them, use the newest material3 version that has them (beta or alpha), pin it explicitly, and justify it in DECISIONS.md.
   - Add androidx.graphics:graphics-shapes if MaterialShapes or Morph needs it.
   - Add com.materialkolor:material-kolor (Apache-2.0) to generate the full M3 color scheme from one seed color (used for the fallback scheme and the dev panel seed picker). Justify it in DECISIONS.md (purpose, license, size). If it is incompatible with the chosen Compose version, stop and report. Do not hand-type 30 hex values instead.
   - Keep minSdk 31.
2. Proper ViewModels:
   - Create ViewModelProvider.Factory implementations (or viewModelFactory { initializer { } }) that pull dependencies from AppContainer.
   - In composables, obtain every ViewModel with viewModel(factory = …) scoped to its NavBackStackEntry or to the Activity where state must be shared (TransmitViewModel shared between composer and sending screen).
   - Remove every `remember { SomeViewModel(...) }` from MainActivity. Do not hold an Activity Context inside a ViewModel. Use the application context or pass callbacks.
3. Dead code: wire or delete ReceiveSession and SessionLifecycleManager (and anything else from V2_AUDIT.md section d). Prefer deleting if ReceiveViewModel already covers the behavior. Record the choice.
4. Rules in code:
   - Update docs/RULES.md: append the v2 amendments and Rules 29 to 32 from docs/V2_BRIEF.md (keep the old text, mark replaced lines as "replaced in v2").
   - Update the verifyPermissions Gradle task to the exact new permission set from Rule 24 (replaced), for BOTH flavors. Add the new permissions to AndroidManifest.xml now (NEARBY_WIFI_DEVICES with neverForLocation, ACCESS_FINE_LOCATION with maxSdkVersion 32), so the guard passes.
   - Add a Gradle task checkNoNetworkClients that fails the build if app/ or modem/ sources contain any of: HttpURLConnection, HttpsURLConnection, openConnection(, okhttp, ktor, retrofit, java.net.URL( used for I/O, InetAddress.getByName( with a non-literal argument, WebView. Allowlist the future file core/transfer/ only for java.net.Socket and ServerSocket. Hook it into the check task.
   - Extend checkNoHardcodedStyle to also fail on: emoji code points and these glyphs ▲ ▼ ■ ● ◆ ℹ ✓ ✔ ✕ ✖ inside UI string literals in app/src/main; FontFamily.Monospace in app/src/main; .uppercase() applied to text passed to Text(). Allowlist only the dev flavor DSP Lab.
5. Fix every warning you introduced. Existing tests must still pass. Update tests that constructed ViewModels the old way.

## Out of scope
Any visual change, new screens, protocol changes.

## Verification
Full SV + checkNoNetworkClients + manifest permission listing for both flavors. Note: checkNoHardcodedStyle WILL fail on the old UI code (emoji, uppercase). That is expected. Add the existing violations to config/style-allowlist.txt with a "remove in R3 to R6" comment, list them in the report, and make the build pass. R3 to R6 must remove them all.
Also rotate the emulator during a transmission (if available) and confirm state survives. Otherwise write "not verified".

## Documentation
As in the brief. Report: v2-step-R01-report.md.
```

---

### Step R2: Material 3 Expressive design system

```text
# STEP R2: New Material 3 Expressive design system + dev panel update

## Project context
Read docs/RULES.md, docs/V2_BRIEF.md, docs/DESIGN_SYSTEM.md. R1 is done (deps upgraded, guards in place).

## Goal of this step
Replace the "Cyber Radio" design system with a strict Material 3 Expressive system. Screens keep compiling (they may look half-migrated until R3 to R6), but every token and wrapper is new.

## DESIGN DECISIONS (owner confirmed)
- Theme name: ShoutSendTheme (replaces ChirpTheme). Built on MaterialExpressiveTheme with MotionScheme.expressive().
- Color: dynamic color ON by default (dynamicLightColorScheme / dynamicDarkColorScheme). Fallback = MaterialKolor scheme from ONE seed in DefaultTokens.kt: seed = a vivid playful violet (use 0xFF6750A4 unless the owner changes it), style TonalSpot, contrast standard. Light, dark and system-follow (default) supported.
- Extended semantic colors (only these): success / onSuccess / successContainer / onSuccessContainer, and warning with the same 4 roles. Derive them by harmonizing fixed hues with the primary (MaterialKolor or the M3 harmonize algorithm), so they follow dynamic color.
- Waterfall/spectrogram palette: derived from the color scheme (surfaceContainerLowest → primaryContainer → primary → tertiary). Delete all signal*/glow*/scanline/syncMarker/toneGuide roles.
- Shapes: M3 Expressive scale. extraSmall 4, small 8, medium 16, large 24, largeIncreased 28, extraLarge 32, extraLargeIncreased 40, full = pill (values live only in DefaultTokens). Component shape tokens: button = full, card = large, sheet = extraLarge (top only), dialog = extraLarge, textField = large, chip = medium.
- Hero shapes: use MaterialShapes (Cookie9Sided, Sunny, Clover4Leaf, Pill, SoftBurst, etc.) and Morph for playful moments: the Send button, the listening indicator, success bursts. Expose them through a `ShoutShapes` token object so the dev panel can swap the hero shape.
- Typography: the M3 type scale with the device default family (no bundled font). Use the emphasized styles for hero headlines (displaySmall/headlineLarge emphasized, if available in the chosen material3 version). No monospace.
- Spacing tokens: keep the existing scale, rename if needed (xs 4, sm 8, md 12, lg 16, xl 24, xxl 32, xxxl 48).
- Motion: use MaterialTheme.motionScheme spatial and effects springs. Remove custom tween durations where a motionScheme spec fits. Keep reduced-motion support (respect the system animator scale).
- Icons: Material Symbols Rounded, imported as vector drawables into app/src/main/res/drawable with the prefix ic_ (outlined for default, _fill suffix for selected). Download the SVGs from fonts.google.com/icons (Apache-2.0) and convert to VectorDrawable. Needed at minimum: home, history, settings, send, mic, mic_off, text_fields, link, wifi, contact_page, attach_file, image, description, lock, lock_open, content_copy, open_in_new, share, download, check_circle, error, close, arrow_back, more_vert, star, delete, graphic_eq, volume_up, tune, info, qr_code (unused is OK to skip), folder, done_all. If downloading is impossible, add androidx.compose.material:material-icons-extended as a fallback, justify it, and rely on R8 to strip unused icons. Record which path you took.

## Tasks
1. Rewrite core/designsystem:
   - DefaultTokens.kt: seed color, shape dp values, spacing, sizes, strokes, motion constants. The only hex/dp literal file.
   - DesignTokens / TokenOverrides: model = { dynamicColor: Boolean, themeMode: System|Light|Dark, seedColor, contrast, shape scale overrides (per corner token), heroShape }. Keep DataStore persistence, debounce and reset-to-defaults working (Rule 12). Migrate or reset old persisted cyber tokens safely: on decode failure, fall back to defaults. Never crash.
   - ShoutSendTheme: builds colorScheme (dynamic or seed), Shapes, Typography, motionScheme; provides extended colors and ShoutShapes via CompositionLocal.
2. Delete the Cyber Radio components: NeonCard, ScanlineOverlay, RadioReadout, BrandMark (if neon), glow modifiers. Replace usages with M3 equivalents so everything compiles.
3. New component wrappers (thin, token-only, real M3 inside). Rename the Cd* prefix to Shout*:
   ShoutButton (filled, tonal, outlined, text; sizes S/M/L/XL using the expressive button sizes if available), ShoutIconButton, ShoutFab and ShoutExtendedFab, ShoutButtonGroup (connected toggle group for type pickers), ShoutCard (filled/elevated/outlined), ShoutListItem, ShoutTextField (outlined, large shape, supporting text, character/byte counter slot), ShoutSwitch, ShoutChip (filter/assist/input), ShoutBottomSheet (ModalBottomSheet), ShoutDialog, ShoutTopAppBar (medium/large flexible app bar with scroll behavior), ShoutNavigationBar (+ NavigationRail for width ≥ 600 dp), ShoutSnackbarHost, ShoutLoadingIndicator (M3 LoadingIndicator / ContainedLoadingIndicator), ShoutProgress (wavy linear/circular progress if available), ShoutEmptyState (hero shape + title + body + optional action), MorphingShape (animates between two MaterialShapes using Morph and the motionScheme spring).
   Keep LevelMeter / SignalMeter only if rebuilt on the color scheme with no glow.
4. Developer panel (src/dev only, Rules 10 to 12): replace the per-role cyber color editor with: dynamic color toggle, theme mode, seed color picker (hue/chroma sliders + a few preset swatches), contrast level, per-corner-token sliders, hero shape selector, "reset everything". Changes apply instantly app-wide and persist. Keep the DSP Lab working.
5. Previews (dev source set): a "Design system gallery" preview showing every component in light/dark × dynamic off, plus all hero shapes.
6. Update DesignSystemTest / ThemePersistenceTest for the new model.

## Out of scope
Redesigning screen layouts (R3 to R6). Screens only get the minimum changes needed to compile on the new wrappers.

## Verification
Full SV. verifyUserApkClean must confirm the new dev panel classes are absent from the user APK. Screenshots of the design-system gallery (dev build) in light and dark if possible.

## Documentation
Rewrite docs/DESIGN_SYSTEM.md completely for v2 (tokens, components, shapes, icons, how to add things, anti-slop rules). Report: v2-step-R02-report.md.
```

---

### Step R3: App shell and Home hub

```text
# STEP R3: App shell and Home hub

## Project context
Read the brief and docs/DESIGN_SYSTEM.md. R2 delivered ShoutSendTheme and the Shout* wrappers.

## Goal of this step
New navigation structure and a playful, useful Home screen where receiving is automatic and sending starts with one tap.

## DESIGN DECISIONS (owner confirmed)
Navigation
- Bottom NavigationBar with 2 destinations: Home (ic_home / ic_home_fill) and History (ic_history). NavigationRail on width ≥ 600 dp.
- Settings opens from a settings icon in Home's top app bar (not a tab).
- Routes: HomeRoute, HistoryRoute, HistoryDetailRoute(id), SettingsRoute, CalibrationRoute, OnboardingRoute, ComposeRoute(type), SendingRoute, plus routes added in R9. Delete SendRoute/ReceiveRoute/VaultRoute and TopLevelDestination glyphs. Migrate settings.lastTab safely (old values map to Home).
- Transitions: M3 shared-axis style using motionScheme springs. Predictive back enabled (android:enableOnBackInvokedCallback="true").

Home layout (top to bottom, scrollable, edge-to-edge)
1. Top app bar: "ShoutSend" title (headline emphasized), settings icon button on the right.
2. Listening card (filled card, surfaceContainerHigh, large shape):
   - Left: a MorphingShape indicator (Cookie9Sided ↔ Sunny, slow breathing morph while listening; static Circle when paused) in primaryContainer with ic_graphic_eq inside.
   - Title: "Listening for nearby phones" / "Listening paused" / "Mic access needed".
   - Body: a subtle live amplitude line (a smooth wavy line from the existing level meter, drawn in primary, no numbers).
   - A trailing Switch: listening on/off (default on, persisted in settings).
   - Tapping the card opens a "Signal details" bottom sheet (band, level, SNR, sample rate, input source; the only place for technical numbers) with an "Open waterfall" toggle that shows the existing spectrogram, recolored from the scheme.
   - Mic permission missing: the card becomes a tonal call-to-action "Allow microphone to receive" with a button that runs the existing permission flow (including "Open settings" when permanently denied).
3. "What do you want to send?" section title.
4. Type picker: a 2-row grid of large tonal tiles (not tiny chips), each with an icon and label: Text (ic_text_fields), Link (ic_link), Wi-Fi (ic_wifi), Contact (ic_contact_page), File (ic_attach_file). Each tile uses a different container role (primaryContainer, secondaryContainer, tertiaryContainer, …) for a playful look while staying in-scheme. Press = shape morph from large to medium corner plus scale spring plus haptic. Tapping opens the composer for that type (R4). File is visible but shows a "Coming soon" snackbar until R9.
5. Recent card: the last 3 History items as ShoutListItems with an "See all" text button. Empty state: hero shape with "Nothing yet. Things you send and receive show up here."

Listening behavior
- Listening runs only while Home is visible, the app is in the foreground, the switch is on and mic permission is granted. It pauses automatically when sending, on other screens, or when the app goes to the background. Use LifecycleResumeEffect / repeatOnLifecycle in the ViewModel layer. Never listen in the background.
- When a message arrives while on Home, show the result card from R5. For R3, show a simple placeholder bottom sheet with the decoded type and a "View" action into History. R5 replaces it.

## Tasks
1. Implement the routes, NavHost and AppScaffold changes above.
2. HomeScreen + HomeViewModel (merging the listening part of ReceiveViewModel; keep ReceiveViewModel logic but expose a clean HomeUiState: listeningState, amplitude (0..1 float), recentItems, permissionState, incomingProgress).
3. Signal details sheet + recolored waterfall (WaterfallCanvas palette from colorScheme via the R2 waterfall palette).
4. Remove this step's entries from config/style-allowlist.txt (navigation, receive screen header and status). Zero emoji/glyph icons left in navigation and Home.
5. Update NavigationStateTest and add HomeViewModelTest (listening state machine: permission missing, paused by switch, paused by lifecycle, resumed).

## Out of scope
Composer content (R4), sending/receiving visuals (R5), History redesign (R6).

## Verification
Full SV. Screenshots: Home light/dark, dynamic on/off, mic denied state, Signal details sheet.

## Documentation
As in the brief. Report: v2-step-R03-report.md.
```

---

### Step R4: Composer sheets

```text
# STEP R4: Composer: Text, Link, Wi-Fi, Contact

## Project context
Read the brief. Home (R3) opens ComposeRoute(type).

## Goal of this step
Fast, friendly composing for the 4 sound payload types, reusing the existing SendViewModel logic (validation, compaction, secure mode, byte budget, shared-text parsing, contact picker).

## DESIGN DECISIONS (owner confirmed)
- ComposeRoute is a full-screen screen with a large flexible top app bar ("Send text", "Send a link", "Share Wi-Fi", "Share a contact") and a back arrow. It is not a dialog.
- A ShoutButtonGroup at the top lets the user switch type without going back (Text / Link / Wi-Fi / Contact).
- Text: one large outlined field, auto-focus, multi-line. Supporting text shows "Fits in one chirp · about 6 s" / "Too long by 24 characters". No raw byte numbers unless over the limit. Paste-from-clipboard assist chip (ic_content_copy) when the clipboard has text (use the existing ClipboardSanitizer).
- Link: single-line URL field, keyboard type Uri, auto-prefix https:// on blur if missing, inline validation message in human words.
- Wi-Fi: network name, password (visibility toggle with ic_visibility), security segmented button (WPA/WPA2/WPA3, WEP, Open), "Hidden network" switch. A tonal info card: "Tip: turn on Secure mode for passwords".
- Contact: "Pick from contacts" tonal button (existing system picker, no READ_CONTACTS) plus editable name, up to 2 phones and 1 email fields.
- Secure mode: a ShoutListItem row with ic_lock and a switch, "Lock with a passphrase". Turning it on opens a bottom sheet with passphrase + confirm + strength meter (existing logic), with a plain explanation: "The other phone needs the same passphrase."
- Bottom: a sticky extra-large filled Send button (pill, full width minus margins) with ic_send and the estimated time in the label: "Send · about 6 s". Disabled with a reason in supporting text when invalid. Press = morph to the hero shape, then navigate to SendingRoute.
- Share intake: text shared from other apps lands here, pre-filled with the detected type (existing SharedTextParser).

## Tasks
1. ComposeScreen + sub-composables per type using only Shout* wrappers and tokens.
2. Adapt SendViewModel to expose ComposeUiState (type, fields, validation messages in human strings from strings.xml, estimatedSeconds, canSend, secureMode). Keep its tests green and add tests for the new messages and time estimate.
3. Delete the old SendScreen, WifiComposer and ContactComposer UI (keep logic files that are still used). Remove their style-allowlist entries.
4. All strings in res/values/strings.xml, sentence case.

## Out of scope
The sending screen (R5), files (R9).

## Verification
Full SV. Screenshots: each type in light/dark, an invalid state, the secure mode sheet, IME open.

## Documentation
As in the brief. Report: v2-step-R04-report.md.
```

---

### Step R5: Sending and receiving moments

```text
# STEP R5: Sending and receiving moments, result cards

## Project context
Read the brief. Compose (R4) navigates to SendingRoute. Home (R3) shows a placeholder for incoming messages.

## Goal of this step
Make the two key moments delightful and clear: "my phone is chirping" and "I just received something".

## DESIGN DECISIONS (owner confirmed)
Sending screen (SendingRoute, full screen, replaces TransmitScreen and RingsCanvas)
- Center: a large MorphingShape (hero shape, primary color) that pulses with the real playback progress and morphs through 3 MaterialShapes in a loop while transmitting. Around it, 2 to 3 soft expanding rings drawn as outlines in primary at decreasing alpha (no glow, no blur).
- Below: headline "Sending…", body "Hold phones close · about 4 s left", and a wavy LinearProgressIndicator bound to real progress (ShoutProgress).
- A tonal "Cancel" button. Volume hint: if media volume is below 50%, show an assist chip "Turn volume up" that opens the volume panel (existing AudioRouteMonitor knows the route; warn on headphones/Bluetooth: "Sound is going to your headphones").
- Done: the shape morphs to a SoftBurst/Sunny "success" shape in successContainer with ic_check, headline "Sent!", buttons "Send again" (tonal) and "Done" (filled). Success haptic. Failed: errorContainer shape with ic_error, a human reason ("The speaker was busy. Try again.") and "Try again".
- Repeat count and preset are read from settings. No technical numbers on screen.

Receiving (on Home, from the Listening card)
- When the preamble locks, the Listening card expands with a spring: title "Receiving…", a wavy progress bound to fragments received (no "FRAME 2/4" text), MorphingShape speeds up.
- On success: a modal bottom sheet "Result card" slides up:
  - Header: type icon in a tonal hero shape, title ("Text received", "Link received", "Wi-Fi network", "Contact"), timestamp "Just now".
  - Content per type (reuse PayloadActionHelper logic):
    Text: selectable text, actions Copy (primary) and Share.
    Link: the domain large, full URL small, a safety line from UrlSafety in human words; actions Open (primary), Copy, Share.
    Wi-Fi: network name, password hidden with reveal; actions Connect (primary, existing WifiNetworkSuggestion / settings panel flow), Copy password.
    Contact: name, numbers, email; action Add to contacts (primary, ACTION_INSERT intent), Copy.
    Encrypted: a passphrase field with "Unlock" before showing content; wrong passphrase gives a friendly error.
  - Footer: "Saved to History" with a star toggle.
- Failure (CRC/RS failed after repeats): a snackbar "Couldn't hear that clearly. Ask them to send again, a bit closer." No error codes.
- Haptic: light click on lock, success pattern on receive (existing TactileFeedbackController), earcons as in settings.

## Tasks
1. SendingScreen + TransmitViewModel UI state rework (progress 0..1, remainingSeconds, phase: Preparing / Sending / Done / Failed(reason as a string resource), routeWarning).
2. Receiving card states on Home + ResultSheet replacing ReceivedPayloadCard and SegmentedProgressBar UI.
3. Map every TransmitState.Failed reason and receive failure to a human string resource. Add tests for the mapping.
4. Delete TransmitScreen, RingsCanvas, ReceivedPayloadCard, SegmentedProgressBar (keep logic still used). Remove their style-allowlist entries.

## Out of scope
History screen (R6), files (R9).

## Verification
Full SV + existing TransmitViewModelTest/ReceiveViewModelTest updated. Screenshots: sending, done, failed, receiving card, each result sheet type, encrypted unlock.
OWNER RUNS: real two-phone send/receive of all 4 types in Retro mode.

## Documentation
As in the brief. Report: v2-step-R05-report.md.
```

---

### Step R6: History, Settings, Onboarding, Calibration

```text
# STEP R6: History, Settings, Onboarding, Calibration redesign

## Project context
Read the brief. These screens still have v1 visuals.

## Goal of this step
Finish the visual migration. After this step, config/style-allowlist.txt must contain no v1 UI entries.

## DESIGN DECISIONS (owner confirmed)
History (replaces Vault; the Room database and Keystore encryption stay)
- Large flexible top app bar "History", with a search icon that expands into an M3 SearchBar (search in decrypted titles in memory only).
- Filter chips row: All, Sent, Received, Starred, and the types (Text, Link, Wi-Fi, Contact, File).
- List grouped by day ("Today", "Yesterday", date) using ShoutListItem: leading type icon in a tonal shape, title (first line or domain or network name or contact name or file name), supporting text ("Received · 14:02"), trailing star.
- Swipe to delete with an undo snackbar. Long-press for multi-select (contextual top bar with delete/share).
- Empty state with a hero shape. Secrets (Wi-Fi passwords) stay masked in the list.
- Detail screen: the same content blocks and actions as the Result sheet (R5) for consistency, plus Delete and "Send again".

Settings (grouped M3 list, large top app bar)
- Appearance: Dynamic color (on), Theme (System/Light/Dark) as a segmented button.
- Sound: Sound mode Audible/Ultrasonic (rename Retro → "Audible", Stealth → "Quiet (near-ultrasonic)", with the existing safety disclosure dialog), Speed (Reliable / Balanced / Fast), Repeat count, Earcons, Haptics.
- Receiving: Auto-listen on Home, "Save received items to History".
- Files (enabled in R9): "Save files to" (Downloads/ShoutSend, read-only info), "Ask before receiving files" (always on, shown disabled with explanation).
- Tools: "Test your phones" (Calibration), "Signal details".
- Privacy: "Clear history" (confirm dialog), short privacy summary.
- About: version, licenses (OSS list for Material Symbols and MaterialKolor), "How it works" sheet.

Onboarding (3 pages max, horizontal pager, playful)
1. "Send things with sound": big morphing hero shape, short body.
2. "Big files go over Wi-Fi Direct": shape illustration plus "No internet needed, phones talk directly." (text only now; real flow in R9).
3. "Allow microphone": permission CTA (existing flow) and a "Not now" text button.
Page indicator, Skip, and Next as an extended FAB that morphs into "Get started" on the last page.

Calibration / Link test
- Same logic, new UI: a stepper ("Place phones 30 cm apart" → "Testing audible" → "Testing quiet mode" → result), result as a card with a plain recommendation ("Audible mode works great. Quiet mode is weak on this phone pair."), with an expandable "Details" for the numbers.

## Tasks
1. Implement the screens above with Shout* wrappers only.
2. Rename user-facing "Vault" to "History" and "Retro/Stealth" to "Audible/Quiet" in strings only (keep internal enum names; record this in DECISIONS.md).
3. Empty all v1 entries from config/style-allowlist.txt. checkNoHardcodedStyle must pass with the allowlist containing only dev DSP Lab entries.
4. Update VaultViewModelTest, SettingsViewModelTest, OnboardingViewModelTest and CalibrationViewModelTest.

## Out of scope
File features beyond showing placeholders (R9).

## Verification
Full SV. Screenshots of every screen in light/dark. grep the main source set for emoji/glyph icons and paste the empty result.

## Documentation
As in the brief. Report: v2-step-R06-report.md.
```

---

### Step R7: Protocol additions (`:modem`)

```text
# STEP R7: Protocol additions: FILE and HANDOFF payloads

## Project context
Read docs/PROTOCOL.md and the brief. Protocol v1 MessageType uses ids 0 to 3, and 4 to 7 are reserved. MAX_AIR_PAYLOAD_BYTES = 320.

## Goal of this step
Add two payload types in pure Kotlin, without changing framing, FEC, timing or existing type encodings. Old receivers ignore the new types gracefully.

## Decisions
- MessageType.FILE = 4: a tiny file sent entirely over sound.
  Body: [nameLen u8][name UTF-8 ≤ 40 bytes][mimeCode u8 (table: 0 octet-stream, 1 text/plain, 2 text/vcard, 3 text/calendar, 4 application/json, 5 text/markdown, 6 text/csv, 255 = explicit mime follows)][optional mimeLen u8 + mime][data].
  data is raw or DEFLATE (java.util.zip, no dependency). Pick whichever is smaller and set a flag bit inside the body (first byte high bit) so the receiver knows.
  The total after optional SecureBox must be ≤ 320 bytes on air. Expose `FilePayload.fitsOverSound(name, mime, bytes): Boolean` so the app can decide sound vs Wi-Fi Direct.
- MessageType.HANDOFF = 5: a one-time Wi-Fi Direct invite. Target ≤ 64 bytes on air:
  [version u8 = 1]
  [ssidSuffix 2 ASCII alnum chars]     // network name is "DIRECT-" + suffix + "-ShoutSend"
  [passphrase 10 ASCII chars from a 62-char alphabet, from SecureRandom]
  [port u16]
  [sessionKey 16 bytes, from SecureRandom]
  [fileCount u8 (1..255)]
  [totalSize varint (bytes)]
  [kindHint u8: 0 mixed, 1 images, 2 video, 3 audio, 4 documents, 5 archive, 6 other]
  [firstNameLen u8][firstName UTF-8 truncated to ≤ 20 bytes]
  The SecureBox passphrase mode still works on top (encrypted flag). The data class toString() must never print the passphrase, key or name (Rule 30).
- Receiving an unknown future type (6, 7): the receiver emits a ReceiveEvent that the UI maps to "Update ShoutSend to open this", never a crash.

## Tasks
1. MessageType, Payload.File, Payload.Handoff, PayloadCodec encode/decode, validation, PayloadCompactor pass-through (no compaction for HANDOFF; FILE uses its own DEFLATE choice).
2. Tests: round-trip, boundary sizes (exactly 320, 321), DEFLATE choice, invalid/truncated bodies, toString redaction, SecureBox + FILE/HANDOFF, and channel-simulator loopback for HANDOFF at Balanced speed (report the decode rate and air time in seconds for MFSK Normal and DTFSK Fast).
3. Update docs/PROTOCOL.md (v1.1: new types only, framing unchanged) and record it in DECISIONS.md with the simulator numbers (Rule 28).

## Out of scope
Android code, UI, Wi-Fi Direct.

## Verification
Full SV (:modem:test, checkModemPurity included).

## Documentation
As in the brief. Report: v2-step-R07-report.md.
```

---

### Step R8: Wi-Fi Direct transfer engine

```text
# STEP R8: Wi-Fi Direct transfer engine (core/transfer)

## Project context
Read the brief, docs/PROTOCOL.md (v1.1 HANDOFF). Permissions were declared in R1.

## Goal of this step
A tested, UI-free engine that sends N files from a sender phone to a receiver phone over Wi-Fi Direct, triggered by a HANDOFF invite heard over sound.

## Decisions
Roles: the sender is always the Group Owner (GO). The GO IP is 192.168.49.1 on Android.

Sender flow
1. Check: Wi-Fi enabled (else return NeedsWifi), permission granted (NEARBY_WIFI_DEVICES on 33+, ACCESS_FINE_LOCATION on 31-32, else NeedsPermission), and on 31-32 that Location is on (else NeedsLocation).
2. WifiP2pManager.initialize → removeGroup (clear stale) → createGroup(channel, WifiP2pConfig.Builder().setNetworkName("DIRECT-" + suffix + "-ShoutSend").setPassphrase(passphrase).setGroupOperatingBand(GROUP_OWNER_BAND_AUTO).enablePersistentMode(false).build()).
3. Open a ServerSocket on an ephemeral port, accept timeout 90 s.
4. Return the Handoff payload to the caller (the app chirps it with the existing TransmitSession; repeat the chirp every 15 s, up to 4 times, until a client authenticates).
5. Authenticate the first client (see the wire protocol). Accept exactly one authenticated client, then stop accepting.
6. Stream files. Then close the socket and removeGroup. Always tear down in finally, including on cancel, error or timeout.

Receiver flow
1. On a decoded HANDOFF, the app asks the user (R9). On Accept: the same checks as the sender.
2. connect(channel, WifiP2pConfig.Builder().setNetworkName(name).setPassphrase(pass).build()). This joins the group without peer discovery or a system dialog.
3. Listen for WIFI_P2P_CONNECTION_CHANGED_ACTION, then requestConnectionInfo → groupOwnerAddress. Connect a Socket with a 10 s timeout, retrying 3 times with backoff.
4. Authenticate, receive, verify, save, disconnect (removeGroup / cancelConnect) in finally.

Wire protocol (version 1, all integers big-endian)
- Keys: HKDF-SHA256(sessionKey, salt = "ShoutSend-xfer-v1") → encKey (32 bytes) + macKey (32 bytes).
- Handshake: server sends 16-byte nonceS. Client replies with 16-byte nonceC + HMAC-SHA256(macKey, "C" | nonceS | nonceC). Server verifies (constant-time compare), then replies HMAC(macKey, "S" | nonceC | nonceS). Client verifies. On any failure, close immediately.
- Manifest frame (encrypted): fileCount, then per file: name (sanitized: strip path separators and control chars, max 120 chars), mime, size.
- Data: each file is sent as 64 KiB chunks. Each chunk is AES-256-GCM with a 12-byte nonce = 4-byte file index + 8-byte chunk counter, AAD = file index | chunk index | isLast. Per file trailer: SHA-256 of the plaintext.
- Receiver: write to MediaStore.Downloads with RELATIVE_PATH "Download/ShoutSend" and IS_PENDING = 1. On a verified hash, set IS_PENDING = 0. On failure or cancel, delete the pending row. Resolve duplicate names "photo (1).jpg".
- Progress: bytesDone/bytesTotal, current file index, throughput, emitted ≤ 10 times per second.
- Limits: max 255 files, max total 8 GiB, otherwise reject before chirping.

Architecture
- core/transfer/: WifiDirectController (wraps WifiP2pManager + BroadcastReceiver into suspend functions/Flows), TransferCrypto (pure, JVM-testable), TransferProtocol (pure; works on InputStream/OutputStream so it is testable with piped streams), FileSource (ContentResolver reads by Uri), FileSink (MediaStore writes), TransferSession (state machine: Idle → Preparing → WaitingForReceiver → Connecting → Transferring(progress) → Done(files) / Failed(reason) / Cancelled).
- Sealed failure reasons with human string resources: NeedsWifi, NeedsPermission, NeedsLocation, WifiDirectUnsupported, Timeout, AuthFailed, IntegrityFailed, StorageFull, Cancelled, Unknown.
- Only these files may use java.net.Socket/ServerSocket (checkNoNetworkClients allowlist). Bind/connect only to 192.168.49.x. Assert this in code.
- No logging of names, keys or passphrases (Rule 30). Use redacted toString().
- Keep the screen on during transfer (the UI does it in R9). No foreground service.

## Tasks
1. Implement the above. Add a PermissionRepository extension for the Wi-Fi Direct permission set (API-level aware).
2. Tests (JVM): HKDF/HMAC/GCM vectors, a handshake success and every failure path, a tampered chunk → IntegrityFailed and no file committed, truncated stream, multi-file round-trip over PipedInput/OutputStream with fake FileSource/FileSink, name sanitization (../../evil, control chars, very long names), duplicate naming, progress throttling, cancel mid-transfer cleans up.
3. androidTest (compile only if no device): WifiDirectController smoke test.

## Out of scope
UI (R9).

## Verification
Full SV + checkNoNetworkClients (must pass with the core/transfer allowlist only) + manifest permissions for both flavors.
OWNER RUNS (after R9): real two-phone transfer.

## Documentation
docs/TRANSFER.md (flows, wire protocol, threat model: anyone who records the chirp learns the invite. Mitigations: one-time keys, first-authenticated-client-only, receiver must Accept, Secure mode wraps the invite with a passphrase, group torn down after). Report: v2-step-R08-report.md.
```

---

### Step R9: File sharing UI and share-sheet intake

```text
# STEP R9: File sharing UI and share-sheet intake

## Project context
Read the brief, docs/TRANSFER.md. R7 added FILE/HANDOFF payloads, R8 the transfer engine.

## Goal of this step
Make "send a file" as easy as "send text", with clear states and zero dead ends.

## DESIGN DECISIONS (owner confirmed)
Picking
- Home "File" tile opens a bottom sheet with two big options: "Photos & videos" (Photo Picker, PickMultipleVisualMedia) and "Files" (OpenMultipleDocuments, */*). No storage permission.
- Share intake: add intent filters for ACTION_SEND and ACTION_SEND_MULTIPLE with */* (keep text/* handling). Shared files go straight to the file compose screen.

File compose screen (ComposeRoute(File))
- A list of selected files (thumbnail for images/video via ContentResolver.loadThumbnail, otherwise a type icon in a tonal shape), name, size, remove button. "Add more" text button.
- A transport line, decided automatically:
  - All files fit over sound (single file and FilePayload.fitsOverSound): "Sends by sound · about 12 s".
  - Otherwise: "Sends over Wi-Fi Direct · 24 MB · the other phone taps Accept".
- Secure mode row (wraps the sound payload / invite with a passphrase).
- Sticky XL Send button.

Sending a file over Wi-Fi Direct (reuses SendingRoute with file phases)
1. Preparing: "Setting up a direct connection…" (LoadingIndicator).
2. Inviting: the hero shape chirp animation as in R5, "Ask them to open ShoutSend on Home". The invite repeats automatically. A countdown ring shows the 90 s window. "Cancel" button.
3. Connecting: "They accepted. Connecting…"
4. Transferring: big wavy circular progress with % in the center, "3 of 5 files · 12.4 MB/s", file name line, Cancel.
5. Done: success burst, "Sent 5 files", Done/Send more. Failed: a human reason + a fix action: NeedsWifi → "Turn on Wi-Fi" (Settings.Panel.ACTION_WIFI), NeedsPermission → request / open settings, NeedsLocation (Android 12) → "Turn on location", Timeout → "No one accepted. Try again", WifiDirectUnsupported → "This phone can't do Wi-Fi Direct".
- Request the Wi-Fi Direct permission just in time (when the user first sends or accepts a big file) with a rationale sheet: "ShoutSend uses Wi-Fi Direct to move big files straight to the other phone. It never uses the internet or your location."

Receiving a file
- Tiny FILE over sound: a Result sheet (R5 style) "File received" with name, size, actions Save (writes to Downloads/ShoutSend via MediaStore), Open, Share.
- HANDOFF invite: a Result sheet "Incoming files": kind icon, "photo.jpg and 4 more · 24 MB", buttons Decline (text) and Accept (filled). Never auto-accept.
- After Accept: the sheet becomes the progress view (connecting → transferring with per-file progress), then "Saved to Downloads/ShoutSend" with Open (ACTION_VIEW via the MediaStore Uri) / Share / Show in History.
- Keep the screen on (FLAG_KEEP_SCREEN_ON via a Compose effect) during transfer on both phones. If the app goes to the background mid-transfer, cancel cleanly and show "Transfer stopped because ShoutSend was closed".
- Listening pauses during a transfer and resumes after.

History
- New vault item kind FILE_TRANSFER storing direction, file count, names (encrypted with the Keystore key like other secrets), total size, MediaStore Uris (received) and status. Never store file bytes in Room. Room migration v1 → v2 with a migration test.
- History detail: file list with Open/Share for received files that still exist ("File was moved or deleted" otherwise).

## Tasks
1. Implement the picking, composing, sending and receiving UI and ViewModels (FileComposeViewModel, TransferViewModel) on top of TransferSession.
2. Intent filters + MainActivity intake for files (Uri permissions: takePersistableUriPermission is NOT needed; copy nothing, stream directly while the grant is valid).
3. Settings → Files section enabled. Onboarding page 2 stays text-only.
4. Tests: transport decision, state → UI mapping for every failure reason, Room migration, share-intent parsing for single/multiple/mixed.

## Out of scope
Background transfers, resume after disconnect, sending to multiple receivers.

## Verification
Full SV + checkNoNetworkClients + manifest permissions both flavors. Screenshots of every file state.
OWNER RUNS (paste results back): two phones, (a) a 150-byte .txt over sound, (b) one 5 MB photo, (c) 10 photos ~40 MB, (d) one 300 MB video, (e) decline, (f) sender cancel mid-way, (g) Wi-Fi off on receiver, (h) Secure mode with the correct and a wrong passphrase. Note phone models and Android versions, MB/s and failures.

## Documentation
As in the brief, plus docs/TRANSFER.md UI section. Report: v2-step-R09-report.md.
```

---

### Step R10: Polish

```text
# STEP R10: Polish: motion, haptics, accessibility, large screens

## Project context
Read the brief. All features exist.

## Goal of this step
Make it feel premium and playful everywhere, and fix rough edges.

## Tasks
1. Motion audit: every navigation and state change uses motionScheme springs. Shared-element transition from a Home type tile into its composer (SharedTransitionLayout) and from a History item into its detail. Press feedback (scale + corner morph) on tiles and the big buttons. Respect reduced motion (system animator scale 0 → no morph loops, instant transitions).
2. Haptics map: tile press = light tick, send start = confirm, receive lock = click, success = success pattern, error = reject. Use the HapticFeedbackConstants available on API 31+ via the existing controller. Toggleable in settings.
3. Accessibility: TalkBack labels and order on every screen; live regions for listening/receiving/transfer progress ("Receiving, 50 percent"); touch targets ≥ 48 dp; contrast check with dynamic color off at the high contrast level; font scale 200% without clipping; the Send button reachable with switch access.
4. Large screens and foldables: NavigationRail ≥ 600 dp; Home in two panes (listening + recent on the left, type grid on the right) ≥ 840 dp; History list-detail with ListDetailPaneScaffold if available (justify the adaptive dependency) or a simple two-pane Row.
5. Edge-to-edge and insets everywhere (IME, nav bar, display cutout). Predictive back animations work on all sub-screens and sheets.
6. Empty/loading/error states review on every screen (Rule 32g). Add any that are missing.
7. App icon: an adaptive icon with a monochrome layer (themed icons), playful hero-shape glyph from tokens, no neon. Splash via core-splashscreen only if already present. Otherwise use the system default with the icon.
8. Remove every remaining v1 asset, string or color role not used anymore. Run lint and fix all new warnings.

## Verification
Full SV + lint with 0 new warnings. Screenshots: tablet/foldable width, font scale 200%, TalkBack focus order list in the report.

## Documentation
As in the brief. Report: v2-step-R10-report.md.
```

---

### Step R11: Release v2.0.0

```text
# STEP R11: Release v2.0.0, README, owner test plan

## Project context
Read the brief. R10 done and reviewed.

## Tasks
1. Bump versionName to 2.0.0 and versionCode accordingly. Build signed devRelease and userRelease with the existing signing setup (keystore stays outside the repo). Confirm R8 keeps the transfer and crypto code working (add keep rules only if a release smoke test proves they are needed).
2. verifyUserApkClean, checkNoNetworkClients, verifyPermissions on the release APKs. Paste the output.
3. README rewrite: honest pitch ("Small stuff by sound. Big files by Wi-Fi Direct. Never the internet."), real screenshots from docs/screenshots (light and dark), the permission list with a one-line reason each, speed table (sound: measured numbers from the owner's field tests if available, otherwise "not yet measured"; Wi-Fi Direct: from R9 OWNER RUNS), limits, privacy/threat model summary linking docs/TRANSFER.md.
4. docs/FIELD_TEST_PLAN_V2.md: the R9 OWNER RUNS matrix plus sound tests per mode/speed/distance, and a results table to fill in.
5. CHANGELOG.md with a 2.0.0 entry. Do NOT publish a GitHub release or push tags. Prepare the notes in docs/RELEASE_NOTES_2.0.0.md and STOP. The owner publishes.

## Verification
Full SV on release variants + APK sizes + sha256 of both APKs.

## Documentation
As in the brief. Report: v2-step-R11-report.md (include a "what is NOT verified" list).
```

---

## 6. Owner notes (for you, not Antigravity)

- **Paste order:** Section 3 brief + Step R0 → wait for the report → send it to Claude → next step. Do not batch steps. Last time, batching is where quality fell apart.
- **Red flags to watch for in reports:** "should work", missing real Gradle output, "screenshots not verified" on UI steps when an emulator exists, new entries in `style-allowlist.txt` after R6, any permission outside the Rule 24 list, any `remember { …ViewModel(` left.
- **Things only you can test:** real chirps between two phones (R5, R9), Wi-Fi Direct speed and reliability across phone brands (Samsung/Pixel/Xiaomi behave differently), and Quiet (ultrasonic) mode on your specific phones.
- **Trade-off you accepted:** the app no longer has "mic + vibrate only". It now declares INTERNET (local sockets only, guarded by `checkNoNetworkClients`) and Nearby Wi-Fi devices. On Android 12 only, Wi-Fi Direct also needs the location permission. That is an Android rule, not a choice.
