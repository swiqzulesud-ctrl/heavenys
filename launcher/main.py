"""
Heavenys Client Launcher
========================
Entry point for the standalone .exe application.

Run:
    python main.py

Build as .exe:
    pyinstaller heavenys.spec          (Windows)
    pyinstaller heavenys_mac.spec      (macOS — produces .app)
"""
from __future__ import annotations

import sys
import os

# ── Make sure sibling packages are importable when frozen by PyInstaller ──────
if getattr(sys, "frozen", False):
    _BASE = sys._MEIPASS  # type: ignore[attr-defined]
else:
    _BASE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, _BASE)

import customtkinter as ctk
from core.constants import *
from ui.sidebar import Sidebar
from ui.page_home import HomePage
from ui.page_modules import ModulesPage
from ui.page_settings import SettingsPage
from ui.page_about import AboutPage


# ── Global appearance ──────────────────────────────────────────────────────────
ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("dark-blue")


class HeavenysLauncher(ctk.CTk):
    """Main application window."""

    def __init__(self):
        super().__init__()

        self.title(f"{APP_NAME}  —  Launcher  v{APP_VERSION}")
        self.geometry(f"{WIN_W}x{WIN_H}")
        self.minsize(WIN_MIN_W, WIN_MIN_H)
        self.configure(fg_color=C_BG)

        # Try to set a window icon (silently skip if asset missing)
        try:
            icon_path = os.path.join(_BASE, "assets", "icon.ico")
            if os.path.exists(icon_path):
                self.iconbitmap(icon_path)
        except Exception:
            pass

        self._pages: dict[str, ctk.CTkFrame] = {}
        self._active_page: str = ""
        self._build()

    # ── Layout ─────────────────────────────────────────────────────────────────

    def _build(self):
        # Sidebar
        self._sidebar = Sidebar(self, on_navigate=self._navigate)
        self._sidebar.pack(side="left", fill="y")

        # Thin separator line between sidebar and content
        ctk.CTkFrame(self, width=1, fg_color=C_BORDER).pack(side="left", fill="y")

        # Content area
        self._content = ctk.CTkFrame(self, fg_color=C_BG, corner_radius=0)
        self._content.pack(side="left", fill="both", expand=True)

        # Instantiate all pages (hidden initially)
        self._pages = {
            "home":     HomePage(self._content),
            "modules":  ModulesPage(self._content),
            "settings": SettingsPage(self._content),
            "about":    AboutPage(self._content),
        }

        # Show the home page
        self._navigate("home")

    # ── Navigation ─────────────────────────────────────────────────────────────

    def _navigate(self, page_id: str):
        if page_id == self._active_page:
            return

        # Hide current page
        if self._active_page and self._active_page in self._pages:
            self._pages[self._active_page].pack_forget()

        # Show new page
        page = self._pages[page_id]
        page.pack(fill="both", expand=True)
        page.refresh()
        self._active_page = page_id


# ── Entry point ────────────────────────────────────────────────────────────────

def main():
    app = HeavenysLauncher()
    app.mainloop()


if __name__ == "__main__":
    main()
