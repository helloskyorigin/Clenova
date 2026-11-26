# CLENOVA

**Your phone. On autopilot.**

CLENOVA is a native Android intelligent storage management application built around explicit user control, on-device processing, and calm minimalism.

> **Current Stage:** Phase 0 — Foundation Only.

## Phase 0 Scope

- **Design System ("Calm Intelligence")**: Centralized color tokens (light mode default with dark mode architecture), bundled Manrope typography, spacing, shapes, elevation, spring motion tokens, and reusable Compose components.
- **3+1 Floating Navigation**: Custom floating pill (`Home`, `Clean`, `Guard`) paired with a dedicated Electric Blue circular `Agent` button featuring a custom orbital intelligence icon, plus hidden-bar support for focused workflows (`Settings`).
- **Native Launch Experience**: Android `SplashScreen` API (`androidx.core:core-splashscreen`) with branded orbital splash vector.
- **Real Local Telemetry & Preferences**:
  - Genuine internal storage reading via `StorageStatsManager` / `StatFs` (no simulated metrics).
  - Local user preferences persisted via Jetpack `DataStore` (`ThemePreference`, haptic feedback).
- **Honest Foundations**: Unimplemented Autopilot, Smart Scan, and Memory Guard engines are explicitly represented as inactive or planned without fake scans or silent actions.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Navigation**: Navigation Compose (`@Serializable` type-safe routes)
- **Architecture**: Single-activity MVVM with unidirectional `StateFlow`
- **Persistence**: Jetpack DataStore Preferences
- **SDK**: `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- **Application ID**: `com.clenova.app`

## Documentation

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for package structure, extension points, and safety principles.

## Building & Testing

Using a local Gradle installation or Android Studio:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

> Note: `gradle/wrapper/gradle-wrapper.properties` is included. If running via wrapper on a fresh clone without Android Studio, generate the wrapper JAR/scripts once with `gradle wrapper`.
