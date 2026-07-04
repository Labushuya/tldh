#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 4 ]; then
  echo "Usage: $0 <version> <versionCode> <offlineApk> <updaterApk>" >&2
  exit 2
fi

VERSION="$1"
VERSION_CODE="$2"
OFFLINE_APK="$3"
UPDATER_APK="$4"

python - "$VERSION" "$VERSION_CODE" "$OFFLINE_APK" "$UPDATER_APK" <<'PY'
import hashlib
import json
import os
import pathlib
import re
import sys

version, version_code_raw, offline_raw, updater_raw = sys.argv[1:5]

if not re.fullmatch(r"\d+\.\d+\.\d+", version):
    raise SystemExit(f"Invalid SemVer version: {version!r}")
try:
    version_code = int(version_code_raw)
except ValueError as exc:
    raise SystemExit(f"Invalid versionCode: {version_code_raw!r}") from exc
if version_code <= 0:
    raise SystemExit("versionCode must be positive")

def apk_payload(raw_path: str, kind: str) -> dict:
    path = pathlib.Path(raw_path)
    if not path.is_file():
        raise SystemExit(f"Missing {kind} APK: {path}")
    data = path.read_bytes()
    if not data:
        raise SystemExit(f"{kind} APK is empty: {path}")
    return {
        "name": path.name,
        "sha256": hashlib.sha256(data).hexdigest(),
        "sizeBytes": len(data),
    }

manifest = {
    "schemaVersion": 1,
    "version": version,
    "versionCode": version_code,
    "channel": "stable",
    "yanked": False,
    "minSdk": 28,
    "targetSdk": 36,
    "apk": {
        "offline": apk_payload(offline_raw, "offline"),
        "updater": apk_payload(updater_raw, "updater"),
    },
    "notes": {
        "summary": f"Stable tl;dh release {version}",
    },
}

json.dump(manifest, sys.stdout, indent=2, sort_keys=True)
sys.stdout.write("\n")
PY
