# Installing Heavenys Client

Heavenys ships as two pieces:

1. **Heavenys Launcher** — a desktop app (packaged as a Windows `.exe`, or run from the fat jar).
2. **Heavenys Client** — the in-game Fabric mod (a `.jar`).

> Prebuilt binaries are **not** committed to the repository. Download them from the CI
> artifacts (GitHub Actions → latest run) or build them yourself (see below).

## Option A — Windows installer (recommended)

1. Download `heavenys-launcher-windows` from the latest GitHub Actions run, or build it:
   ```bat
   scripts\build-windows.bat
   ```
   The installer is written to `launcher\build\jpackage\` (an `.exe`, or a portable app-image
   folder if the WiX Toolset is not installed).
2. Run the installer and launch **Heavenys Launcher** from the Start menu.
3. In the launcher: add an account (Offline, or Microsoft — see below), set your RAM/resolution,
   pick a version, and press **Launch**.

## Option B — Run the launcher from the fat jar (any OS)

```bash
./gradlew :launcher:fatJar
java -jar launcher/build/libs/launcher-*-all.jar
```

## Installing the in-game mod manually

The client mod is a standard Fabric mod:

1. Install [Fabric Loader](https://fabricmc.net/use/) for the target Minecraft version.
2. Copy `client/build/libs/heavenys-client-*.jar` **and**
   [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods/` folder.
3. Optionally add the optimization stack (Sodium, Lithium, FerriteCore, …) and Simple Voice Chat.

Build the mod jar with:
```bash
./gradlew :client:build   # -> client/build/libs/
```

## Microsoft login

Microsoft (Xbox) login uses the OAuth **device-code** flow and requires an **Azure AD
application client id**. Create one in the [Azure Portal](https://portal.azure.com) (App
registrations → new registration → allow public client / device-code flow), then provide it:

```bash
# Windows (PowerShell)
setx HEAVENYS_MSA_CLIENT_ID "<your-client-id>"
# Linux/macOS
export HEAVENYS_MSA_CLIENT_ID="<your-client-id>"
```

Without it, the launcher cleanly disables Microsoft login and you can still use **Offline**
accounts.

## Version note (Minecraft 1.26.2)

Heavenys targets the latest generation, but the in-game mod is currently built against
`1.21.11` because the Fabric mapping toolchain and a matching Java runtime are not yet
published for the 1.26.2 / "26.x" line. See the root `README.md` and `AGENTS.md` for details;
bumping is a one-line change in `gradle.properties` once mappings ship.
