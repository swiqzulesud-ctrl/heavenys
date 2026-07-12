"""
Modules page — toggle each HUD module and preview descriptions.
Reads/writes directly to the heavenys-client.json config file.
"""
from __future__ import annotations

import customtkinter as ctk

from core.constants import *
from core import minecraft as mc
from ui.widgets import HCard, HLabel, HLabelMuted, HToggleRow, HSeparator, HButton


_MODULES = [
    {
        "key":   "armorStatusEnabled",
        "label": "Armor Status",
        "desc":  "Shows equipped armor pieces with optional durability bars.",
        "safe":  "Read-only — getEquippedStack()",
    },
    {
        "key":   "potionStatusEnabled",
        "label": "Potion Status",
        "desc":  "Active effects, duration timer, and amplifier level.",
        "safe":  "Read-only — getStatusEffects()",
    },
    {
        "key":   "keystrokesEnabled",
        "label": "Keystrokes",
        "desc":  "WASD · Space · LMB · RMB highlight overlay.",
        "safe":  "Read-only — KeyBinding.isPressed()",
    },
    {
        "key":   "fpsEnabled",
        "label": "FPS Counter",
        "desc":  "Current FPS with green/yellow/red threshold coloring.",
        "safe":  "Read-only — getCurrentFps()",
    },
    {
        "key":   "pingEnabled",
        "label": "Ping Display",
        "desc":  "Server latency in ms from the TAB-list entry.",
        "safe":  "Read-only — PlayerListEntry.getLatency()",
    },
]


class ModulesPage(ctk.CTkFrame):

    def __init__(self, master, **kw):
        super().__init__(master, fg_color="transparent", **kw)
        self._config = mc.load_config()
        self._rows: dict[str, HToggleRow] = {}
        self._build()

    def _build(self):
        # Header
        ctk.CTkLabel(
            self,
            text="HUD Modules",
            font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"),
            text_color=C_WHITE,
            anchor="w",
        ).pack(fill="x", padx=24, pady=(22, 2))

        ctk.CTkLabel(
            self,
            text="Toggle modules here or press  Right Shift  in-game.\n"
                 "All modules are read-only — no unfair advantages.",
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color=C_MUTED,
            anchor="w",
            justify="left",
        ).pack(fill="x", padx=24, pady=(0, 14))

        # Module list
        card = HCard(self)
        card.pack(fill="both", expand=True, padx=24, pady=(0, 12))

        scroll = ctk.CTkScrollableFrame(
            card, fg_color="transparent",
            scrollbar_button_color=C_YELLOW_DIM,
            scrollbar_button_hover_color=C_YELLOW,
        )
        scroll.pack(fill="both", expand=True, padx=8, pady=8)

        for i, mod in enumerate(_MODULES):
            if i > 0:
                HSeparator(scroll).pack(fill="x", padx=4, pady=4)

            row_frame = ctk.CTkFrame(scroll, fg_color="transparent")
            row_frame.pack(fill="x", padx=8, pady=6)

            # Compliance tag
            tag_frame = ctk.CTkFrame(row_frame, fg_color="transparent")
            tag_frame.pack(fill="x")

            key = mod["key"]
            toggle = HToggleRow(
                tag_frame,
                label=mod["label"],
                description=mod["desc"],
                initial=self._config.get(key, True),
                on_change=lambda val, k=key: self._on_toggle(k, val),
            )
            toggle.pack(fill="x")
            self._rows[key] = toggle

            ctk.CTkLabel(
                tag_frame,
                text=f"  ✔  {mod['safe']}",
                font=ctk.CTkFont(family="Segoe UI", size=10),
                text_color=C_GREEN,
                anchor="w",
            ).pack(fill="x", padx=(24, 0))

        # Bottom action bar
        bar = ctk.CTkFrame(self, fg_color="transparent")
        bar.pack(fill="x", padx=24, pady=(0, 20))

        HButton(bar, text="Save Module Settings", command=self._save).pack(side="left")

        ctk.CTkLabel(
            bar,
            text="Changes take effect after restarting Minecraft.",
            font=ctk.CTkFont(family="Segoe UI", size=11),
            text_color=C_MUTED,
        ).pack(side="left", padx=14)

    def _on_toggle(self, key: str, value: bool):
        self._config[key] = value

    def _save(self):
        mc.save_config(self._config)

    def refresh(self):
        self._config = mc.load_config()
        for key, row in self._rows.items():
            row.set(self._config.get(key, True))
