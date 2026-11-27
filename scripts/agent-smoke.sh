#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/onboarding-smoke-helper.sh"
mkdir -p build/agent-evidence
adb shell am force-stop com.clenova.app
adb shell am start -W -n com.clenova.app/.MainActivity
sleep 2
onboarding_continue agent-1
dump() {
  adb shell uiautomator dump /sdcard/agent-qa.xml >/dev/null
  adb pull /sdcard/agent-qa.xml "build/agent-evidence/$1.xml" >/dev/null
  adb exec-out screencap -p > "build/agent-evidence/$1.png"
}
tap() {
  local xy
  xy="$(python3 - "$1" "$2" <<'PY'
import sys,xml.etree.ElementTree as ET,re
nodes=[n for n in ET.parse(sys.argv[1]).iter('node') if n.get('text')==sys.argv[2] or n.get('content-desc')==sys.argv[2]]
if not nodes: raise SystemExit('Missing UI element: '+sys.argv[2])
a,b,c,d=map(int,re.findall(r'\d+',nodes[-1].get('bounds')))
print((a+c)//2,(b+d)//2)
PY
)"
  adb shell input tap $xy
  sleep 1
}
dump home
tap build/agent-evidence/home.xml Agent
dump welcome
grep -F 'How can I help?' build/agent-evidence/welcome.xml
tap build/agent-evidence/welcome.xml 'Review my duplicate photos'
dump chat
grep -F 'Review duplicates' build/agent-evidence/chat.xml
grep -F 'Ask Agent...' build/agent-evidence/chat.xml
if grep -F 'content-desc="Tools"' build/agent-evidence/chat.xml; then exit 1; fi
tap build/agent-evidence/chat.xml 'Agent menu'
dump drawer
grep -F 'Temporary conversation' build/agent-evidence/drawer.xml
if grep -F 'Recent conversations' build/agent-evidence/drawer.xml; then exit 1; fi
adb shell input keyevent 4
sleep 1
dump restored
adb shell am force-stop com.clenova.app
adb shell am start -W -n com.clenova.app/.MainActivity
sleep 2
onboarding_continue agent-2
dump relaunched
tap build/agent-evidence/relaunched.xml Agent
dump resumed
grep -F 'Review duplicates' build/agent-evidence/resumed.xml
tap build/agent-evidence/resumed.xml 'New conversation'
dump new
grep -F 'How can I help?' build/agent-evidence/new.xml
adb shell am force-stop com.clenova.app
adb shell am start -W -n com.clenova.app/.MainActivity
sleep 2
onboarding_continue agent-3
dump cleared_home
tap build/agent-evidence/cleared_home.xml Agent
dump cleared
grep -F 'How can I help?' build/agent-evidence/cleared.xml
if grep -F 'Review duplicates' build/agent-evidence/cleared.xml; then exit 1; fi
adb logcat -d -s AndroidRuntime:E > build/agent-evidence/runtime.log
if grep -F 'FATAL EXCEPTION' build/agent-evidence/runtime.log; then exit 1; fi
