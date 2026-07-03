#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="9.4.1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_GRADLE="$ROOT_DIR/.gradle-local/gradle-$GRADLE_VERSION/bin/gradle"

if [[ -x "$ROOT_DIR/gradlew" ]]; then
  exec "$ROOT_DIR/gradlew" "$@"
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

mkdir -p "$ROOT_DIR/.gradle-local"
ZIP="$ROOT_DIR/.gradle-local/gradle-$GRADLE_VERSION-bin.zip"
if [[ ! -x "$LOCAL_GRADLE" ]]; then
  echo "Bootstrapping Gradle $GRADLE_VERSION locally..."
  curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  unzip -q "$ZIP" -d "$ROOT_DIR/.gradle-local"
fi

exec "$LOCAL_GRADLE" "$@"
