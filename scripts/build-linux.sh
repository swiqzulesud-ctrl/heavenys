#!/usr/bin/env bash
# Heavenys Client — Linux/macOS build script.
# Builds the in-game mod jar, the launcher fat jar, and a native app-image.
# Requires: JDK 21 on PATH.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[Heavenys] Building client mod + launcher (with tests)..."
./gradlew :client:build :launcher:test :launcher:fatJar

echo "[Heavenys] Packaging launcher as a native app-image..."
./gradlew :launcher:jpackage

echo "[Heavenys] Done."
echo "  Mod jar:       client/build/libs/"
echo "  Launcher jar:  launcher/build/libs/"
echo "  App image:     launcher/build/jpackage/"
