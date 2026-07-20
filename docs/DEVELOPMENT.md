# Developer Guide

## Repository layout

```
client/     In-game Fabric mod (Java). Modular HUD, in-game menu, themes.
launcher/   Standalone JavaFX desktop launcher (Java).
brand/      Logo sources (SVG / PNG / ICO).
scripts/    Windows / Linux build scripts.
docs/       Install & developer docs.
.github/    CI (GitHub Actions).
```

Both modules are part of one Gradle build (`settings.gradle` includes `:client` and `:launcher`).
Use the Gradle **wrapper** (`./gradlew`) — it pins Gradle 9.6.1.

## Launcher module (`:launcher`)

JavaFX 21 application, organised roughly MVVM:

```
model/    LauncherConfig, Account            (plain data)
core/     ConfigManager, AccountManager, LaunchService, MicrosoftAuth,
          SystemInfo, DiscordRichPresence    (services / logic — unit tested)
ui/       Theme, Animations, LauncherShell, Ui, LauncherContext
ui/views/ HomeView, AccountsView, SettingsView, OptimizationView
```

Common commands:

```bash
./gradlew :launcher:test              # unit tests (ConfigManager, AccountManager, LaunchService, MicrosoftAuth)
./gradlew :launcher:run               # run the launcher (add -Pswrender on headless/software-GL machines)
./gradlew :launcher:fatJar            # runnable fat jar -> launcher/build/libs/*-all.jar
./gradlew :launcher:jpackage          # native app-image  -> launcher/build/jpackage/
./gradlew :launcher:jpackage -PinstallerType=exe   # Windows .exe installer (run on Windows + WiX)
```

Design notes:
- **Config** is JSON at `~/.heavenys/launcher.json` with auto-save, versioned migration,
  named profiles (`profiles/`) and timestamped backups (`backups/`).
- **LaunchService** assembles + validates the JVM command and can run a real `-version`
  dry-run. Full vanilla bootstrapping (asset/library/native download, session auth) is the
  next layer and plugs in behind the same command prefix.
- **MicrosoftAuth** implements the device-code + Xbox/XSTS/Minecraft token chain; it is inert
  until `HEAVENYS_MSA_CLIENT_ID` is set, so the launcher degrades gracefully.
- Only the **launcher** font/opacity are themed here; the in-game Minecraft font is owned by
  the client mod and never altered.

## Client module (`:client`)

Fabric Loom mod. See the root `README.md` and `AGENTS.md` for run/build details and the
Minecraft version rationale.

```bash
./gradlew :client:build       # compile + remap the mod jar
./gradlew :client:runClient   # dev client (add -Penable_render_stack=false on headless GPUs)
```

## CI

`.github/workflows/build.yml`:
- **build** (Ubuntu): compiles the client + launcher, runs launcher tests, uploads the mod jar
  and launcher fat jar.
- **windows-installer** (Windows): runs `jpackage` to produce the `.exe` installer (falls back
  to a portable app-image if WiX is unavailable) and uploads it.
