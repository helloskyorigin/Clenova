# CLENOVA Architecture (Phase 0)

## Package Organization

```text
com.example
└── MainActivity.kt                  # Native SplashScreen + edge-to-edge host

com.clenova.app
├── ClenovaApp.kt                    # Root theme, NavHost, and floating 3+1 nav visibility
├── core
│   ├── designsystem                 # Color tokens, Manrope typography, spacing, shapes, motion, reusable UI
│   ├── navigation                   # @Serializable routes, ClenovaNavHost, 3+1 ClenovaFloatingNavBar
│   ├── storage                      # DeviceStorageProvider (StorageStatsManager + StatFs)
│   └── utilities                    # ByteFormatter, HapticFeedbackHelper
├── data
│   ├── preferences                  # DataStorePreferencesRepository
│   └── storage                      # LocalStorageRepository
├── domain
│   ├── model                        # StorageOverview, StorageState, AutopilotStatus, UserPreferences
│   └── repository                   # StorageRepository, PreferencesRepository interfaces
└── feature
    ├── home                         # HomeScreen, HomeViewModel (branding, real storage, inactive Agent)
    ├── clean                        # CleanScreen (planned manual cleaning tools)
    ├── guard                        # GuardScreen (Memory Guard unconfigured foundation)
    ├── agent                        # AgentScreen (dedicated Autopilot workspace)
    └── settings                     # SettingsScreen, SettingsViewModel (preferences, privacy, app info)
```

## Navigation Architecture (3+1)

- **Main Pill**: `Home`, `Clean`, `Guard` with spring-animated selection capsule and saved back-stack state.
- **Orbital Button**: Separate Electric Blue circular button opening the dedicated `Agent` workspace.
- **Focused Workflows**: `Settings` smoothly hides the floating navigation bar and respects system back navigation as well as an explicit top back control.

## Safety & Privacy Guardrails

1. No files are modified or deleted without explicit user approval and Android system confirmation.
2. Storage telemetry is read directly from Android volume APIs; errors degrade honestly to `StorageState.Unavailable`.
3. Autopilot is explicitly represented as `AutopilotStatus.NotConfigured` until Phase 1+ configuration is built.
4. No external analytics, cloud uploads, advertisements, or remote AI APIs are included.

## Planned Extension Points (Phase 1+)

- **MediaStore & Room**: Content indexing, exact/perceptual hashing, and Memory Guard exclusion tables.
- **WorkManager**: Periodic low-overhead storage checks when Autopilot is explicitly enabled by the user.
- **Media3**: Local video preview and on-device compression pipelines.
- **On-device ML & Play Billing**: Local photo quality/similarity scoring and optional pro capabilities.
