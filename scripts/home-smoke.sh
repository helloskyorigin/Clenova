#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/onboarding-smoke-helper.sh"
mkdir -p build/home-evidence
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell wm size 1080x2400
adb shell wm density 420
adb shell am force-stop com.clenova.app
adb shell am start -W -n com.clenova.app/.MainActivity
sleep 2
onboarding_continue home-1
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
tools_tap build/home-evidence/tools.xml 'Swipe Photos, Keep what matters'
tools_dump swipe_entry
grep -F 'Swipe Photos' build/home-evidence/swipe_entry.xml
if grep -F 'Allow photo access' build/home-evidence/swipe_entry.xml; then
  # Permission is intentionally not requested until the user taps Allow.
  grep -F 'Nothing is uploaded or removed automatically.' build/home-evidence/swipe_entry.xml
fi
adb shell input keyevent 4
sleep 1
tools_dump tools_after_swipe
grep -F 'Tools, selected' build/home-evidence/tools_after_swipe.xml
tools_tap build/home-evidence/tools_after_swipe.xml 'Video Compressor, Reduce video size'
tools_dump video_compressor
grep -F 'Video Compressor' build/home-evidence/video_compressor.xml
grep -F 'Browse files' build/home-evidence/video_compressor.xml
grep -F 'Compression history' build/home-evidence/video_compressor.xml
tools_tap build/home-evidence/video_compressor.xml 'Compression history'
tools_dump video_history
grep -F 'Compression history' build/home-evidence/video_history.xml
adb shell input keyevent 4
sleep 1
tools_dump video_gallery_return
grep -F 'Browse files' build/home-evidence/video_gallery_return.xml
adb shell input keyevent 4
sleep 1
tools_dump tools_after_video
grep -F 'Tools, selected' build/home-evidence/tools_after_video.xml
tools_tap build/home-evidence/tools_after_video.xml 'Photo Compressor, Save space effortlessly'
tools_dump photo_compressor
grep -F 'Photo Compressor' build/home-evidence/photo_compressor.xml
grep -F '0 / 100 photos selected' build/home-evidence/photo_compressor.xml
grep -F 'Photo compression history' build/home-evidence/photo_compressor.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
nodes=list(ET.parse('build/home-evidence/photo_compressor.xml').iter('node'))
assert any(n.get('text','').startswith('Continue') and n.get('enabled')=='false' for n in nodes), 'Empty selection must not continue'
PY
tools_tap build/home-evidence/photo_compressor.xml 'Photo compression history'
tools_dump photo_history
grep -F 'Compression history' build/home-evidence/photo_history.xml
adb shell input keyevent 4
sleep 1
tools_dump photo_gallery_return
grep -F 'Photo Compressor' build/home-evidence/photo_gallery_return.xml
adb shell input keyevent 4
sleep 1
tools_dump tools_after_photo
grep -F 'Tools, selected' build/home-evidence/tools_after_photo.xml
tools_tap build/home-evidence/tools_after_photo.xml 'Contacts Cleaner'
tools_dump contacts_cleaner
grep -F 'Contacts Cleaner' build/home-evidence/contacts_cleaner.xml
grep -F 'Contacts history' build/home-evidence/contacts_cleaner.xml
if grep -F 'Allow contact access' build/home-evidence/contacts_cleaner.xml; then
  grep -F 'Nothing is changed or uploaded during scanning.' build/home-evidence/contacts_cleaner.xml
fi
tools_tap build/home-evidence/contacts_cleaner.xml 'Contacts history'
tools_dump contacts_history
grep -F 'Contacts history' build/home-evidence/contacts_history.xml
adb shell input keyevent 4
sleep 1
tools_dump contacts_return
grep -F 'Contacts Cleaner' build/home-evidence/contacts_return.xml
adb shell input keyevent 4
sleep 1
tools_dump tools_after_contacts
grep -F 'Tools, selected' build/home-evidence/tools_after_contacts.xml
tools_tap build/home-evidence/tools_after_contacts.xml 'App Manager'
tools_dump app_manager
grep -F 'App Manager' build/home-evidence/app_manager.xml
grep -F 'Search apps' build/home-evidence/app_manager.xml
grep -F 'All apps' build/home-evidence/app_manager.xml
grep -F 'Refresh apps' build/home-evidence/app_manager.xml
# No uninstall is requested by automated navigation checks.
adb shell input keyevent 4
sleep 1
tools_dump tools_after_app_manager
grep -F 'Tools, selected' build/home-evidence/tools_after_app_manager.xml
tools_tap build/home-evidence/tools_after_app_manager.xml 'Speaker Cleaner'
tools_dump speaker_cleaner
for label in 'Speaker Cleaner' 'Quick clean' 'Deep clean' 'Start cleaning'; do
  grep -F "$label" build/home-evidence/speaker_cleaner.xml
done
tools_tap build/home-evidence/speaker_cleaner.xml 'Deep clean, 30-second sound cycle'
tools_dump speaker_deep
grep -F 'Deep clean, 30-second sound cycle, selected' build/home-evidence/speaker_deep.xml
tools_tap build/home-evidence/speaker_deep.xml 'Speaker cleaner information'
tools_dump speaker_info
grep -F 'About speaker cleaning' build/home-evidence/speaker_info.xml
adb shell input keyevent 4
sleep 1
# Navigation smoke checks intentionally do not emit sound.
adb shell input keyevent 4
sleep 1
tools_dump tools_after_speaker
grep -F 'Tools, selected' build/home-evidence/tools_after_speaker.xml
tools_tap build/home-evidence/tools_after_speaker.xml 'Internet Speed Test'
tools_dump internet_speed
for label in 'Internet Speed Test' 'GO, start speed test' 'DOWNLOAD' 'UPLOAD' 'Speed history'; do
  grep -F "$label" build/home-evidence/internet_speed.xml
done
tools_tap build/home-evidence/internet_speed.xml 'Speed history'
tools_dump speed_history
grep -F 'Speed history' build/home-evidence/speed_history.xml
adb shell input keyevent 4
sleep 1
tools_dump speed_return
grep -F 'Internet Speed Test' build/home-evidence/speed_return.xml
# Navigation QA deliberately does not generate network test traffic.
adb shell input keyevent 4
sleep 1
tools_dump tools_restored
grep -F 'Tools, selected' build/home-evidence/tools_restored.xml
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
onboarding_continue home-2
adb shell uiautomator dump /sdcard/home-compact.xml
adb pull /sdcard/home-compact.xml build/home-evidence/home-compact.xml
adb exec-out screencap -p > build/home-evidence/home-compact.png
grep -F 'Agent' build/home-evidence/home-compact.xml
grep -F 'Tools' build/home-evidence/home-compact.xml
adb shell wm size reset
adb shell wm density reset
