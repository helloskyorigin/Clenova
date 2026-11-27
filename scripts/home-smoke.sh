#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/home-evidence
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size 1080x2400
adb shell wm density 420
adb shell am force-stop com.clenova.app
adb shell am start -W -n com.clenova.app/.MainActivity
sleep 2
adb shell uiautomator dump /sdcard/home.xml
adb pull /sdcard/home.xml build/home-evidence/home.xml
adb exec-out screencap -p > build/home-evidence/home.png
grep -F 'Autopilot active' build/home-evidence/home.xml
grep -F 'All under control.' build/home-evidence/home.xml
grep -F 'Agent' build/home-evidence/home.xml
grep -F 'Tools' build/home-evidence/home.xml
# Phase 38: navigate from Home to the Tools landing; check scope and return paths.
tools_dump() {
  adb shell uiautomator dump /sdcard/tools-qa.xml >/dev/null
  adb pull /sdcard/tools-qa.xml "build/home-evidence/$1.xml" >/dev/null
  adb exec-out screencap -p > "build/home-evidence/$1.png"
}
tools_tap() {
  local xy
  xy="$(python3 - "$1" "$2" <<'PY'
import sys, xml.etree.ElementTree as ET, re
nodes=[n for n in ET.parse(sys.argv[1]).iter('node') if n.get('content-desc')==sys.argv[2] and n.get('clickable')=='true']
if not nodes: nodes=[n for n in ET.parse(sys.argv[1]).iter('node') if n.get('text')==sys.argv[2] or n.get('content-desc')==sys.argv[2]]
if not nodes: raise SystemExit('Missing UI element: '+sys.argv[2])
a,b,c,d=map(int,re.findall(r'\d+',nodes[-1].get('bounds')))
print((a+c)//2,(b+d)//2)
PY
)"
  adb shell input tap $xy
  sleep 1
}
tools_tap build/home-evidence/home.xml Tools
tools_dump tools
for label in 'Swipe Photos' 'Video Compressor' 'Photo Compressor' 'Contacts Cleaner' 'App Manager' 'Speaker Cleaner' 'Internet Speed Test' 'Tools, selected'; do
  grep -F "$label" build/home-evidence/tools.xml
done
if grep -F 'Email Cleaner' build/home-evidence/tools.xml; then exit 1; fi
tools_tap build/home-evidence/tools.xml 'Internet Speed Test'
tools_dump tools_pending
grep -F 'No action has been performed.' build/home-evidence/tools_pending.xml
adb shell input keyevent 4
sleep 1
tools_dump tools_restored
tools_tap build/home-evidence/tools_restored.xml Settings
tools_dump tools_settings
tools_tap build/home-evidence/tools_settings.xml 'Back to Tools'
tools_dump tools_returned
grep -F 'Tools, selected' build/home-evidence/tools_returned.xml
adb shell input keyevent 4
sleep 1
tools_dump tools_back_home
grep -F 'Home, selected' build/home-evidence/tools_back_home.xml
adb shell wm size 720x1280
adb shell wm density 320
adb shell am force-stop com.clenova.app
adb shell am start -W -n com.clenova.app/.MainActivity
sleep 2
adb shell uiautomator dump /sdcard/home-compact.xml
adb pull /sdcard/home-compact.xml build/home-evidence/home-compact.xml
adb exec-out screencap -p > build/home-evidence/home-compact.png
grep -F 'Agent' build/home-evidence/home-compact.xml
grep -F 'Tools' build/home-evidence/home-compact.xml
adb shell wm size reset
adb shell wm density reset
