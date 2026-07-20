# Heavenys Client

Heavenys Client is an **open-source, heaven-themed Minecraft: Java Edition client** for the
[Fabric](https://fabricmc.net) mod loader, targeting **Minecraft 26.2** (marketed as "1.26.2").

It is original work created as a spiritual successor to the open-source
[Sol Client](https://github.com/Sol-Client/Client) (GPL-3.0). No code or assets are taken from
Sol Client (which targets legacy Minecraft), and **no proprietary Lunar Client code or assets are
used**. See [`NOTICE`](NOTICE) for attribution and [`LICENSE`](LICENSE) for the GNU GPL v3.0.

## Theme

A minimal, premium "Heaven" look:

- Background `#111111`, white outlines, accent `#FFD54A`
- Rounded UI, world blur behind panels, soft accent glow, smooth easing animations
- Simple **"H"** logo

## Features

- **Custom ClickGUI** with category tabs, toggles, sliders, enum/color/keybind settings
- **HUD Editor** with **drag & drop** positioning of every HUD element
- **Theme controls**: accent color picker, UI background opacity slider, blur, glow, animation style/speed, **font selector** (Inter / Poppins / Vanilla)
- **JSON config system**: profiles, auto-save, import/export
- **HUD modules**: FPS, Ping, TPS, CPS, Combo Counter, Coordinates, Direction, Clock, Armor Status, Inventory HUD, Potion Effects, Keystrokes, Watermark, Scoreboard, Boss Bar, Session Stats
- **Visual modules**: Zoom, Motion Blur, Fullbright, Time Changer, Weather Changer, Clear Water, Item Physics, GUI Scale, Crosshair editor, Chat Customizer
- **Compatibility detection** for Sodium, Lithium, FerriteCore, Entity Culling, ImmediatelyFast, Dynamic FPS, More Culling, Enhanced Block Entities and Noisium
- **Simple Voice Chat** auto-detection with push-to-talk, mic selector, volume, overlay, mute and deafen controls (the mod is never bundled)

## Building

Requires **JDK 25** (Minecraft 26.2 ships as Java 25 class files). The mod's own source targets Java 21.

```bash
./gradlew build        # produces build/libs/heavenys-client-<version>.jar
./gradlew runClient    # launches a dev Minecraft client with the mod loaded
```

Default keybinds: **Right Shift** opens the ClickGUI, **Right Ctrl** opens the HUD Editor.

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
├── compat/ModCompat          # third-party mod detection
└── registry/ModuleRegistry   # single place that wires up every module
```

The client intentionally uses **no mixins**: HUD rendering uses Fabric's
`HudElementRegistry`, input uses GLFW polling, and vanilla overlays (scoreboard, boss bar) are
toggled through Fabric's element replacement API.

## License

GNU General Public License v3.0 or later. See [`LICENSE`](LICENSE).
