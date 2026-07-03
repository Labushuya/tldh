#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

manifest_path = Path(sys.argv[1] if len(sys.argv) > 1 else "release-manifest.json")
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
errors = []

if not re.fullmatch(r"\d+\.\d+\.\d+", str(manifest.get("version", ""))):
    errors.append("version must be SemVer without leading v")
if manifest.get("channel") != "stable":
    errors.append("channel must be stable")
if manifest.get("yanked") is not False:
    errors.append("yanked must be false")
apk = manifest.get("apk", {})
if not apk.get("name", "").endswith(".apk"):
    errors.append("apk.name must end with .apk")
if not re.fullmatch(r"[a-fA-F0-9]{64}", str(apk.get("sha256", ""))):
    errors.append("apk.sha256 must be a 64-char hex digest")
if int(apk.get("sizeBytes", 0)) <= 0:
    errors.append("apk.sizeBytes must be positive")

if errors:
    for error in errors:
        print(f"ERROR: {error}")
    sys.exit(1)
print("stable release manifest verified")
