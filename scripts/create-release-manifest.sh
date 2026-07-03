#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?version required, e.g. 0.1.0}"
VERSION_CODE="${2:?versionCode required}"
APK_PATH="${3:?apk path required}"
CHANNEL="${4:-stable}"

APK_NAME="$(basename "$APK_PATH")"
SHA256="$(sha256sum "$APK_PATH" | awk '{print $1}')"
SIZE_BYTES="$(stat -c%s "$APK_PATH")"

cat > release-manifest.json <<JSON
{
  "version": "$VERSION",
  "versionCode": $VERSION_CODE,
  "channel": "$CHANNEL",
  "minSdk": 28,
  "targetSdk": 36,
  "apk": {
    "name": "$APK_NAME",
    "sha256": "$SHA256",
    "sizeBytes": $SIZE_BYTES
  },
  "notes": {
    "summary": "tl;dh stable release $VERSION",
    "changelogUrl": "CHANGELOG.md"
  },
  "yanked": false
}
JSON
