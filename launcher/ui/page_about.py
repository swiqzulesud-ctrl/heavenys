"""
About page — version info, tech stack, and compliance notes.
"""
from __future__ import annotations

import webbrowser
import customtkinter as ctk

from core.constants import *
from ui.widgets import HCard, HButton, HButtonGhost, HSeparator


_STACK = [
    ("Minecraft",       MC_VERSION,     "Target game version"),
    ("Fabric Loader",   FABRIC_VERSION, "Lightweight mod loader"),
    ("Sodium",          "0.6.x",        "GPU-accelerated rendering"),
    ("Iris",            "1.8.x",        "Shader pack support"),
    ("Lithium",         "0.14.x",       "Game logic optimisations"),
    ("Fabric API",      "0.119.x",      "Standard Fabric utilities"),
]

_MODULES_INFO = [
    ("ArmorStatus",   "Armor pieces + durability bars"),
    ("PotionStatus",  "Active effects, duration, amplifier"),
    ("Keystrokes",    "WASD / Space / LMB / RMB"),
    ("FPS Counter",   "FPS with color thresholds"),
    ("Ping Display",  "Latency from TAB-list entry"),
]

_EXCLUDED = [
    "ESP / entity highlighting",
    "Reach extender",
    "Auto-clicker / auto-aim",
    "Kill aura / combat bots",
    "Any server-packet manipulation",
]


class AboutPage(ctk.CTkFrame):

    def __init__(self, master, **kw):
        super().__init__(master, fg_color="transparent", **kw)
        self._build()

    def _build(self):
        scroll = ctk.CTkScrollableFrame(
            self, fg_color="transparent",
            scrollbar_button_color=C_YELLOW_DIM,
            scrollbar_button_hover_color=C_YELLOW,
        )
        scroll.pack(fill="both", expand=True, padx=24, pady=(16, 8))

        # ── Title ─────────────────────────────────────────────────────────────
        ctk.CTkLabel(
            scroll,
            text="✦  Heavenys Client",
            font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"),
            text_color=C_YELLOW, anchor="w",
        ).pack(fill="x", pady=(4, 2))

        ctk.CTkLabel(
            scroll,
            text=f"Version {APP_VERSION}  ·  MIT License  ·  No-Cheat, Compliance-First",
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color=C_MUTED, anchor="w",
        ).pack(fill="x", pady=(0, 14))

        # ── Tech stack ────────────────────────────────────────────────────────
        stack_card = HCard(scroll, title="Technology Stack")
        stack_card.pack(fill="x", pady=(0, 10))

        for name, ver, desc in _STACK:
            row = ctk.CTkFrame(stack_card, fg_color="transparent")
            row.pack(fill="x", padx=14, pady=2)
            ctk.CTkLabel(row, text=f"•  {name}",
                         font=ctk.CTkFont(size=13, weight="bold"),
                         text_color=C_WHITE, width=140, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=ver,
                         font=ctk.CTkFont(family="Consolas", size=12),
                         text_color=C_YELLOW, width=80, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=desc,
                         font=ctk.CTkFont(size=12),
                         text_color=C_MUTED, anchor="w").pack(side="left")

        ctk.CTkFrame(stack_card, height=12, fg_color="transparent").pack()

        # ── HUD Modules ───────────────────────────────────────────────────────
        mod_card = HCard(scroll, title="HUD Modules Included")
        mod_card.pack(fill="x", pady=(0, 10))

        for name, desc in _MODULES_INFO:
            row = ctk.CTkFrame(mod_card, fg_color="transparent")
            row.pack(fill="x", padx=14, pady=2)
            ctk.CTkLabel(row, text=f"✔  {name}",
                         font=ctk.CTkFont(size=13, weight="bold"),
                         text_color=C_GREEN, width=160, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=desc,
                         font=ctk.CTkFont(size=12),
                         text_color=C_MUTED, anchor="w").pack(side="left")

        ctk.CTkFrame(mod_card, height=12, fg_color="transparent").pack()

        # ── Explicitly excluded ───────────────────────────────────────────────
        excl_card = HCard(scroll, title="Explicitly Excluded  (never will be added)")
        excl_card.pack(fill="x", pady=(0, 10))

        for item in _EXCLUDED:
            row = ctk.CTkFrame(excl_card, fg_color="transparent")
            row.pack(fill="x", padx=14, pady=1)
            ctk.CTkLabel(row, text=f"✘  {item}",
                         font=ctk.CTkFont(size=12),
                         text_color=C_RED, anchor="w").pack(fill="x")

        ctk.CTkFrame(excl_card, height=12, fg_color="transparent").pack()

        # ── Compliance note ───────────────────────────────────────────────────
        note_card = HCard(scroll, title="Server Compliance")
        note_card.pack(fill="x", pady=(0, 10))

        ctk.CTkLabel(
            note_card,
            text=(
                "Heavenys Client is designed to be allowed on Hypixel, LifestealSMP, and\n"
                "similar competitive servers. All HUD modules are read-only overlays using\n"
                "only data the vanilla client already has access to.\n\n"
                "Always verify the current 'Allowed Mods' list for any server you join."
            ),
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color=C_MUTED, anchor="w", justify="left",
        ).pack(fill="x", padx=14, pady=(0, 14))

        # ── Links ─────────────────────────────────────────────────────────────
        links = ctk.CTkFrame(scroll, fg_color="transparent")
        links.pack(fill="x", pady=(4, 20))

        HButtonGhost(
            links, text="GitHub",
            command=lambda: webbrowser.open("https://github.com/swiqzulesud-ctrl/heavenys"),
            width=100,
        ).pack(side="left", padx=(0, 8))

        HButtonGhost(
            links, text="Issues / Bug Reports",
            command=lambda: webbrowser.open(
                "https://github.com/swiqzulesud-ctrl/heavenys/issues"),
            width=160,
        ).pack(side="left")

    def refresh(self):
        pass
