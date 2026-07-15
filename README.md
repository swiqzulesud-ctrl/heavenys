# HeavenEssentials

A Heaven-themed **Lifesteal** system combined with a lightweight **Essentials-style** command suite for Paper 26.2 (Minecraft "1.26.2"), built with Java 21 and Maven.

## Building

```bash
mvn clean package
```

The plugin jar is produced at `target/HeavenEssentials-1.0.0.jar`.

The source code only uses Java 21 language features, but the Paper 26.2 API (and the Paper 26.2 server itself) is compiled for Java 25, so building requires JDK 25 (`maven.compiler.release` is set to 25 accordingly).

## Lifesteal

- Killing another **player** transfers one heart (2 health) from the victim to the killer.
- Deaths caused by mobs, fall damage, lava, explosions, drowning, the void, etc. never transfer hearts.
- Players can never drop below the configured minimum (default 1 heart) or rise above the configured maximum (default 20 hearts).
- New players start with the configured default hearts (default 10).
- Hearts are stored by UUID in `data.yml`, applied through `Attribute.MAX_HEALTH`, and saved instantly on every change, so they survive disconnects, reloads, and restarts.
- `/heartsteal` (permission `heaven.admin`) opens a Heaven-themed admin GUI to adjust the maximum hearts with instant saving; lowering the maximum automatically clamps all players, online and offline.
- `/heartsteal reload` reloads `config.yml`, `messages.yml`, and `data.yml`.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/heartsteal [reload]` | `heaven.admin` | Lifesteal admin GUI / reload |
| `/gm <mode> [player]` | `heaven.command.gamemode[.others]` | Change game mode |
| `/fly [player]` | `heaven.command.fly[.others]` | Toggle flight |
| `/heal [player]` | `heaven.command.heal[.others]` | Restore health |
| `/feed [player]` | `heaven.command.feed[.others]` | Restore hunger |
| `/god [player]` | `heaven.command.god[.others]` | Toggle invulnerability |
| `/speed <1-10> [player]` | `heaven.command.speed[.others]` | Set walk/fly speed |
| `/tp <target>` / `/tp <player> <target>` | `heaven.command.tp[.others]` | Teleport |
| `/tphere <player>` | `heaven.command.tphere` | Summon a player |
| `/tpall` | `heaven.command.tpall` | Summon everyone |
| `/spawn [player]` | `heaven.command.spawn[.others]` | Go to spawn |
| `/setspawn` | `heaven.command.setspawn` | Set the global spawn |
| `/back` | `heaven.command.back` | Return to your previous location |
| `/invsee <player>` | `heaven.command.invsee` | View a player's inventory |
| `/enderchest [player]` (`/ec`) | `heaven.command.enderchest[.others]` | Open an ender chest |
| `/clear [player]` | `heaven.command.clear[.others]` | Clear an inventory |
| `/repair [all]` | `heaven.command.repair[.all]` | Repair items |
| `/workbench` | `heaven.command.workbench` | Portable crafting table |
| `/anvil` | `heaven.command.anvil` | Portable anvil |
| `/hat` | `heaven.command.hat` | Wear the held item |
| `/top` | `heaven.command.top` | Teleport to the surface |
| `/bottom` | `heaven.command.bottom` | Teleport to the lowest safe spot |
| `/day` / `/night` | `heaven.command.day` / `.night` | Change time |
| `/sun` / `/rain` | `heaven.command.sun` / `.rain` | Change weather |
| `/broadcast <message>` | `heaven.command.broadcast` | Server-wide announcement |
| `/msg <player> <message>` | `heaven.command.msg` | Private message |
| `/reply <message>` | `heaven.command.reply` | Reply to the last message |
| `/ignore <player>` | `heaven.command.ignore` | Toggle ignoring a player |

`heaven.*` grants everything.

## Configuration

- `config.yml` — Lifesteal settings (default/min/max hearts, hearts per kill, master switch).
- `messages.yml` — every message, GUI title, and item name in MiniMessage format using the Heaven palette (soft white `#EAF6FF`, gold `#FFD966`, light blue `#9BDCFF`, soft cyan `#A7F0EA`). Set a message to `''` to disable it.
- `data.yml` — managed automatically: player hearts, the global spawn, and ignore lists.
