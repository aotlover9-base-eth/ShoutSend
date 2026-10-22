# ShoutSend Decision Log

## 2026-10-22 - Step 1: Project Setup Decisions

### DEC-001: Toolchain and Android SDK Versions
- **Decision**: Set `compileSdk = 35`, `targetSdk = 35`, `minSdk = 31`.
- **Reason**: SDK 35 is installed and stable. Android 12 (SDK 31) provides full native support for Dynamic Color and low-latency audio APIs.
- **Java Toolchain**: Java 17 compatibility configured in Gradle.

### DEC-002: Version Catalog and Initial Dependencies
- **Decision**: Added Gradle Version Catalog (`gradle/libs.versions.toml`) with:
  - Compose BOM `2024.06.00`
  - Kotlin `2.0.0`
  - AGP `8.5.2`
  - AndroidX Core KTX `1.13.1`
  - Activity Compose `1.9.0`
  - Lifecycle `2.8.2` (Runtime KTX, Runtime Compose, ViewModel Compose)
  - Navigation Compose `2.7.7`
  - Kotlinx Coroutines `1.8.1` (Core, Android, Test)
  - Kotlinx Serialization JSON `1.6.3`
  - DataStore Preferences `1.1.1`
  - JUnit `4.13.2`
- **Reason**: Standardized dependencies approved in Section 7 of roadmap. No Room or Hilt added yet.

### DEC-003: Multi-Flavor Seam Architecture (Rule 15)
- **Decision**: Flavors configured on dimension `audience`: `dev` and `user`.
  - Application ID: `app.shoutsend.dev` (dev) and `app.shoutsend` (user).
  - App Name: `ShoutSend (Dev)` vs `ShoutSend`.
  - DevTools interface in `src/main`, bound to `DevToolsImpl` in `src/dev` and `NoOpDevTools` in `src/user`.
- **Reason**: Guarantees zero developer tooling code or leakages in the production User APK.

### DEC-004: Zero Permissions in Initial Scaffolding
- **Decision**: Manifest contains zero permissions at Step 1.
- **Reason**: Permissions (`RECORD_AUDIO` and `VIBRATE`) will be introduced strictly in Step 26 with proper rationale.
