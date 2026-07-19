# HeavenEssentials

A **production-ready Minecraft Spigot 1.26.2 plugin** (also runs on Paper) that combines
a **Lifesteal** system with a lightweight **Essentials**-style command suite. Built with
**Java 21** and **Maven**, using the Spigot API and **Adventure + MiniMessage** (shaded
and relocated into the jar), clean OOP architecture, and no deprecated gameplay APIs.

Everything is **Heaven-themed** — elegant white, gold, light blue and soft cyan — and
every message is written in MiniMessage and fully configurable in `messages.yml`.

## Features

### Lifesteal
- Killing another **player** grants the killer **+1 heart** (2 health) and removes **1 heart** from the victim.
- Hearts transfer **only** on player-vs-player kills. Deaths from mobs, lava, fall, explosions, drowning, void, etc. never move hearts.
- Hearts are clamped: never below **1 heart**, never above the configured **maximum** (default **20**).
- New players start with the configured **default** hearts.
- Hearts are stored by **UUID** in `data.yml`, persist across disconnects/reloads/restarts, and drive `Attribute.MAX_HEALTH`.
- Data saves automatically whenever hearts change.
- `/heartsteal` opens a **Heaven-themed admin GUI** (permission `plugin.admin`) to raise/lower the maximum hearts with instant saving and automatic clamping of players when the max is lowered.
- `/heartsteal reload` reloads `config.yml` and `messages.yml`.

### Commands
`/gm`, `/fly`, `/heal`, `/feed`, `/god`, `/speed`, `/tp`, `/tphere`, `/tpall`,
`/spawn`, `/setspawn`, `/back`, `/invsee`, `/enderchest` (`/ec`), `/clear`, `/repair`,
`/workbench` (`/wb`, `/craft`), `/anvil`, `/hat`, `/top`, `/bottom`, `/day`, `/night`,
`/sun`, `/rain`, `/broadcast` (`/bc`), `/msg` (`/tell`, `/w`), `/reply` (`/r`), `/ignore`.

Every command has its own permission node, configurable MiniMessage output, tab
completion, and proper error handling. Optional target players are supported where
appropriate (guarded by `*.others` permissions).

> The only location system is the global `/spawn` + `/setspawn`. There are intentionally
> **no** homes, warps, `/tpa`, economy, kits, shops, mail, claims, vanish, jail, mute, ban, etc.

## Building

Requires **JDK 25** to build (the Spigot 1.26.2 API is Java 25 bytecode) while the plugin
itself targets Java 21. The build shades Adventure + MiniMessage into the jar. See
[`AGENTS.md`](AGENTS.md) for details.

```bash
export JAVA_HOME=/path/to/jdk-25
mvn clean package
```

The compiled plugin is produced at `target/HeavenEssentials-1.0.0.jar` (a copy is committed
to `dist/`). Drop it into your Spigot (or Paper) server's `plugins/` folder.

## Configuration

- `config.yml` — hearts (default/maximum/minimum), lifesteal settings, and the global spawn.
- `messages.yml` — every user-facing message as MiniMessage.
- `data.yml` — per-UUID heart storage (managed automatically).
