#!/usr/bin/env bash
# Heavenys Client Launcher — Linux / macOS build script
# Requirements: Python 3.11+, pip install -r requirements.txt
# Output: dist/HeavenysLauncher (Linux) or dist/HeavenysLauncher (macOS .app via spec tweak)

set -e

echo "============================================"
echo " Heavenys Client Launcher — Build Script"
echo "============================================"
echo

# Install / verify Python deps
pip install -r requirements.txt --quiet

# Clean previous outputs
rm -rf build dist

# Build
python -m PyInstaller heavenys.spec --noconfirm

echo
echo "[OK] Build complete: dist/HeavenysLauncher"
