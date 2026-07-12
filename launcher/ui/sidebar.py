"""
Left sidebar with navigation links and branding.
"""
from __future__ import annotations

from typing import Callable
import customtkinter as ctk
from core.constants import *


_NAV_ITEMS = [
    ("home",    "⌂  Home"),
    ("modules", "⊞  Modules"),
    ("settings","⚙  Settings"),
    ("about",   "✦  About"),
]


class Sidebar(ctk.CTkFrame):

    def __init__(self, master, on_navigate: Callable[[str], None], **kw):
        super().__init__(
            master,
            width=SIDEBAR_W,
            fg_color=C_SURFACE,
            corner_radius=0,
            **kw,
        )
        self.pack_propagate(False)
        self._on_navigate = on_navigate
        self._active = "home"
        self._buttons: dict[str, ctk.CTkButton] = {}
        self._build()

    def _build(self):
        # Brand logo area
        logo_frame = ctk.CTkFrame(self, fg_color="transparent", height=HEADER_H)
        logo_frame.pack(fill="x")
        logo_frame.pack_propagate(False)

        ctk.CTkLabel(
            logo_frame,
            text="✦",
            font=ctk.CTkFont(family="Segoe UI", size=28, weight="bold"),
            text_color=C_YELLOW,
        ).place(relx=0.18, rely=0.5, anchor="center")

        ctk.CTkLabel(
            logo_frame,
            text="Heavenys",
            font=ctk.CTkFont(family="Segoe UI", size=16, weight="bold"),
            text_color=C_WHITE,
        ).place(relx=0.6, rely=0.38, anchor="center")

        ctk.CTkLabel(
            logo_frame,
            text="CLIENT",
            font=ctk.CTkFont(family="Segoe UI", size=9, weight="bold"),
            text_color=C_YELLOW,
            letter_spacing=3 if hasattr(ctk.CTkFont, "letter_spacing") else 0,
        ).place(relx=0.6, rely=0.65, anchor="center")

        # Divider
        ctk.CTkFrame(self, height=1, fg_color=C_BORDER).pack(fill="x", pady=(0, 8))

        # Nav buttons
        for page_id, label in _NAV_ITEMS:
            btn = ctk.CTkButton(
                self,
                text=label,
                font=ctk.CTkFont(family="Segoe UI", size=13),
                anchor="w",
                height=40,
                corner_radius=8,
                fg_color="transparent",
                hover_color=C_SURFACE2,
                text_color=C_MUTED,
                command=lambda pid=page_id: self._navigate(pid),
            )
            btn.pack(fill="x", padx=10, pady=2)
            self._buttons[page_id] = btn

        # Bottom version label
        ctk.CTkLabel(
            self,
            text=f"v{APP_VERSION}",
            font=ctk.CTkFont(family="Segoe UI", size=10),
            text_color=C_DIMMED,
        ).pack(side="bottom", pady=12)

        # Highlight default
        self._highlight("home")

    def _navigate(self, page_id: str):
        self._highlight(page_id)
        self._on_navigate(page_id)

    def _highlight(self, active: str):
        for pid, btn in self._buttons.items():
            if pid == active:
                btn.configure(fg_color=C_SURFACE2, text_color=C_YELLOW)
            else:
                btn.configure(fg_color="transparent", text_color=C_MUTED)
        self._active = active
