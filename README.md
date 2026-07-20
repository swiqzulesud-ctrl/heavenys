# Heavenys Client

A premium, **Heaven-themed** Minecraft PvP client — a custom desktop **launcher** plus an
in-game **Fabric mod** — focused on competitive play, performance and a clean, polished UI.

- **100% original & license-respecting.** No proprietary Lunar Client code, assets, textures,
  or branding. Optimization and voice mods are pulled from their official open-source releases
  and credited; bundled fonts ship with their OFL/Apache licenses.
- **Theme:** Black (`#0D0D0D`), white outlines, gold (`#E8B923`) accents, rounded corners,
  smooth animations.

<p>
  <img src="brand/icon.png" width="96" alt="Heavenys logo"/>
</p>

## Components

| Module | What it is | Build |
|--------|------------|-------|
| `launcher/` | Standalone **JavaFX** launcher: accounts, RAM, resolution, versions, fonts, opacity, optimization presets. Packaged as a Windows `.exe` via `jpackage`. | `./gradlew :launcher:run` |
| `client/` | In-game **Fabric mod**: modular HUD, in-game config menu, black/gold theme, custom UI font. | `./gradlew :client:runClient` |

## Launcher features (implemented)

- **Accounts:** offline login, multiple accounts, active-account selection, "remember".
  Microsoft (Xbox) device-code login is implemented and enabled once an Azure client id is set
  (`HEAVENYS_MSA_CLIENT_ID`); it degrades gracefully when absent.
- **Java & memory:** min/max RAM sliders, GC selection, custom JVM args, Java-path picker.
- **Video:** resolution presets + custom size, fullscreen toggle.
- **Directories & versions:** game directory picker, version selector.
- **Launcher UI:** live opacity slider, UI **font selector** (Poppins / Inter / Montserrat /
  JetBrains Mono, + system fallback), UI scale, dark mode, Discord Rich Presence hook.
- **Optimization page:** toggle the Fabulously-Optimized-style stack (Sodium, Lithium,
  FerriteCore, Entity Culling, ImmediatelyFast, More Culling, Krypton, Dynamic FPS, FastQuit,
  Noisium, Enhanced Block Entities, Memory Leak Fix) with **Quality / Balanced / Competitive /
  Ultra FPS** presets.
- **Config system:** JSON, auto-save, versioned migration, profiles, backup/restore.
- **Launch:** assembles + validates the JVM command and runs a real runtime dry-run.

## In-game client features

Modular HUD (FPS, Ping, Armor, Potions, Keystrokes) with per-panel opacity and a custom,
toggleable UI font — see `client/`'s section in the repo. Ships alongside Sodium/Iris/Lithium
and Simple Voice Chat as dev-runtime mods.

## Quick start

```bash
# Everything (mod jar + launcher jar + tests)
./gradlew :client:build :launcher:test :launcher:fatJar

# Run the launcher (add -Pswrender on a headless / software-GL machine)
./gradlew :launcher:run

# Package the launcher (native app-image; add -PinstallerType=exe on Windows for the installer)
./gradlew :launcher:jpackage

# Run the in-game mod in a dev client
./gradlew :client:runClient
```

See [`docs/INSTALL.md`](docs/INSTALL.md) and [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).

## Scope, honesty & roadmap

This is a large product; the repository delivers a **working, tested foundation** with a clean
architecture and clear extension points, rather than unfinished stubs:

- ✅ Tested end-to-end: launcher UI, offline accounts, settings persistence, opacity/font live
  changes, optimization presets, launch dry-run, unit tests, and `jpackage` packaging.
- 🔌 **Microsoft login** needs a user-provided Azure app client id (`HEAVENYS_MSA_CLIENT_ID`).
- 🪟 The **Windows `.exe`** is produced by the build/CI on Windows (not committed) — see the CI
  workflow and `scripts/build-windows.bat`.
- 🧭 **Minecraft 1.26.2:** the in-game mod currently targets `1.21.11`. The Fabric mapping
  toolchain (Yarn/official mappings) and the required Java 25 runtime are **not yet published**
  for the 1.26.2 / "26.x" line, so the newest fully-mapped release is used. This is a one-line
  bump in `gradle.properties` once mappings ship.
- 🎙️ **Voice chat / Simple Voice Chat** is wired for the client's current version; the launcher
  keeps voice/optimization integrations modular so unavailable-for-1.26.2 components are simply
  disabled until a compatible release exists.
- 🗺️ Further in-game systems (full 60-module PvP suite, drag-and-drop HUD editor, cosmetics
  framework, in-depth performance graphs, replay) are architected for but not all implemented in
  this iteration.

## Licensing

Heavenys is MIT-licensed (see `LICENSE`). Bundled fonts retain their original licenses
(e.g. Poppins — SIL OFL). Third-party mods referenced by the launcher/optimization stack remain
under their own open-source licenses and are downloaded from their official sources.
