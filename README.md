# SMPlugin

A white/gold prestige layer for survival SMP servers, built for **Arclight 1.20.1**
("Trials", Forge 47 hybrid) on **Java 17**.

> **Download:** grab the ready-to-use jar from
> [`dist/SMPlugin-1.0.0-arclight-1.20.1.jar`](dist/SMPlugin-1.0.0-arclight-1.20.1.jar)
> and drop it into your server's `plugins/` folder.

Arclight implements the **Spigot API** (not the Paper API), so the plugin targets
`spigot-api 1.20.1` and ships with the Adventure/MiniMessage library shaded and relocated
into the jar. It also runs on plain Spigot/Paper 1.20.1, and on newer servers
(1.20.x–1.21.x) thanks to Bukkit's `api-version: 1.20` compatibility layer.

## Features

### 👑 The Three Crowns
Three permanent, simultaneous crowns with full in-game presentation — golden crown hologram
above the holder, white glow, tab-list prefix, broadcast + title + sound on every change,
and a white-glass `/crowns` GUI:

- **Crown of Kills** — auto-tracked PvP kills, recalculated instantly on every kill.
  `/crowns kills` shows the live top 10.
- **Crown of Resources** — awarded by community vote on Discord based on storage/base
  screenshots; staff set it with `/crowns setresources <player>` / `/crowns clearresources`.
- **Crown of the Builder** — won through the recurring in-game builder vote.

### ✦ Builder Vote (every 4 real-life days, configurable)
- **Nomination window** — `/build submit <name>` while standing at your build (the spot is
  saved as its showcase point).
- **Vote window** — `/build vote` opens a paginated white-themed GUI of player heads;
  self-voting is blocked, revoting is configurable.
- **Results** — automatic tally, server-wide announcement, white fireworks over the winning
  build, and a winner-only **Choose Your Reward** GUI: +2 max hearts (capped, persistent),
  a beacon, an end crystal, a dragon egg, or any player's head (chat-prompted name lookup).
- Empty cycles are announced and roll over cleanly.

### ⚔ The Sovereign's Relic
A rare boss event (every 14 days and/or `/relic summon`):
- Ominous countdown broadcasts at 10 / 5 / 1 minutes before arrival.
- The **Sovereign Guardian** — a heavily buffed Wither Skeleton (~420 HP, high damage,
  full knockback resistance, periodic lightning AOE + summoned adds, enrage below 30% HP)
  with a white boss bar and an END_ROD spiral aura. Balanced for a coordinated group in
  enchanted Netherite; under-geared players entering the arena get a soft warning.
- On death it drops the one-of-a-kind **Crown-Splitter Axe** (Sharpness X, Looting IV,
  Unbreaking V, PDC-tagged) *on the ground* — no auto-assign, pure scramble — under a
  sky-high particle beam. Only one can exist; if it's destroyed the Guardian may rise again.
- `/relic log` shows the damage leaderboard of the last fight.

## Commands

| Command | Description |
| --- | --- |
| `/crowns` | Open the main Crowns GUI |
| `/crowns kills` | Kill Crown top-10 leaderboard |
| `/crowns setresources <player>` | (admin) set the Resource Crown holder |
| `/crowns clearresources` | (admin) clear the Resource Crown |
| `/build submit <name>` | Submit your build during the nomination window |
| `/build vote` | Open the voting GUI |
| `/build results` | Show last cycle's results |
| `/build reward` | Re-open an unclaimed reward chooser |
| `/relic summon [now]` | (admin) trigger the Guardian (with or without build-up) |
| `/relic log` | (admin) last event's participants |
| `/relic reset` | (admin) clear the one-copy flag if the axe was lost untracked |
| `/smplugin reload` | Reload `config.yml` |

Permissions: `smplugin.crowns.use/admin`, `smplugin.build.use`, `smplugin.relic.admin`,
`smplugin.admin` (grants all admin nodes).

## Tech
- Spigot API `1.20.1` (Arclight-compatible), Java 17, Maven.
- SQLite persistence (kills, crown holders, bonus hearts, vote cycles, relic flag,
  participation logs) — all reads/writes off the main thread.
- Adventure/MiniMessage for all text, shaded + relocated (`dev.smplugin.libs.kyori`) and
  bridged through `adventure-platform-bukkit`, since Spigot/Arclight don't bundle Adventure.
- PersistentDataContainer tags on the relic item, boss entities and GUI items.

## Building

```bash
mvn package
```

The jar lands in `target/SMPlugin-1.0.0.jar` (identical to the one in `dist/`). Drop it into
`plugins/`; the SQLite driver is pulled automatically at first startup via the plugin
`libraries` mechanism.

### Verified on Arclight
Smoke-tested on `arclight-forge-1.20.1-1.0.6` (Trials, Java 17): plugin loads/enables, all
commands respond, the full Sovereign Guardian lifecycle works (summon → fight → relic drop →
one-copy lock → destruction → re-arm, including silent removals caught by the drop watchdog),
and shutdown is clean. `/relic reset` is available as an admin escape hatch if the axe is ever
lost in a way the plugin cannot observe.

## Configuration
See `src/main/resources/config.yml` — vote cycle lengths, relic schedule, boss stats,
max-heart cap, arena coordinates and per-feature toggles are all exposed.
