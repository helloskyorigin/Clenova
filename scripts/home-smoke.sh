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
