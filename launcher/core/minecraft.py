"""
Minecraft / Fabric installation detection and mod management.
Works on Windows, macOS, and Linux.
"""
from __future__ import annotations

import json
import os
import platform
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Optional


# ── Minecraft directory detection ─────────────────────────────────────────────

def minecraft_dir() -> Path:
    system = platform.system()
    if system == "Windows":
        return Path(os.environ.get("APPDATA", "~")) / ".minecraft"
    elif system == "Darwin":
        return Path("~/Library/Application Support/minecraft").expanduser()
    else:
        return Path("~/.minecraft").expanduser()


def mods_dir() -> Path:
    return minecraft_dir() / "mods"


def config_dir() -> Path:
    return minecraft_dir() / "config"


def is_minecraft_installed() -> bool:
    return minecraft_dir().exists()


def is_fabric_installed(mc_version: str) -> bool:
    """
    Check whether at least one Fabric version folder exists under versions/.
    Fabric version folders are named like '1.21.5-fabric-<loader>'.
    """
    versions = minecraft_dir() / "versions"
    if not versions.exists():
        return False
    return any(
        d.name.startswith(f"{mc_version}-fabric")
        for d in versions.iterdir()
        if d.is_dir()
    )


# ── Config read / write ───────────────────────────────────────────────────────

_CONFIG_FILE = "heavenys-client.json"

_DEFAULTS: dict = {
    "armorStatusEnabled":  True,
    "potionStatusEnabled": True,
    "keystrokesEnabled":   True,
    "fpsEnabled":          True,
    "pingEnabled":         True,
    "fpsTopRight":         True,
    "armorDurabilityBar":  True,
    "swingSpeedMultiplier": 1.0,
    # positions
    "armorStatusX": 4,  "armorStatusY": 4,
    "potionStatusX": 4, "potionStatusY": 52,
    "keystrokesX": 4,   "keystrokesY": 90,
    "fpsX": 4,          "fpsY": 4,
    "pingX": 4,         "pingY": 14,
}


def load_config() -> dict:
    path = config_dir() / _CONFIG_FILE
    if path.exists():
        try:
            with path.open("r", encoding="utf-8") as f:
                data = json.load(f)
            return {**_DEFAULTS, **data}
        except Exception:
            pass
    return dict(_DEFAULTS)


def save_config(cfg: dict) -> None:
    cfg_dir = config_dir()
    cfg_dir.mkdir(parents=True, exist_ok=True)
    path = cfg_dir / _CONFIG_FILE
    with path.open("w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2)


# ── Mod install / remove ──────────────────────────────────────────────────────

def find_installed_jar(mod_id: str) -> Optional[Path]:
    mods = mods_dir()
    if not mods.exists():
        return None
    for f in mods.iterdir():
        if f.suffix == ".jar" and mod_id in f.name:
            return f
    return None


def install_jar(source: Path) -> Path:
    """Copy a JAR into the mods folder. Returns destination path."""
    dest_dir = mods_dir()
    dest_dir.mkdir(parents=True, exist_ok=True)
    dest = dest_dir / source.name
    shutil.copy2(source, dest)
    return dest


def remove_jar(mod_id: str) -> bool:
    jar = find_installed_jar(mod_id)
    if jar:
        jar.unlink()
        return True
    return False


# ── Minecraft launcher ────────────────────────────────────────────────────────

def launch_minecraft() -> bool:
    """
    Attempt to open the official Minecraft Launcher.
    Returns True if a process was started.
    """
    system = platform.system()
    try:
        if system == "Windows":
            launcher = Path(os.environ.get("PROGRAMFILES(X86)", "C:/Program Files (x86)")) \
                / "Minecraft Launcher" / "MinecraftLauncher.exe"
            if not launcher.exists():
                launcher = Path(os.environ.get("PROGRAMFILES", "C:/Program Files")) \
                    / "Minecraft Launcher" / "MinecraftLauncher.exe"
            if launcher.exists():
                subprocess.Popen([str(launcher)])
                return True
        elif system == "Darwin":
            subprocess.Popen(["open", "-a", "Minecraft"])
            return True
        else:
            for cmd in ("minecraft-launcher", "minecraft"):
                if shutil.which(cmd):
                    subprocess.Popen([cmd])
                    return True
    except Exception:
        pass
    return False


def open_mods_folder() -> None:
    path = mods_dir()
    path.mkdir(parents=True, exist_ok=True)
    system = platform.system()
    if system == "Windows":
        os.startfile(str(path))
    elif system == "Darwin":
        subprocess.Popen(["open", str(path)])
    else:
        subprocess.Popen(["xdg-open", str(path)])
