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

## Cleanup protection landing
Settings > Cleanup protection provides four persisted smart switches, all enabled by default, and protected folders/files rows reserved for subsequent screens. The pure rule evaluator uses a seven-day recent window, conservatively protects unknown metadata when enabled, preserves mandatory originals and personal exclusions regardless of switch values, and always requires approval. It is not yet connected to a real scanner or deletion engine; existing sample galleries continue protecting their originals. No device files are read. CI includes tests for all 16 switch combinations.

## Protected folders
Real Android ACTION_OPEN_DOCUMENT_TREE picker, empty initial state, locally persisted URI exclusions and display metadata, duplicate prevention, removable entries with confirmation and no file deletion. The app reads only selected folder name using a temporary read grant; no persistent file access is required and no write grant is requested. External-storage document IDs have boundary-safe descendant matching. Opaque providers require ancestry resolution in future scanner integration. OS picker restrictions apply. No active scanner/deletion engine yet; saved exclusions are not a filesystem lock. CI includes folder match tests.

## Protected files
Native ACTION_OPEN_DOCUMENT multi-selection, real provider metadata (name, MIME, size when available), local persisted canonical document URI exclusions, duplicate prevention and confirmed exclusion removal. No persistent read or write grant and no file contents read or deleted. Alternate URI aliases, moved files, or provider identity changes need resolution in the future scanner. Actual cleanup enforcement remains pending; saved exclusions are not a filesystem lock. No sample files are inserted.

## Activity history
Settings > Activity history shows a local, initially empty event journal grouped by device-local date, with anchored All activity/Scans/Cleanups/Protection filter. Records successful protection changes and actual interactions with sample recommendations/approved cleanup previews, using real timestamps. Preview completion totals are explicitly simulated, never actual recovered storage. Approval and completion use stable operation IDs to prevent duplicate completion on recreation. No past history is fabricated or backfilled. Activity Details is deferred; rows are read-only in this phase. No real scan events until the scanner exists.

## Activity details
Tap a history row to open a dynamic receipt with recorded timestamp, status, size/count and per-category snapshot for new sample recommendation and cleanup events. Older journal entries without breakdown remain readable and explicitly report missing breakdown. Approval and completion snapshot the same selected item counts and sizes; rotation preserves the in-progress snapshot, selected event, filter and history scroll position. Protection entries show the historical setting change, not current file status or verified deletion protection. All cleanup/scan records remain explicitly preview-only. No data is backfilled or invented.

## App permissions
Settings > App permissions reads Android runtime permission, notification channel and background restriction status. Refreshes on resume and notification permission result. Notification row requests POST_NOTIFICATIONS on Android 13+ or opens notification settings; app-settings shortcut allows revocation. Media and contacts are not requested in this preview, and files use the Android picker, not broad storage access. Background row explains limitations and opens app settings, without requesting battery exemptions. No new manifest permissions or background scanning added.

## Privacy & data
Native privacy page with OS-reported app UID data bytes (including cache), labeled fallback to saved preference-file bytes, and real record/exclusion counts. Explicitly confirmed activity history JSON export through ACTION_CREATE_DOCUMENT; destination may be a user-selected cloud provider. Clear history removes local journal only. Reset clears five known Clenova preference stores and in-memory cleanup previews, leaving personal files, Android permissions, system notification channel settings and exported copies untouched. No broad storage permission or automatic uploads added. Errors are reported without claiming success.
