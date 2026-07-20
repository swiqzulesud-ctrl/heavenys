# Know Mods (KnowClient)

Know Mods is a **premium, open-source Minecraft: Java Edition client** for the
[Fabric](https://fabricmc.net) mod loader, targeting **Minecraft 26.2** (marketed
as "1.26.2"). It is designed as a modern, fair, and fully vanilla-compatible
alternative to commercial clients such as Lunar Client, with an emphasis on
**FPS, comfort, customization, and visual quality**.

It is original work created as a spiritual successor to the open-source
[Sol Client](https://github.com/Sol-Client/Client) (GPL-3.0). No code or assets
are taken from Sol Client (which targets legacy Minecraft), and **no proprietary
Lunar Client code or assets are used**. See [`NOTICE`](NOTICE) for attribution and
[`LICENSE`](LICENSE) for the GNU GPL v3.0.

## Fair play

Know Mods contains **no cheats, automation, combat assistance, x-ray, or unfair
gameplay advantages**. Every feature is a legitimate quality-of-life improvement,
and the client works on vanilla multiplayer servers.

## Theme

A premium, modern look:

- Near-black background `#0B0B0B`, white outlines, orange accent `#FF7A1A`
- Rounded UI (up to 20px), world blur behind panels, soft accent glow, smooth easing animations
- Simple **"K"** logo

## Features

- **Custom ClickGUI** with category tabs, toggles, sliders, enum/color/keybind settings
- **HUD Editor** with **drag & drop** positioning of every HUD element
- **Theme controls**: accent color picker, UI background opacity slider, blur, glow, animation style/speed, corner radius, **font selector** (Inter / Poppins / Vanilla)
- **JSON config system**: profiles, auto-save, import/export
- **Info HUD modules**: FPS, Ping, TPS, CPS, Combo Counter, Coordinates, Direction, Clock, Watermark, Scoreboard, Boss Bar, Session Stats
- **PvP / survival HUD counters**: Armor Status, Inventory HUD, Potion Effects, Keystrokes, **Totem Counter** (with off-hand tally), **End Crystal Counter** (low-stock warning), **Ender Pearl Counter**, **Arrow Counter**, **Durability** of the held item
- **Visual modules**: Zoom, Motion Blur, Fullbright, Time Changer, Weather Changer, Clear Water, Item Physics, GUI Scale, Crosshair editor, Chat Customizer
- **Compatibility detection** (read-only, never bundled) for Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling, ModernFix, Dynamic FPS, Indium, More Culling, Enhanced Block Entities, Noisium, Iris Shaders, Continuity, LambDynamicLights, Inventory Profiles Next, AppleSkin and BetterF3
- **Simple Voice Chat** auto-detection with push-to-talk, mic selector, volume, overlay, mute and deafen controls (the mod is never bundled)

> Popular performance/graphics mods (Sodium, Lithium, Iris, etc.) are **detected
> and integrated with**, not shipped inside Know Mods. Install them alongside the
> client to get their benefits; Know Mods adapts its UI and behaviour accordingly.

## Building

Requires **JDK 25** (Minecraft 26.2 ships as Java 25 class files). The mod's own source targets Java 21.

```bash
./gradlew build        # produces build/libs/knowmods-<version>.jar
./gradlew runClient    # launches a dev Minecraft client with the mod loaded
```

Default keybinds: **Right Shift** opens the ClickGUI, **Right Ctrl** opens the HUD Editor.

## Windows installer (.exe)

For players who don't want to copy jars by hand, the `installer` subproject builds
**`KnowMods-Installer.exe`** — a tiny native Windows executable (produced with
[launch4j](https://launch4j.sourceforge.net/)) that embeds the mod jar and copies it into
`%APPDATA%\.minecraft\mods` when run.

```bash
./gradlew :installer:createExe   # -> installer/build/launch4j/KnowMods-Installer.exe
```

The same program also runs as a cross-platform jar (`java -jar installer/build/libs/installer-<version>.jar`).
It only copies a file into your `mods` folder — no network access, registry edits, or elevated
permissions. You still need the Fabric loader for Minecraft 26.2 installed. Optional flags:
`--console` (print instead of showing a dialog) and `--dir <path>` (install into a specific folder).

## Architecture

```
com.heavenys.client
├── HeavenysClient            # Fabric entrypoint + service locator
├── config/ConfigManager      # JSON profiles, auto-save, import/export
├── module/                   # Module framework + typed Setting<T> system
│   ├── setting/              # Boolean/Number/Enum/Color/String/Keybind settings
│   └── impl/                 # client (theme), visual, compat modules
├── hud/                      # HUD base classes + impl/ HUD elements
├── gui/                      # ClickGuiScreen, HudEditorScreen
├── render/                   # UIRenderer (rounded rects, glow) + color helpers
├── input/InputTracker        # GLFW-polled CPS / keystrokes / combo (no mixins)
├── util/InventoryUtil        # read-only item counting for the PvP/survival HUDs
├── compat/ModCompat          # third-party mod detection
└── registry/ModuleRegistry   # single place that wires up every module
```

The Java package namespace remains `com.heavenys.client` for source stability;
the user-facing mod id is `knowmods`.

The client intentionally uses **no mixins**: HUD rendering uses Fabric's
`HudElementRegistry`, input uses GLFW polling, and vanilla overlays (scoreboard, boss bar) are
toggled through Fabric's element replacement API.

## License

GNU General Public License v3.0 or later. See [`LICENSE`](LICENSE).
