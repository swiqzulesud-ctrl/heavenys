# -*- mode: python ; coding: utf-8 -*-
# PyInstaller spec for Heavenys Client Launcher
# Build: pyinstaller heavenys.spec
# Output: dist/HeavenysLauncher.exe  (Windows)
#         dist/HeavenysLauncher      (Linux/macOS)

import sys
import os
from PyInstaller.utils.hooks import collect_data_files, collect_submodules

block_cipher = None

# Collect all CustomTkinter theme/asset files
ctk_data = collect_data_files("customtkinter")

# Collect darkdetect (required by customtkinter at runtime)
darkdetect_data = collect_data_files("darkdetect")

a = Analysis(
    ["main.py"],
    pathex=["."],
    binaries=[],
    datas=[
        # CustomTkinter internal assets
        *ctk_data,
        *darkdetect_data,
        # Launcher's own assets directory
        ("assets", "assets"),
    ],
    hiddenimports=[
        "customtkinter",
        "darkdetect",
        "PIL",
        "PIL._tkinter_finder",
        "PIL.Image",
        "PIL.ImageTk",
        "requests",
        "core.constants",
        "core.minecraft",
        "ui.widgets",
        "ui.sidebar",
        "ui.page_home",
        "ui.page_modules",
        "ui.page_settings",
        "ui.page_about",
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=["matplotlib", "numpy", "scipy", "pandas"],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name="HeavenysLauncher",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,           # compress with UPX if available (smaller exe)
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False,      # no black terminal window — GUI-only
    disable_windowed_traceback=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    # Windows: embed version info and icon
    version=None,
    icon="assets/icon.ico" if os.path.exists("assets/icon.ico") else None,
)
