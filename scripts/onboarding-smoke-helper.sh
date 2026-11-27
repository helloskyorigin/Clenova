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
    onboarding_continue "$1"
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
