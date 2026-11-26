# CLEAN verification

Production uses real MediaStore results; debug design fixtures are separate. All trash actions require an Android 11+ system request, and must be explicitly confirmed by the user. No direct deletion of originals is performed by the app. Video compression creates a separate copy on Android 10+.

## Automated verification

- Android quality checks: build, localization, unit tests and lint.
- CLEAN Android device smoke tests: fresh API 34 emulator, synthetic images, exact duplicates, similar images, screenshot, large MP4, real Media3 encoding/export, source-size verification and history persistence. The test creates and cancels system trash requests without sending them; it checks the source files remain intact. Only synthetic fixtures are cleaned up.
- The emulator test needs the generated test asset; run through the workflow or generate the fixture using its FFmpeg command before local instrumentation.

## Physical-device acceptance (not covered by creating a PendingIntent)

Use disposable photos/videos, not valuable memories. Verify grant/deny/partial permission, empty gallery, scan cancellation, background/resume, rotate, re-scan, thumbnail failure, favorite protection, exact duplicate keeper, similar photo manual selection, screenshot selection, large media, swipe and undo, system trash confirmation and cancellation, and gallery/Trash after each operation. Verify actual video playback, audio, rotation, sizes, save/cancel, and insufficient storage. Check history persists after restarting and clearing history does not change media. Test multiple device vendors and Android 11–16.

## Scope and limits

- MediaStore only: large photos and videos >=100 MB, not arbitrary documents/ZIP/APK.
- Scans are bounded; incomplete/partial results are indicated. Similarity is heuristic, not an AI model or an identity guarantee.
- Trash may retain bytes temporarily. Compression creates a copy and does not free storage until the user independently removes an original.
- Memory Guard custom rules are not integrated yet. Favorites and protected duplicate/similar keepers are excluded.
