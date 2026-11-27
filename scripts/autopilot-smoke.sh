#!/usr/bin/env bash
# CI emulator only: run a real read-only job, then revoke media access.
set -euo pipefail
pkg=com.clenova.app
job=4410453
mkdir -p build/autopilot-evidence
# Home smoke completes onboarding with Agent enabled and media access denied.
adb shell run-as "$pkg" cat shared_prefs/clenova_preferences.xml | grep -F 'name="agent_autopilot" value="true"'
adb shell pm grant "$pkg" android.permission.READ_MEDIA_IMAGES
adb shell pm grant "$pkg" android.permission.READ_MEDIA_VIDEO
adb shell am start -W -n "$pkg/.MainActivity"
adb shell cmd jobscheduler get-job-state "$pkg" "$job" | tee build/autopilot-evidence/scheduled.txt
adb shell cmd jobscheduler run -f "$pkg" "$job"
# Real MediaStore scan; no fabricated results and no file deletion.
for i in $(seq 1 60); do
  adb shell run-as "$pkg" cat shared_prefs/clenova_autopilot.xml > build/autopilot-evidence/summary.xml 2>/dev/null || true
  if grep -Fq 'name="checked"' build/autopilot-evidence/summary.xml; then break; fi
  sleep 2
done
grep -F 'name="checked"' build/autopilot-evidence/summary.xml
grep -F 'name="eligible" value="7"' build/autopilot-evidence/summary.xml
grep -F 'name="count"' build/autopilot-evidence/summary.xml
# Revoke only the test grants. Relaunch to reconcile and invalidate the old summary.
adb shell pm revoke "$pkg" android.permission.READ_MEDIA_IMAGES
adb shell pm revoke "$pkg" android.permission.READ_MEDIA_VIDEO
adb shell am start -W -n "$pkg/.MainActivity"
adb shell run-as "$pkg" cat shared_prefs/clenova_autopilot.xml > build/autopilot-evidence/revoked.xml
if grep -Fq 'name="checked"' build/autopilot-evidence/revoked.xml; then
  echo "Stale aggregate survived permission revocation" >&2
  exit 1
fi
adb shell cmd jobscheduler get-job-state "$pkg" "$job" > build/autopilot-evidence/revoked-job.txt 2>&1 || true
grep -Ei 'unknown|no such|not found' build/autopilot-evidence/revoked-job.txt
echo "Autopilot OS job, real local aggregate and permission revocation verified"
