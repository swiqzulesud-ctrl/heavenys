"""
Home page — status overview + launch controls.
"""
from __future__ import annotations

import threading
import webbrowser
from pathlib import Path

import customtkinter as ctk

from core import constants as C
from core.constants import *
from core import minecraft as mc
from ui.widgets import (
    HCard, HButton, HButtonGhost, HLabel, HLabelMuted,
    HSeparator, HStatusBadge,
)


class HomePage(ctk.CTkFrame):

    def __init__(self, master, **kw):
        super().__init__(master, fg_color="transparent", **kw)
        self._build()

    def _build(self):
        # ── Top hero banner ───────────────────────────────────────────────────
        banner = ctk.CTkFrame(self, fg_color=C_SURFACE, corner_radius=12,
                              border_width=1, border_color=C_BORDER)
        banner.pack(fill="x", padx=24, pady=(20, 0))

        ctk.CTkLabel(
            banner,
            text="✦  Heavenys Client",
            font=ctk.CTkFont(family="Segoe UI", size=28, weight="bold"),
            text_color=C_YELLOW,
            anchor="w",
        ).pack(fill="x", padx=20, pady=(18, 2))

        ctk.CTkLabel(
            banner,
            text=f"Version {APP_VERSION}  ·  Minecraft {MC_VERSION}  ·  Fabric {FABRIC_VERSION}",
            font=ctk.CTkFont(family="Segoe UI", size=13),
            text_color=C_MUTED,
            anchor="w",
        ).pack(fill="x", padx=20, pady=(0, 18))

        # ── Status row ────────────────────────────────────────────────────────
        status_card = HCard(self, title="Installation Status")
        status_card.pack(fill="x", padx=24, pady=(14, 0))

        grid = ctk.CTkFrame(status_card, fg_color="transparent")
        grid.pack(fill="x", padx=14, pady=(4, 14))
        grid.columnconfigure((0, 1, 2, 3), weight=1)

        self._badge_mc     = self._status_column(grid, "Minecraft",     0)
        self._badge_fabric = self._status_column(grid, "Fabric Loader", 1)
        self._badge_mod    = self._status_column(grid, "Heavenys Mod",  2)
        self._badge_sodium = self._status_column(grid, "Sodium/Iris",   3)

        # ── Action buttons ────────────────────────────────────────────────────
        btn_row = ctk.CTkFrame(self, fg_color="transparent")
        btn_row.pack(fill="x", padx=24, pady=(18, 0))

        HButton(
            btn_row, text="▶  Launch Minecraft",
            command=self._launch,
        ).pack(side="left", padx=(0, 10))

        HButtonGhost(
            btn_row, text="⬇  Install / Update Mod",
            command=self._install,
        ).pack(side="left", padx=(0, 10))

        HButtonGhost(
            btn_row, text="📁  Open Mods Folder",
            command=mc.open_mods_folder,
        ).pack(side="left")

        # ── Log / console strip ───────────────────────────────────────────────
        log_card = HCard(self, title="Log")
        log_card.pack(fill="both", expand=True, padx=24, pady=(14, 20))

        self._log = ctk.CTkTextbox(
            log_card,
            fg_color=C_BG,
            text_color=C_MUTED,
            font=ctk.CTkFont(family="Consolas", size=12),
            corner_radius=6,
            border_width=0,
            state="disabled",
            wrap="word",
        )
        self._log.pack(fill="both", expand=True, padx=12, pady=(0, 12))

        # populate status badges after the widget tree is up
        self.after(50, self._refresh_status)

    # ── Status helpers ────────────────────────────────────────────────────────

    def _status_column(self, parent, title: str, col: int) -> HStatusBadge:
        frame = ctk.CTkFrame(parent, fg_color="transparent")
        frame.grid(row=0, column=col, padx=8, pady=4, sticky="w")
        ctk.CTkLabel(
            frame, text=title,
            font=ctk.CTkFont(family="Segoe UI", size=11),
            text_color=C_MUTED, anchor="w",
        ).pack(fill="x")
        badge = HStatusBadge(frame, text="Checking…", state="neutral")
        badge.pack(anchor="w", pady=(2, 0))
        return badge

    def _refresh_status(self):
        def run():
            mc_ok = mc.is_minecraft_installed()
            self._badge_mc.update_state(
                "Installed" if mc_ok else "Not Found",
                "ok" if mc_ok else "error",
            )
            fabric_ok = mc.is_fabric_installed(MC_VERSION)
            self._badge_fabric.update_state(
                "Installed" if fabric_ok else "Not Found",
                "ok" if fabric_ok else "warn",
            )
            mod_jar = mc.find_installed_jar(MOD_ID)
            self._badge_mod.update_state(
                "Installed" if mod_jar else "Not Installed",
                "ok" if mod_jar else "warn",
            )
            # Sodium check — look for any jar with "sodium" in name
            sodium_ok = mc.find_installed_jar("sodium") is not None
            self._badge_sodium.update_state(
                "Present" if sodium_ok else "Not Found",
                "ok" if sodium_ok else "warn",
            )

        threading.Thread(target=run, daemon=True).start()

    # ── Actions ───────────────────────────────────────────────────────────────

    def _log_line(self, text: str):
        self._log.configure(state="normal")
        self._log.insert("end", text + "\n")
        self._log.see("end")
        self._log.configure(state="disabled")

    def _launch(self):
        self._log_line("Launching Minecraft…")
        ok = mc.launch_minecraft()
        if ok:
            self._log_line("✔  Launcher started.")
        else:
            self._log_line("✘  Could not find Minecraft launcher. Open it manually.")

    def _install(self):
        self._log_line("Scanning for built JAR…")

        def run():
            # Look for a locally built jar first
            candidates = list(Path(".").rglob(f"*{MOD_ID}*.jar")) + \
                         list(Path("..").rglob(f"*{MOD_ID}*.jar"))
            # Filter out sources jars
            candidates = [p for p in candidates if "sources" not in p.name]

            if candidates:
                jar = sorted(candidates, key=lambda p: p.stat().st_mtime, reverse=True)[0]
                self.after(0, lambda: self._log_line(f"Found JAR: {jar.name}"))
                dest = mc.install_jar(jar)
                self.after(0, lambda: self._log_line(f"✔  Installed to: {dest}"))
            else:
                self.after(0, lambda: self._log_line(
                    "No local JAR found. Build with: ./gradlew build\n"
                    f"Then re-run Install, or place the JAR manually in:\n  {mc.mods_dir()}"
                ))
            self.after(100, self._refresh_status)

        threading.Thread(target=run, daemon=True).start()

    def refresh(self):
        self._refresh_status()
