#!/usr/bin/env bash
# Temporary Phase 47 navigation: verify the actual onboarding CTA before entering Home.
onboarding_continue() {
  local name="${1:-onboarding}" xy
  mkdir -p build/onboarding-evidence
  adb shell uiautomator dump /sdcard/onboarding-qa.xml >/dev/null
  adb pull /sdcard/onboarding-qa.xml "build/onboarding-evidence/$name.xml" >/dev/null
  adb exec-out screencap -p > "build/onboarding-evidence/$name.png"
  xy="$(python3 - "build/onboarding-evidence/$name.xml" <<'PY'
import sys, re, xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).iter('node'))
assert any('Meet your personal' in n.get('text','') for n in nodes), 'Onboarding heading missing'
assert any('Illustrative Agent session' in n.get('text','') for n in nodes), 'Illustrative label missing'
matches=[n for n in nodes if n.get('text')=='Get Started' and n.get('clickable')=='true' and n.get('enabled')=='true']
assert matches, 'Get Started must be available immediately'
a,b,c,d=map(int,re.findall(r'\d+',matches[0].get('bounds')))
assert c>a and d>b, 'CTA not visible'
print((a+c)//2,(b+d)//2)
PY
)"
  adb shell input tap $xy
  sleep 1
}
