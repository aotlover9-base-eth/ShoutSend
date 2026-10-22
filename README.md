# ShoutSend

A native Android application (100% Kotlin, Jetpack Compose, Material 3, MVVM) that transmits text, URLs, Wi-Fi credentials, and contacts between devices using sound waves. Zero Wi-Fi, Bluetooth, cellular, or cloud pairing required.

## Build Variants

ShoutSend supports two distinct product flavors on the `audience` dimension:

1. **Developer (`dev`)**:
   - Application ID: `app.shoutsend.dev`
   - App Label: `ShoutSend (Dev)`
   - Includes real-time runtime token editor, DSP Lab, and developer diagnostics (`src/dev`).

2. **User (`user`)**:
   - Application ID: `app.shoutsend`
   - App Label: `ShoutSend`
   - Production build. Cleaned of all developer panel classes and tooling (`src/user`).

Both flavors can be installed side-by-side on the same physical device or emulator.

### Switching in Android Studio
Open the **Build Variants** panel (`View -> Tool Windows -> Build Variants`) and select:
- `devDebug` or `userDebug` for active development.
- `devRelease` or `userRelease` for production builds.

## Command-Line Builds

### Build Developer Debug APK
```bash
./gradlew assembleDevDebug
```
Output:
`app/build/outputs/apk/dev/debug/app-dev-debug.apk`

### Build User Debug APK
```bash
./gradlew assembleUserDebug
```
Output:
`app/build/outputs/apk/user/debug/app-user-debug.apk`

### Build Both Debug APKs
```bash
./gradlew assembleBothDebug
```

### Run Unit Tests & Lint
```bash
./gradlew testDevDebugUnitTest testUserDebugUnitTest
./gradlew lintDevDebug lintUserDebug
```

## Permissions Policy
- `RECORD_AUDIO`: Microphone access required exclusively for the acoustic receiver.
- `VIBRATE`: Tactile feedback on symbol decoding and transmission.
- `INTERNET`: **Strictly forbidden and absent** from all merged manifests. 100% offline.
