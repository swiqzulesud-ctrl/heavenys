# Heavenys Client

> A performance-first, visually polished Minecraft client mod built on **Fabric** for Minecraft **1.21.5**.
> Strict no-cheat policy — fully compliant with Hypixel, LifestealSMP, and similar competitive server rules.

---

## Color Palette

| Swatch | Name | Hex | ARGB Int |
|--------|------|-----|----------|
| ██████ White | Primary text / accents | `#FFFFFF` | `0xFFFFFFFF` |
| ██████ Butter Yellow | Brand / active UI | `#F7E78E` | `0xFFF7E78E` |
| ██████ Butter Light | Soft backgrounds | `#FFFACD` | `0xFFFFFACD` |
| ██████ Panel BG | HUD panel backing | `#101010` @ 72% | `0xB8101010` |

All color constants live in `HeavenysColors.java` and are referenced by every module.

---

## Technology Stack

| Layer | Library | Why |
|-------|---------|-----|
| Loader | Fabric Loader 0.16.14 | Modern, fast, lightweight |
| Rendering | **Sodium 0.6.x** | GPU-accelerated chunk rendering, replaces OptiFine |
| Shaders | **Iris 1.8.x** | OptiFine-compatible shader packs via Sodium |
| Logic optimisation | **Lithium 0.14.x** | Server-tick and pathfinding optimisations |
| Config UI | Cloth Config 17.x | Runtime config API compatible with ModMenu |

---

## Project Structure

```
heavenys-client/
├── build.gradle                          # Fabric Loom build script
├── gradle.properties                     # All version pins
├── settings.gradle
├── gradlew / gradle/wrapper/             # Reproducible Gradle wrapper
└── src/main/
    ├── resources/
    │   ├── fabric.mod.json               # Mod descriptor + entrypoints
    │   ├── heavenys-client.mixins.json   # Mixin target config
    │   ├── heavenys-client.accesswidener # Field/method access wideners
    │   └── assets/heavenys-client/
    │       └── lang/en_us.json
    └── java/net/heavenys/client/
        ├── HeavenysClient.java           # ClientModInitializer entrypoint
        ├── config/
        │   └── HeavenysConfig.java       # JSON persistence (Gson)
        ├── hud/
        │   ├── HudModule.java            # Abstract base — render contract
        │   ├── HudModuleRegistry.java    # Module collection & DI
        │   ├── HudRenderer.java          # Per-frame orchestrator
        │   └── modules/
        │       ├── ArmorStatusModule.java
        │       ├── PotionStatusModule.java
        │       ├── KeystrokesModule.java
        │       ├── FpsModule.java
        │       └── PingModule.java
        ├── gui/
        │   └── HeavenysHudScreen.java    # In-game toggle menu (Right Shift)
        ├── mixin/
        │   ├── InGameHudMixin.java       # Injects HUD rendering at TAIL
        │   ├── HandRendererMixin.java    # 1.7-style arm swing speed
        │   └── GameRendererMixin.java    # Reserved for future hooks
        └── util/
            ├── HeavenysColors.java       # Central ARGB palette + helpers
            └── RenderUtil.java           # Thin DrawContext wrappers
```

---

## HUD Modules

All modules are **toggled via Right Shift → in-game menu**. State is saved
automatically to `.minecraft/config/heavenys-client.json` on close.

| Module | What it Shows | Compliance |
|--------|--------------|------------|
| **ArmorStatus** | Equipped armor pieces + optional durability bars | Safe — read-only client data |
| **PotionStatus** | Active effects, duration (mm:ss), amplifier | Safe — read-only client data |
| **Keystrokes** | WASD + Space + LMB/RMB highlight | Safe — reads `KeyBinding.isPressed()` |
| **FPS Counter** | Current FPS with color threshold (≥60 green) | Safe — `MinecraftClient.getCurrentFps()` |
| **Ping Display** | Latency from TAB-list entry | Safe — `PlayerListEntry.getLatency()` |

### Explicitly Excluded (by design)
- ESP / entity highlighting
- Reach extender
- Auto-clicker / auto-aim
- Kill aura
- Any module sending extra packets
- Any module reading server-side combat state beyond what the vanilla HUD shows

---

## 1.7-Style Combat Animation

`HandRendererMixin` intercepts the `swingProgress` float before the hand
renderer uses it and applies `swingSpeedMultiplier` from config (default 1.0,
range 0.5 – 2.0). This is **purely cosmetic** — attack cooldown and server
hitboxes are untouched. Increasing towards 2.0 gives the snappier 1.7 feel.

```json
// heavenys-client.json
{
  "swingSpeedMultiplier": 1.3
}
```

---

## Building

```bash
./gradlew build
# Output: build/libs/heavenys-client-1.0.0.jar
```

> **Java 21** is required. The Gradle wrapper downloads Gradle 8.12 automatically.

### Running in dev
```bash
./gradlew runClient
```

---

## Modularisation Principles

1. **Each module is a single class** extending `HudModule`. Adding a module
   means creating one file + registering one line in `HudModuleRegistry`.
2. **Config is a flat POJO** — no reflection magic, Gson handles it.
   Adding a new setting = one field in `HeavenysConfig`.
3. **Mixins are minimal** — only three targets, all well-scoped injections.
   Sodium/Iris compatibility is guaranteed because we never hook into their
   rendering internals.
4. **Color palette is a single constant class** (`HeavenysColors`). Change
   the brand look by editing one file.

---

## License

MIT — see `LICENSE_heavenys-client` in the built JAR.
