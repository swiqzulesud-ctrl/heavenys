# HeavenEssentials

A Heaven-themed **Lifesteal** system combined with a lightweight **Essentials**-style
command suite for **Paper 1.26.2**, written in Java 21 with the modern Paper API,
Adventure + MiniMessage.

- Plugin entry point: `com.heaven.essentials.HeavenEssentials`
- Build tool: Maven (`pom.xml`)
- Runtime target: Paper `26.2` (Minecraft 1.26.2)
- Config files (auto-generated in `plugins/HeavenEssentials/`): `config.yml`, `messages.yml`, `data.yml`

## Cursor Cloud specific instructions

### Toolchain (non-obvious)
- **JDK 25 is required to build**, even though the plugin source targets Java 21.
  Paper `paper-api:26.2.build.62-beta` ships as Java 25 bytecode (classfile major 69),
  so a Java 21 compiler cannot read it. Build with a JDK 25 while keeping
  `<maven.compiler.release>21</maven.compiler.release>` (already set in `pom.xml`).
  A Temurin JDK 25 is installed at `/usr/lib/jvm/jdk-25.0.3+9`.
- The Paper 1.26.2 **server also requires Java 25+** to run.
- Maven is installed system-wide (`mvn`). The default `java` on PATH is Java 21, so set
  `JAVA_HOME` to the JDK 25 for build/run commands.

### Build (produces the downloadable jar)
```
export JAVA_HOME=/usr/lib/jvm/jdk-25.0.3+9
mvn clean package
```
Output: `target/HeavenEssentials-1.0.0.jar`.

### Run / test on a live server
There is no server in this repo; download Paper 1.26.2 and drop the jar in `plugins/`:
```
# server jar (build 62): https://fill.papermc.io/v3/projects/paper/versions/26.2
# place target/HeavenEssentials-1.0.0.jar into <server>/plugins/, set eula=true,
# then run with JDK 25:
export JAVA_HOME=/usr/lib/jvm/jdk-25.0.3+9
java -jar paper.jar --nogui
```
Notes:
- Set `online-mode=false` in `server.properties` for offline/local testing.
- Console-runnable commands (no player needed): `broadcast`, `day`/`night`, `sun`/`rain`,
  `heartsteal reload`. Player-context commands (`gm`, `fly`, `heal`, GUI via `heartsteal`,
  and PvP **lifesteal**) require an in-game player/client.
- Lifesteal only transfers hearts on **player-vs-player** kills (`Player#getKiller()` is
  non-null). Environmental deaths never move hearts.

### Lint / verify
- `mvn -q clean package` compiles with `-Xlint:all` and is the effective lint+build gate.
