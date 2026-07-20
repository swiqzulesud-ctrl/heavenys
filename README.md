# Heavenys Client

A clean, lightweight and **fully rule‑compliant** Minecraft utility client built as a
[Fabric](https://fabricmc.net/) mod. Heavenys focuses on performance, visual polish and a
tidy user experience — **no cheats, no unfair advantages**, safe for competitive servers
(Hypixel / Lifesteal‑style rules).

- **Aesthetic:** clean **Black & Yellow** UI with a bundled sans‑serif client font.
- **Performance stack:** ships alongside **Sodium** (FPS), **Iris** (shaders) and
  **Lithium** (tick perf) instead of OptiFine.
- **Voice:** works with **Simple Voice Chat** (modular proximity voice chat).
- **HUD modules (all non‑bannable):** ArmorStatus, PotionStatus, minimal Keystrokes,
  FPS and Ping — every module toggleable from a clean in‑game menu (default key: `Right Shift`).

> **Strictly excluded by design:** ESP, reach, auto‑clicker, or anything that reads
> server‑side combat data. Heavenys only *reads and displays* information the local player
> already has.

## Target version

| Component | Version |
|-----------|---------|
| Minecraft | `1.21.11` (see note below) |
| Fabric Loader | `0.19.3` |
| Yarn mappings | `1.21.11+build.6` |
| Fabric API | `0.141.5+1.21.11` |
| Java | 21 |

**Why 1.21.11 and not 26.x?** Heavenys is designed for the latest "26.x" generation, but the
Fabric mapping toolchain (Yarn / official mappings) and a matching Java runtime are not yet
published for 26.x. The dev environment therefore targets the newest fully‑mapped stable
release. Bumping is a one‑line change in `gradle.properties` once 26.x mappings ship.

## Project layout

```
build.gradle / settings.gradle / gradle.properties   # Fabric Loom build
src/main/                     # common (environment-agnostic) entrypoint + resources
  java/com/heavenys/HeavenysClient.java
  resources/fabric.mod.json, assets/heavenys/{icon.png, font/, lang/}
src/client/                   # client-only code (split source set)
  java/com/heavenys/client/
    HeavenysClientMod.java     # client entrypoint: HUD + key bind
    config/HeavenysConfig.java # JSON-backed settings singleton
    theme/HeavenysTheme.java   # Black/Yellow palette + opacity
    theme/HeavenysFont.java    # bundled client font (per-Text, never touches game font)
    hud/HudModule.java, HudManager.java, hud/modules/*   # modular HUD
    gui/HeavenysConfigScreen.java + themed widgets       # in-game menu
```

The HUD is modular: a single `HudElement` is registered with Fabric's `HudElementRegistry`
and delegates to each enabled `HudModule`, so adding a module is one `register(...)` call and
the render‑hook count stays at exactly one.

## Build & run

```bash
./gradlew build         # compile + produce the mod jar in build/libs/
./gradlew runClient     # launch a dev Minecraft client with the mod loaded
```

The performance stack and voice chat are pulled as **dev‑runtime** mods. Disable them if
needed (e.g. on headless/software OpenGL):

```bash
./gradlew runClient -Penable_render_stack=false   # skip Sodium/Iris/Lithium
./gradlew runClient -Penable_voicechat=false       # skip Simple Voice Chat
```

## Installing on your PC

A Fabric mod is distributed as the built **`.jar`** (`build/libs/heavenys-client-*.jar`), not
an `.exe`. To run it:

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11.
2. Drop `heavenys-client-*.jar` **and** [Fabric API](https://modrinth.com/mod/fabric-api) into
   your `.minecraft/mods/` folder (optionally add Sodium/Iris/Lithium and Simple Voice Chat).
3. Launch the Fabric profile.

> RAM allocation, game resolution presets and account login are **launcher‑level** features
> (handled by the Minecraft launcher / a wrapper launcher), not something a mod controls at
> runtime. The in‑game menu applies window resolution live and surfaces the current RAM/user
> for clarity; changing the actual allocated RAM or the signed‑in account is done in the launcher.
