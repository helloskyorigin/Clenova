# Clenova — Home design phase

Native Android Java prototype. Launches directly into the approved Home screen. Uses sample data only: score 92, 42.8 GB available, 12.4 GB recovered, 4.8 GB proposed recovery. No device permissions or network access. No real scanning, deletion, Agent, billing or other screens yet.

## Build
JDK 17, Android SDK 35, Gradle 8.9 (AGP 8.7.3). Run `./gradlew :app:assembleDebug` from the repository root. Windows: `gradlew.bat :app:assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`. The repository can be imported into Android Studio.

## Visual acceptance
- Light gray background, black header, blue Pro pill, circular Settings.
- No app-drawn status bar; OS status bar hidden in this preview.
- Autopilot status, animated segmented gauge, score/label, last checked text.
- Available / recovered storage summary and segmented storage bar.
- Agent approval card, privacy line and pinned 3+1 navigation.
- Other navigation targets and approval are explicitly preview dialogs; no destructive behavior.
- On shorter screens content scrolls while the bottom navigation remains visible.
- Screen layout based on user-approved screenshots. Device/emulator validation required before claiming pixel-perfect fidelity.

## Scope
Home only. Future screens and real-data integration are deliberately out of scope.
