"""
Settings page — visual tweaks, animation speed, HUD positions.
"""
from __future__ import annotations

import customtkinter as ctk

from core.constants import *
from core import minecraft as mc
from ui.widgets import (
    HCard, HButton, HButtonGhost, HLabel, HLabelMuted,
    HSeparator, HSliderRow, HToggleRow,
)


class SettingsPage(ctk.CTkFrame):

    def __init__(self, master, **kw):
        super().__init__(master, fg_color="transparent", **kw)
        self._config = mc.load_config()
        self._sliders: dict[str, HSliderRow] = {}
        self._toggles: dict[str, HToggleRow] = {}
        self._build()

    def _build(self):
        ctk.CTkLabel(
            self,
            text="Settings",
            font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"),
            text_color=C_WHITE, anchor="w",
        ).pack(fill="x", padx=24, pady=(22, 14))

        scroll = ctk.CTkScrollableFrame(
            self, fg_color="transparent",
            scrollbar_button_color=C_YELLOW_DIM,
            scrollbar_button_hover_color=C_YELLOW,
        )
        scroll.pack(fill="both", expand=True, padx=24, pady=(0, 12))

        # ── Animation ─────────────────────────────────────────────────────────
        anim_card = HCard(scroll, title="Combat Animation")
        anim_card.pack(fill="x", pady=(0, 12))

        ctk.CTkLabel(
            anim_card,
            text="Swing Speed Multiplier — 1.0 is default. Higher values give a snappier\n"
                 "1.7-style feel. Attack timing and server hitboxes are NOT affected.",
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color=C_MUTED, anchor="w", justify="left",
        ).pack(fill="x", padx=14, pady=(0, 10))

        swing = HSliderRow(
            anim_card, label="Swing Speed Multiplier",
            from_=0.5, to=2.0,
            initial=self._config.get("swingSpeedMultiplier", 1.0),
            fmt="{:.2f}x",
            on_change=lambda v: self._config.update({"swingSpeedMultiplier": round(v, 2)}),
        )
        swing.pack(fill="x", padx=14, pady=(0, 14))
        self._sliders["swingSpeedMultiplier"] = swing

        # ── HUD Visual Options ────────────────────────────────────────────────
        hud_card = HCard(scroll, title="HUD Display")
        hud_card.pack(fill="x", pady=(0, 12))

        fps_top = HToggleRow(
            hud_card,
            label="FPS / Ping — Anchor to Top-Right",
            description="When ON, FPS and Ping modules stack in the top-right corner.",
            initial=self._config.get("fpsTopRight", True),
            on_change=lambda v: self._config.update({"fpsTopRight": v}),
        )
        fps_top.pack(fill="x", padx=14, pady=(4, 4))
        self._toggles["fpsTopRight"] = fps_top

        HSeparator(hud_card).pack(fill="x", padx=14, pady=2)

        dur_bar = HToggleRow(
            hud_card,
            label="Armor Durability Bars",
            description="Show a color-coded durability bar beneath each armor icon.",
            initial=self._config.get("armorDurabilityBar", True),
            on_change=lambda v: self._config.update({"armorDurabilityBar": v}),
        )
        dur_bar.pack(fill="x", padx=14, pady=(4, 14))
        self._toggles["armorDurabilityBar"] = dur_bar

        # ── HUD Positions (text entry grid) ────────────────────────────────────
        pos_card = HCard(scroll, title="HUD Module Positions  (pixels from top-left)")
        pos_card.pack(fill="x", pady=(0, 12))

        positions = [
            ("Armor Status",  "armorStatusX",  "armorStatusY"),
            ("Potion Status", "potionStatusX", "potionStatusY"),
            ("Keystrokes",    "keystrokesX",   "keystrokesY"),
        ]

        grid = ctk.CTkFrame(pos_card, fg_color="transparent")
        grid.pack(fill="x", padx=14, pady=(0, 14))
        grid.columnconfigure((1, 2), weight=1)

        self._pos_entries: dict[str, ctk.CTkEntry] = {}

        for row_i, (name, xk, yk) in enumerate(positions):
            ctk.CTkLabel(
                grid, text=name,
                font=ctk.CTkFont(family="Segoe UI", size=12),
                text_color=C_MUTED, anchor="w",
            ).grid(row=row_i, column=0, padx=(0, 12), pady=4, sticky="w")

            for col_i, (key, prefix) in enumerate([(xk, "X: "), (yk, "Y: ")]):
                frame = ctk.CTkFrame(grid, fg_color="transparent")
                frame.grid(row=row_i, column=col_i + 1, padx=4, pady=4, sticky="ew")
                ctk.CTkLabel(frame, text=prefix,
                             font=ctk.CTkFont(size=12), text_color=C_MUTED,
                             width=22).pack(side="left")
                entry = ctk.CTkEntry(
                    frame, width=56,
                    fg_color=C_BG,
                    border_color=C_BORDER,
                    text_color=C_WHITE,
                    font=ctk.CTkFont(family="Consolas", size=12),
                )
                entry.insert(0, str(self._config.get(key, 4)))
                entry.pack(side="left")
                self._pos_entries[key] = entry

        # ── Save ──────────────────────────────────────────────────────────────
        bar = ctk.CTkFrame(self, fg_color="transparent")
        bar.pack(fill="x", padx=24, pady=(0, 20))
        HButton(bar, text="Save Settings", command=self._save).pack(side="left")
        HButtonGhost(bar, text="Reset to Defaults", command=self._reset).pack(side="left", padx=10)

        ctk.CTkLabel(
            bar,
            text="Changes take effect after restarting Minecraft.",
            font=ctk.CTkFont(family="Segoe UI", size=11),
            text_color=C_MUTED,
        ).pack(side="left", padx=14)

    def _save(self):
        for key, entry in self._pos_entries.items():
            try:
                self._config[key] = int(entry.get())
            except ValueError:
                pass
        mc.save_config(self._config)

    def _reset(self):
        from core.minecraft import _DEFAULTS
        self._config.update(_DEFAULTS)
        mc.save_config(self._config)
        self.refresh()

    def refresh(self):
        self._config = mc.load_config()
        self._sliders["swingSpeedMultiplier"]._slider.set(
            self._config.get("swingSpeedMultiplier", 1.0))
        self._toggles["fpsTopRight"].set(self._config.get("fpsTopRight", True))
        self._toggles["armorDurabilityBar"].set(self._config.get("armorDurabilityBar", True))
        for key, entry in self._pos_entries.items():
            entry.delete(0, "end")
            entry.insert(0, str(self._config.get(key, 4)))
