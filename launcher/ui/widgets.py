"""
Reusable styled widgets for the Heavenys Launcher.
All widgets inherit from customtkinter and apply the Butter Yellow palette.
"""
from __future__ import annotations

import customtkinter as ctk
from core.constants import *


class HCard(ctk.CTkFrame):
    """A styled surface card with optional title."""

    def __init__(self, master, title: str = "", **kw):
        super().__init__(
            master,
            fg_color=C_SURFACE,
            corner_radius=10,
            border_width=1,
            border_color=C_BORDER,
            **kw,
        )
        if title:
            ctk.CTkLabel(
                self, text=title,
                font=ctk.CTkFont(family="Segoe UI", size=13, weight="bold"),
                text_color=C_YELLOW,
                anchor="w",
            ).pack(fill="x", padx=14, pady=(12, 6))


class HButton(ctk.CTkButton):
    """Primary Butter Yellow action button."""

    def __init__(self, master, **kw):
        kw.setdefault("fg_color",          C_YELLOW)
        kw.setdefault("hover_color",       C_YELLOW_DARK)
        kw.setdefault("text_color",        C_BG)
        kw.setdefault("corner_radius",     8)
        kw.setdefault("height",            38)
        kw.setdefault("font", ctk.CTkFont(family="Segoe UI", size=13, weight="bold"))
        super().__init__(master, **kw)


class HButtonGhost(ctk.CTkButton):
    """Ghost (outline) button — secondary action."""

    def __init__(self, master, **kw):
        kw.setdefault("fg_color",          "transparent")
        kw.setdefault("hover_color",       C_SURFACE2)
        kw.setdefault("text_color",        C_YELLOW)
        kw.setdefault("border_color",      C_YELLOW_DIM)
        kw.setdefault("border_width",      1)
        kw.setdefault("corner_radius",     8)
        kw.setdefault("height",            38)
        kw.setdefault("font", ctk.CTkFont(family="Segoe UI", size=13))
        super().__init__(master, **kw)


class HLabel(ctk.CTkLabel):
    """Standard body text label."""

    def __init__(self, master, **kw):
        kw.setdefault("text_color", C_WHITE)
        kw.setdefault("font", ctk.CTkFont(family="Segoe UI", size=13))
        super().__init__(master, **kw)


class HLabelMuted(ctk.CTkLabel):
    """Secondary / muted text label."""

    def __init__(self, master, **kw):
        kw.setdefault("text_color", C_MUTED)
        kw.setdefault("font", ctk.CTkFont(family="Segoe UI", size=12))
        super().__init__(master, **kw)


class HSeparator(ctk.CTkFrame):
    """1-px horizontal rule."""

    def __init__(self, master, **kw):
        super().__init__(master, height=1, fg_color=C_BORDER, **kw)


class HToggleRow(ctk.CTkFrame):
    """
    A single module toggle row:
    [● dot]  Module Name  [description]   [ON/OFF switch]
    """

    def __init__(self, master, label: str, description: str,
                 initial: bool, on_change=None, **kw):
        super().__init__(master, fg_color="transparent", **kw)

        # Status dot
        self._dot = ctk.CTkLabel(
            self, text="●",
            font=ctk.CTkFont(size=11),
            text_color=C_GREEN if initial else C_RED,
            width=16,
        )
        self._dot.pack(side="left", padx=(0, 8))

        # Name + description
        text_frame = ctk.CTkFrame(self, fg_color="transparent")
        text_frame.pack(side="left", fill="x", expand=True)
        ctk.CTkLabel(
            text_frame, text=label,
            font=ctk.CTkFont(family="Segoe UI", size=13, weight="bold"),
            text_color=C_WHITE, anchor="w",
        ).pack(fill="x")
        if description:
            ctk.CTkLabel(
                text_frame, text=description,
                font=ctk.CTkFont(family="Segoe UI", size=11),
                text_color=C_MUTED, anchor="w",
            ).pack(fill="x")

        # Toggle switch
        self._var = ctk.BooleanVar(value=initial)
        self._switch = ctk.CTkSwitch(
            self, variable=self._var, text="",
            onvalue=True, offvalue=False,
            progress_color=C_YELLOW,
            button_color=C_BG,
            button_hover_color=C_SURFACE2,
            fg_color=C_DIMMED,
            width=44, height=22,
            command=self._toggle,
        )
        self._switch.pack(side="right", padx=(8, 0))
        self._on_change = on_change

    def _toggle(self):
        enabled = self._var.get()
        self._dot.configure(text_color=C_GREEN if enabled else C_RED)
        if self._on_change:
            self._on_change(enabled)

    def get(self) -> bool:
        return self._var.get()

    def set(self, value: bool):
        self._var.set(value)
        self._dot.configure(text_color=C_GREEN if value else C_RED)


class HStatusBadge(ctk.CTkLabel):
    """Small colored pill-shaped status badge."""

    _COLORS = {
        "ok":      (C_GREEN,  C_BG),
        "warn":    (C_AMBER,  C_BG),
        "error":   (C_RED,    C_WHITE),
        "neutral": (C_BORDER, C_MUTED),
    }

    def __init__(self, master, text: str, state: str = "neutral", **kw):
        fg, txt = self._COLORS.get(state, self._COLORS["neutral"])
        super().__init__(
            master, text=f"  {text}  ",
            fg_color=fg, text_color=txt,
            corner_radius=6,
            font=ctk.CTkFont(family="Segoe UI", size=11, weight="bold"),
            **kw,
        )

    def update_state(self, text: str, state: str):
        fg, txt = self._COLORS.get(state, self._COLORS["neutral"])
        self.configure(text=f"  {text}  ", fg_color=fg, text_color=txt)


class HSliderRow(ctk.CTkFrame):
    """Labeled slider with live value display."""

    def __init__(self, master, label: str, from_: float, to: float,
                 initial: float, fmt: str = "{:.2f}", on_change=None, **kw):
        super().__init__(master, fg_color="transparent", **kw)
        self._fmt = fmt
        self._on_change = on_change

        ctk.CTkLabel(
            self, text=label,
            font=ctk.CTkFont(family="Segoe UI", size=13),
            text_color=C_WHITE, width=180, anchor="w",
        ).pack(side="left")

        self._value_label = ctk.CTkLabel(
            self, text=fmt.format(initial),
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
            text_color=C_YELLOW, width=44,
        )
        self._value_label.pack(side="right", padx=(8, 0))

        self._slider = ctk.CTkSlider(
            self, from_=from_, to=to,
            progress_color=C_YELLOW,
            button_color=C_YELLOW,
            button_hover_color=C_YELLOW_DARK,
            fg_color=C_BORDER,
            command=self._moved,
        )
        self._slider.set(initial)
        self._slider.pack(side="left", fill="x", expand=True, padx=(8, 0))

    def _moved(self, val: float):
        self._value_label.configure(text=self._fmt.format(val))
        if self._on_change:
            self._on_change(val)

    def get(self) -> float:
        return self._slider.get()
