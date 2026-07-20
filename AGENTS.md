# Know Mods (KnowClient) - Agent Guide

Know Mods (KnowClient) is an original, open-source (GPL-3.0) Fabric client for **Minecraft 26.2**
(marketed as "1.26.2"). It is inspired by the open-source Sol Client but contains none of its
code, and no proprietary Lunar Client code/assets. Keep it that way.

## Cursor Cloud specific instructions

### Branding vs. namespace (non-obvious)
- The client is branded **Know Mods** (mod id `knowmods`, display name "Know Mods"), but the
  Java package/namespace is still **`com.heavenys.client`** and the entrypoint class is
  `HeavenysClient` (retained for source stability). Don't be confused by the mismatch: user-facing
  strings say "Know Mods"; internal packages/classes say "heavenys". The formerly-"Heavenys"
  asset namespace now lives under `assets/knowmods`.
- Premium theme defaults live in `InterfaceModule`: near-black `#0B0B0B` background, orange
  `#FF7A1A` accent, corner radius up to 20px. HUD/GUI colors read from that module.
- The PvP/survival counter HUDs (Totem/EndCrystal/EnderPearl/Arrow/Durability) do **read-only**
  inventory inspection via `com.heavenys.client.util.InventoryUtil` - no cheats, no automation.

### Toolchain (non-obvious)
- **Minecraft 26.2 requires JDK 25**, not Java 21. Loom refuses to configure on Java 21. A JDK 25
  is installed at `/opt/jdk-25` and the Gradle daemon is pointed at it via
  `~/.gradle/gradle.properties` (`org.gradle.java.home` / `org.gradle.java.installations.paths`).
  This user-level file is **not** committed, so if a build fails with
  "Minecraft 26.2 requires Java 25 but Gradle is using 21", recreate it. The mod's *own* bytecode
  still targets Java 21 (`options.release = 21`); only the compiler toolchain is 25.
- Minecraft 26.2 is **unobfuscated**: this project uses the `net.fabricmc.fabric-loom` plugin (the
  non-remapping variant), official Mojang names, **no Yarn/mappings block**, `implementation`
  (not `modImplementation`) and the plain `jar` task (not `remapJar`). Do not re-add mappings.
- The Minecraft version string is `26.2` (see `gradle.properties`), not `1.26.2`.

### Build / run / test (standard commands live in README)
- Build: `./gradlew build` (jar in `build/libs/`). Compile-only: `./gradlew compileJava`.
- Run the dev client: `./gradlew runClient`. It needs a display + OpenGL/Vulkan; on a headless VM
  use `xvfb-run` with software GL (`LIBGL_ALWAYS_SOFTWARE=1`). First run downloads the MC assets.
- There is no automated test suite yet; `./gradlew build` runs an empty `test` task.
- To read the real 26.2 API, `./gradlew genSources` decompiles Minecraft; the sources jar lands
  under `.gradle/loom-cache/.../-sources.jar`. This is the fastest way to verify a signature
  before writing rendering/input code, since APIs changed a lot in 26.1/26.2 (retained-mode GUI
  via `GuiGraphicsExtractor`, `MouseButtonEvent`/`KeyEvent`, `Minecraft.gui.setScreen`).

### Design constraints (durable)
- **No mixins by design.** HUD rendering uses Fabric `HudElementRegistry` + `HudElement`
  (`extractRenderState(GuiGraphicsExtractor, DeltaTracker)`). Input (CPS/keystrokes) uses GLFW
  polling (`Window.handle()`). Vanilla overlays (scoreboard, boss bar) are toggled through
  `HudElementRegistry.replaceElement`. Prefer these over adding mixins.
- Every feature is a `Module` with typed `Setting<T>`s; register new modules in
  `com.heavenys.client.registry.ModuleRegistry`. Config is JSON via `ConfigManager` and keys off
  the module name + setting name, so renaming a module/setting drops its saved value.
- Custom fonts (Inter/Poppins) are bundled under `assets/heavenys/fonts` (OFL). The `Font` selector
  setting currently falls back to the vanilla font renderer; wiring a custom glyph provider is a
  known follow-up.
