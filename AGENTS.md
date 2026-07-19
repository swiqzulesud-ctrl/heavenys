# HeavenEssentials

A Heaven-themed **Lifesteal** system combined with a lightweight **Essentials**-style
command suite for **Spigot 1.26.2** (also runs on Paper). Written in Java 21 with the
Spigot API and **Adventure + MiniMessage** shaded into the jar.

- Plugin entry point: `com.heaven.essentials.HeavenEssentials`
- Build tool: Maven (`pom.xml`)
- Runtime target: Spigot `26.2` (Minecraft 1.26.2); also loads on Paper
- Config files (auto-generated in `plugins/HeavenEssentials/`): `config.yml`, `messages.yml`, `data.yml`

## Cursor Cloud specific instructions

### Toolchain (non-obvious)
- **JDK 25 is required to build**, even though the plugin source targets Java 21.
  `spigot-api:26.2-R0.1-SNAPSHOT` is Java 25 bytecode, so a Java 21 compiler cannot read
  it. Build with a JDK 25 while keeping `<maven.compiler.release>21</maven.compiler.release>`
  (already set in `pom.xml`). A Temurin JDK 25 is installed at `/usr/lib/jvm/jdk-25.0.3+9`.
- The Minecraft 1.26.2 **server also requires Java 25+** to run.
- Maven is installed system-wide (`mvn`). The default `java` on PATH is Java 21, so set
  `JAVA_HOME` to the JDK 25 for build/run commands.

### Adventure / MiniMessage (non-obvious)
- Spigot does **not** ship Adventure. The build **shades** `adventure-api`,
  `adventure-text-minimessage`, `adventure-text-serializer-legacy` and
  `adventure-platform-bukkit`, and **relocates** `net.kyori` to
  `com.heaven.essentials.libs.kyori` to avoid clashes. Do not remove the shade/relocate
  config or the plugin will fail at runtime on Spigot.
- All player-facing output goes through a `BukkitAudiences` (created in `onEnable`, closed
  in `onDisable`). Inventory titles and item name/lore are serialised to legacy (§) strings
  because Spigot's inventory/item APIs are string-based.
- `/anvil` uses `createInventory(..., InventoryType.ANVIL)` because Spigot's `HumanEntity`
  has no `openAnvil`.

### Build (produces the downloadable jar)
```
export JAVA_HOME=/usr/lib/jvm/jdk-25.0.3+9
mvn clean package
```
Output: `target/HeavenEssentials-1.0.0.jar` (a shaded, ~1.1 MB jar). A copy is committed to `dist/`.

### Run / test on a live server
Build a Spigot 1.26.2 server with BuildTools (note the version string is **`26.2`**, not
`1.26.2`), or just use a Paper 1.26.2 server (Paper runs Spigot plugins):
```
export JAVA_HOME=/usr/lib/jvm/jdk-25.0.3+9
java -jar BuildTools.jar --rev 26.2          # produces spigot-26.2.jar
# place target/HeavenEssentials-1.0.0.jar into <server>/plugins/, set eula=true, then:
java -jar spigot-26.2.jar --nogui
```
Notes:
- Set `online-mode=false` in `server.properties` for offline/local testing.
- Console-runnable commands (no player needed): `broadcast`, `day`/`night`, `sun`/`rain`,
  `heartsteal reload`. Player-context commands (`gm`, `fly`, `heal`, GUI via `heartsteal`,
  and PvP **lifesteal**) require an in-game player/client.
- Lifesteal only transfers hearts on **player-vs-player** kills (`Player#getKiller()` is
  non-null). Environmental deaths never move hearts.

### Lint / verify
- `mvn -q clean package` compiles and shades; it is the effective lint+build gate.
