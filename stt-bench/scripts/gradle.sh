#!/usr/bin/env bash
set -euo pipefail
GRADLE_VERSION="9.4.1"
if [ ! -x "./gradlew" ]; then
  curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip
  unzip -q /tmp/gradle.zip -d /tmp
  /tmp/gradle-${GRADLE_VERSION}/bin/gradle wrapper --gradle-version "${GRADLE_VERSION}"
fi
./gradlew --no-daemon "$@"
