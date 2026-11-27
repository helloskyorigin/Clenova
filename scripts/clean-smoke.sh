#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/onboarding-smoke-helper.sh"
mkdir -p build/clean-evidence
adb shell am force-stop com.clenova.app
onboarding_start clean-1
dump() {
  adb shell uiautomator dump /sdcard/clenova-qa.xml >/dev/null
  adb pull /sdcard/clenova-qa.xml "build/clean-evidence/$1.xml" >/dev/null
  adb exec-out screencap -p > "build/clean-evidence/$1.png"
}
tap_text() {
  local xy
  xy="$(python3 - "$1" "$2" <<'PY'
import sys,xml.etree.ElementTree as ET,re
root=ET.parse(sys.argv[1])
nodes=[n for n in root.iter('node') if n.get('text')==sys.argv[2] or n.get('content-desc')==sys.argv[2]]
if not nodes: raise SystemExit('Missing UI element: '+sys.argv[2])
a,b,c,d=map(int,re.findall(r'\d+',nodes[-1].get('bounds')))
print((a+c)//2,(b+d)//2)
PY
)"
  adb shell input tap $xy
  sleep 1
}
dump home
tap_text build/clean-evidence/home.xml Clean
dump clean
grep -F 'Smart Scan' build/clean-evidence/clean.xml
tap_text build/clean-evidence/clean.xml 'Smart Scan'
dump permission
grep -F 'Media access needed' build/clean-evidence/permission.xml
adb shell input keyevent 4
sleep 1
dump clean-return
grep -F 'Smart Scan' build/clean-evidence/clean-return.xml
adb logcat -d -s AndroidRuntime:E > build/clean-evidence/runtime.log
if grep -F 'FATAL EXCEPTION' build/clean-evidence/runtime.log; then exit 1; fi
