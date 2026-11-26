#!/usr/bin/env python3
"""Fail when translations are missing, extra, duplicated, or have mismatched placeholders."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
TAGS = ["b+zh+Hans","hi","es","ar","fr","bn","pt","ru","ur","in","de","ja","ko","tr"]
def read(path):
    out = {}
    for node in ET.parse(path).getroot().findall("string"):
        name = node.attrib["name"]
        if name in out:
            raise AssertionError(f"{path}: duplicate {name}")
        out[name] = (node.attrib, "".join(node.itertext()))
    return out

def placeholders(text):
    return sorted(re.findall(r"%\d+\$[sd]|%%", text))

base = read(ROOT / "values/strings.xml")
expected = {k: v for k, v in base.items() if v[0].get("translatable") != "false"}
for tag in TAGS:
    path = ROOT / f"values-{tag}/strings.xml"
    translated = read(path)
    assert translated.keys() == expected.keys(), (tag, expected.keys() - translated.keys(), translated.keys() - expected.keys())
    for key, (_, text) in translated.items():
        assert text.strip(), (tag, key, "empty translation")
        assert placeholders(text) == placeholders(expected[key][1]), (tag, key, "placeholder mismatch")
print(f"{len(TAGS)+1} languages; {len(expected)} translatable keys per locale; {len(expected)*len(TAGS)} additional-language entries verified.")
