"""
Heavenys Launcher — constants and color palette.
All hex colors match the in-game HeavenysColors.java palette.
"""

APP_NAME        = "Heavenys Client"
APP_VERSION     = "1.0.0"
MOD_ID          = "heavenys-client"
MC_VERSION      = "1.21.5"
FABRIC_VERSION  = "0.16.14"
MOD_JAR_NAME    = f"heavenys-client-{APP_VERSION}.jar"

# GitHub release URL (placeholder — update when you publish a release)
RELEASES_BASE   = "https://github.com/swiqzulesud-ctrl/heavenys/releases/latest/download"
MOD_DOWNLOAD_URL = f"{RELEASES_BASE}/{MOD_JAR_NAME}"

FABRIC_INSTALLER_URL = (
    "https://maven.fabricmc.net/net/fabricmc/fabric-installer/"
    "1.0.1/fabric-installer-1.0.1.jar"
)

# ── Color palette ──────────────────────────────────────────────────────────────
C_BG            = "#0F0F0F"   # main window background
C_SURFACE       = "#1A1A1A"   # cards, panels
C_SURFACE2      = "#222222"   # hover / slightly lighter
C_BORDER        = "#2E2E2E"   # subtle border
C_YELLOW        = "#F7E78E"   # Butter Yellow — primary accent
C_YELLOW_DARK   = "#D4C060"   # pressed / darker variant
C_YELLOW_DIM    = "#6B5E2A"   # disabled / ghost
C_WHITE         = "#FFFFFF"   # primary text
C_MUTED         = "#AAAAAA"   # secondary text
C_DIMMED        = "#555555"   # disabled text
C_GREEN         = "#66EE88"   # enabled indicator
C_RED           = "#EE6655"   # disabled / error indicator
C_AMBER         = "#FFAA33"   # warning

# Sidebar
SIDEBAR_W       = 200
HEADER_H        = 72

# Window
WIN_W           = 920
WIN_H           = 580
WIN_MIN_W       = 860
WIN_MIN_H       = 520
