#!/usr/bin/env bash
# Navigate all three introductions and collect actual device evidence.
# Disable motion before each smoke launch so manual CTA checks cannot race automatic advancement.
onboarding_start() {
  local previous
  previous="$(adb shell settings get global animator_duration_scale | tr -d '\r')"
  (
    restore_motion() {
      if [[ "$previous" == "null" || -z "$previous" ]]; then
        adb shell settings delete global animator_duration_scale >/dev/null
      else
        adb shell settings put global animator_duration_scale "$previous" >/dev/null
      fi
    }
    trap restore_motion EXIT
    adb shell settings put global animator_duration_scale 0
    adb shell am start -W -n com.clenova.app/.MainActivity
    sleep 1
    adb shell uiautomator dump /sdcard/onboarding-start.xml >/dev/null
    if adb shell cat /sdcard/onboarding-start.xml | grep -q 'Your storage, at a glance.'; then return 0; fi
    onboarding_continue "$1"
    onboarding_access "$1"
  )
}
onboarding_continue() {
  local name="${1:-onboarding}" xy stage heading cta
  mkdir -p build/onboarding-evidence
  for stage in 01 02 03; do
    case "$stage" in
      01) heading="Meet your personal"; cta="Get Started" ;;
      02) heading="One Agent."; cta="Continue" ;;
      03) heading="On autopilot."; cta="Continue" ;;
    esac
    adb shell uiautomator dump /sdcard/onboarding-qa.xml >/dev/null
    adb pull /sdcard/onboarding-qa.xml "build/onboarding-evidence/$name-$stage.xml" >/dev/null
    adb exec-out screencap -p > "build/onboarding-evidence/$name-$stage.png"
    xy="$(python3 - "build/onboarding-evidence/$name-$stage.xml" "$heading" "$cta" "$stage" <<'PY'
import sys, re, xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).iter('node'))
assert any(sys.argv[2] in n.get('text','') for n in nodes), 'Onboarding heading missing'
assert any(n.get('content-desc') == 'Introduction '+str(int(sys.argv[4]))+' of 3' for n in nodes), 'Story progress missing'
if sys.argv[3]=='Get Started':
    assert any('Ask it to take care of your phone' in n.get('content-desc','') for n in nodes), 'Agent introduction accessibility summary missing'
for n in nodes:
    for attr in ('text','content-desc'):
        assert not re.search(r'\b(demo|sample|test|illustrative)\b', n.get(attr,''),re.I), 'Development label in onboarding'
matches=[n for n in nodes if n.get('text')==sys.argv[3] and n.get('clickable')=='true' and n.get('enabled')=='true']
assert matches, 'CTA must be available immediately'
a,b,c,d=map(int,re.findall(r'\d+',matches[0].get('bounds')))
assert c>a and d>b, 'CTA not visible'
print((a+c)//2,(b+d)//2)
PY
)"
    adb shell input tap $xy
    sleep 1
  done
}

# Follow the real permission setup on the API 34 emulator. No app-only bypass.
onboarding_access() {
  local name="$1" iteration xml="build/onboarding-evidence/$1-access.xml" action
  for iteration in $(seq 1 30); do
    adb shell uiautomator dump /sdcard/onboarding-access.xml >/dev/null
    adb pull /sdcard/onboarding-access.xml "$xml" >/dev/null
    adb exec-out screencap -p > "build/onboarding-evidence/$name-access-$iteration.png"
    action="$(python3 - "$xml" <<'PY'
import sys, re, xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).iter('node'))
texts=[n.get('text','') for n in nodes]
if 'Your storage, at a glance.' in texts:
    print('DONE'); raise SystemExit
def choose(predicate):
    for n in nodes:
        if predicate(n) and n.get('enabled')=='true' and n.get('clickable')=='true':
            a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds','')))
            if c>a and d>b:
                print((a+c)//2,(b+d)//2); return True
    return False
if choose(lambda n:n.get('resource-id','').endswith('/permission_deny_button') or n.get('text','').lower() in ("don't allow","deny")): raise SystemExit
if choose(lambda n:n.get('text')=='Continue without media'): raise SystemExit
if 'App usage' in texts or 'File access' in texts:
    if choose(lambda n:n.get('text')=='Not now'): raise SystemExit
if 'Ready to activate' in texts:
    if choose(lambda n:n.get('text')=='Activate Agent'): raise SystemExit
if choose(lambda n:n.get('text') in ('Allow','Continue')): raise SystemExit
# Repeated denied notifications may lead to Android settings.
if any(n.get('package')=='com.android.settings' for n in nodes):
    print('BACK'); raise SystemExit
print('WAIT')
PY
)"
    case "$action" in DONE) return 0;; BACK) adb shell input keyevent 4;; WAIT) :;; *) adb shell input tap $action;; esac
    sleep 1
  done
  echo "Permission setup did not reach Home" >&2; return 1
}
