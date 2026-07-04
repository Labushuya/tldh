#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?version required, e.g. 0.2.2}"
VERSION_CODE="${2:?versionCode required}"
APK_PATH="${3:?apk path required}"
CHANNEL="${4:-stable}"

python3 - "$VERSION" "$VERSION_CODE" "$APK_PATH" "$CHANNEL" <<'PY'
import hashlib
import json
import os
import sys
from pathlib import Path

version, version_code_raw, apk_path_raw, channel = sys.argv[1:5]
apk_path = Path(apk_path_raw)
if not apk_path.is_file():
    raise SystemExit(f"APK not found: {apk_path}")
try:
    version_code = int(version_code_raw)
except ValueError as exc:
    raise SystemExit(f"versionCode must be an integer: {version_code_raw}") from exc

sha256 = hashlib.sha256(apk_path.read_bytes()).hexdigest()
manifest = {
    "version": version,
    "versionCode": version_code,
    "channel": channel,
    "minSdk": 28,
    "targetSdk": 36,
    "apk": {
        "name": apk_path.name,
        "sha256": sha256,
        "sizeBytes": apk_path.stat().st_size,
    },
    "notes": {
        "summary": f"tl;dh stable release {version}",
        "changelogUrl": "CHANGELOG.md",
    },
    "yanked": False,
}
Path("release-manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
PY
