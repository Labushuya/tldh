#!/usr/bin/env python3
import json
import pathlib
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: verify-stable-release.py <release-manifest.json>")

manifest_path = pathlib.Path(sys.argv[1])
if not manifest_path.is_file():
    raise SystemExit(f"Manifest file does not exist: {manifest_path}")
text = manifest_path.read_text(encoding="utf-8").strip()
if not text:
    raise SystemExit(f"Manifest file is empty: {manifest_path}")

try:
    manifest = json.loads(text)
except json.JSONDecodeError as exc:
    raise SystemExit(f"Manifest is not valid JSON at line {exc.lineno}, column {exc.colno}: {exc.msg}") from exc

errors: list[str] = []

def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)

version = manifest.get("version")
require(isinstance(version, str) and re.fullmatch(r"\d+\.\d+\.\d+", version or "") is not None, "version must be SemVer X.Y.Z")
version_code = manifest.get("versionCode")
require(isinstance(version_code, int) and version_code > 0, "versionCode must be a positive integer")
require(manifest.get("channel") == "stable", "channel must be stable")
require(manifest.get("yanked") is False, "yanked must be false for stable releases")
require(isinstance(manifest.get("schemaVersion"), int), "schemaVersion must be an integer")

apk = manifest.get("apk")
require(isinstance(apk, dict), "apk must be an object")
if isinstance(apk, dict):
    for flavor in ("offline", "updater"):
        entry = apk.get(flavor)
        require(isinstance(entry, dict), f"apk.{flavor} must be an object")
        if isinstance(entry, dict):
            name = entry.get("name")
            sha = entry.get("sha256")
            size = entry.get("sizeBytes")
            require(isinstance(name, str) and name.endswith(".apk"), f"apk.{flavor}.name must be an APK filename")
            require(isinstance(sha, str) and re.fullmatch(r"[a-fA-F0-9]{64}", sha or "") is not None, f"apk.{flavor}.sha256 must be a 64-char SHA256")
            require(isinstance(size, int) and size > 0, f"apk.{flavor}.sizeBytes must be positive")

if errors:
    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    raise SystemExit(1)

print(f"Stable manifest OK: v{version} ({version_code})")
