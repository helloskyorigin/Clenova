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

## Review cleanup preview
Home → Review & Approve opens a dedicated screen (no bottom navbar). Exact duplicates selected initially; other categories show a sample review dialog before selection. The selected total and approval label update together. Zero selection disables approval. Back and Not now return to Home. Confirmation is simulated; individual file review and completion screens are deferred. No Pro or billing changes.

## Duplicate photos preview
Review Cleanup → Duplicate photos → Review items. Agent Picks provides a compact overview; View All shows all 24 groups, 72 sample thumbnails, 48 selectable copies and 24 protected originals. Each copy is 25 decimal MB. Selections are shared across tabs, with exact totals; Confirm selection commits the draft to the cleanup plan, Back discards unconfirmed changes. Sample images are procedural illustrations, not device photos. Full-size sample preview opens on image tap. No media permission or deletion. Other categories remain placeholders.

Pure Java selection check: `javac -d /tmp/clenova-tests app/src/main/java/com/clenova/app/ReviewPlan.java tests/ReviewPlanTest.java && java -cp /tmp/clenova-tests com.clenova.app.ReviewPlanTest`.

## Similar photos preview
Agent Picks uses inline statistics and a single group preview. View All shows 32 groups (16 groups of four and 16 of three), with 32 protected best shots and 80 selectable sample photos at 12.5 MB each. Selection drafts update both tabs, are committed only on Confirm selection, and are discarded on Back. All totals use exact half-MB sample increments. No device media is accessed or removed.

## Large videos preview
Agent Picks shows four largest videos; View All shows all 12 sorted by size (2600 MB total). Tiles display illustrative thumbnails, sample durations and sizes. Full-screen preview displays a sample still, not playback, because no video assets or device media are accessed. Selection is shared across tabs and added to the review plan only on Confirm selection. Back discards drafts. Pure Java CI test covers all 4096 video selection masks.

## Cleanup progress and completion preview
Explicit approval snapshots only selected sample items. A 2.8-second simulated progress ring changes to a green success ring and tick. Category statuses and receipt use the snapshot; home storage, recovered total and agent update reflect the completed preview. One simulated cleanup per activity session; summary remains accessible and the same sample items cannot be processed twice. Unselected items remain untouched; no new scan is simulated. State is saved across activity recreation. No device file access or actual deletion. CI includes snapshot tests.

## Storage Details
Tap Home's storage card to open Storage. Shared sample data drives Home and Storage; the six-part bar includes available space and five storage categories. Only approved photo/video amounts reduce their respective categories after simulated cleanup. Back returns to Home; navigation is restored on activity recreation. No scan or cleaning button, no real storage access. CI includes initial, full and partial storage reconciliation checks.

## Settings landing preview
Home gear opens the scrollable Settings screen with five groups and fourteen rows, native blue line icons and a centered Clenova footer. System appearance and English are illustrative labels. Rows are intentionally non-interactive until their detail screens are implemented. Footer 1.0.0 is approved design placeholder; actual preview build version remains unchanged. Back and system Back return Home; landing state restores on activity recreation. No new account, billing, privacy or permission functionality.

## Preferences popups
Native focusable PopupWindow overlays the tapped row without changing Settings layout. Appearance (Light/Dark/System) updates app surfaces and persists locally; app-local language selector translates common existing screen labels into English, Hindi, Spanish, French or German. Some dynamic sample text and accessibility descriptions remain English. Notification switches persist and create Android channels; enabling requests notification permission on Android 13+, and blocked permission links to Android settings. No automated notifications are emitted while Agent/storage remain simulated. Other Settings detail screens remain unimplemented.

## Agent preferences
Settings > Agent preferences has persisted autopilot, daily/3-day/weekly interval in an anchored popup, and four discovery toggles. Home and the sample cleanup plan respect autopilot and the three implemented sample categories. Unnecessary-files discovery is saved but has no sample scanner yet. Scan interval is a saved preference, not an active scheduled job. Approval is mandatory, no deletion or real scans occur. Existing theme and translated labels are reused.
