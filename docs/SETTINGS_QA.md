# Settings QA

Source-level checks: actual preference repository writes and reset, locale selection, dynamic runtime permissions, user-controlled email drafts, offline articles, privacy/terms/about, OSS SDK and font attribution. Preference write exceptions are shown instead of being uncaught. Directional Settings arrows mirror in RTL. Support message drafts are not stored in saved instance state; clipboard content is marked sensitive. Smart Scan's debug rescan restarts.

Run from the repository root:
- python3 tools/check_localization.py
- ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --stacktrace

Manual validation: install debug APK; exercise every Settings row; switch each locale and restart; check Arabic and Urdu layout and large font sizes; verify theme persists, reset retains locale, permission status refreshes after Android settings, support composer with and without diagnostics, no-mail-app fallback, and library/font licenses. Confirm that no personal file changes. Inspect release merged manifest and privacy/legal notices before distribution.

The source audit is not a completed Android build, device test, legal review, or native-speaker review.
