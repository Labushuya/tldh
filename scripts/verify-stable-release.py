#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

manifest_path = Path(sys.argv[1] if len(sys.argv) > 1 else "release-manifest.json")
if not manifest_path.is_file():
    print(f"ERROR: manifest not found: {manifest_path}")
    sys.exit(1)
raw = manifest_path.read_text(encoding="utf-8").strip()
if not raw:
    print(f"ERROR: manifest is empty: {manifest_path}")
    sys.exit(1)
try:
    manifest = json.loads(raw)
except json.JSONDecodeError as exc:
    print(f"ERROR: manifest is not valid JSON: {exc}")
    sys.exit(1)

errors = []
version = str(manifest.get("version", ""))
if not re.fullmatch(r"\d+\.\d+\.\d+", version):
    errors.append("version must be SemVer without leading v")
if not isinstance(manifest.get("versionCode"), int) or manifest.get("versionCode", 0) <= 0:
    errors.append("versionCode must be a positive integer")
if manifest.get("channel") != "stable":
    errors.append("channel must be stable")
if manifest.get("yanked") is not False:
    errors.append("yanked must be false")
apk = manifest.get("apk", {})
if not isinstance(apk, dict):
    errors.append("apk must be an object")
else:
    expected_name = f"tldh-{version}.apk" if version else None
    if apk.get("name") != expected_name:
        errors.append(f"apk.name must be {expected_name}")
    if not re.fullmatch(r"[a-fA-F0-9]{64}", str(apk.get("sha256", ""))):
        errors.append("apk.sha256 must be a 64-char hex digest")
    try:
        size = int(apk.get("sizeBytes", 0))
    except Exception:
        size = 0
    if size <= 0:
        errors.append("apk.sizeBytes must be positive")

if errors:
    for error in errors:
        print(f"ERROR: {error}")
    sys.exit(1)
print("stable release manifest verified")
