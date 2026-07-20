# AGENTS.md

## Cursor Cloud specific instructions

Heavenys Client is a **client‑only Fabric mod** for Minecraft. There is a single Gradle
project (Fabric Loom). Standard commands live in `README.md`; the notes below are the
non‑obvious things.

### Toolchain gotchas
- **Gradle 9.6.1 is required** (via the committed wrapper — always use `./gradlew`). Fabric
  Loom `1.17.16` refuses Gradle 8.x with a cryptic "No matching variant … plugin.api-version
  '9.5.0'" error. Do not downgrade the wrapper.
- **JDK 21** is the toolchain (`org.gradle.jvmargs`/`options.release = 21`). It is
  pre‑installed on the VM.
- No Java hot reload: after changing `.java` files you must rebuild and restart
  `runClient` to see changes (resources like lang/font/json do reload on world/resource reload).

### Target version note (why 1.21.11)
- The mod targets `1.21.11` because Yarn/official mappings — and a Java 25 runtime — are **not
  yet available for the "26.x" generation** in this environment (Fabric `meta` has 26.x game
  versions but no 26.x mappings; the 26.2 client also requires Java 25). Everything for 1.21.11
  (mappings, Fabric API, Sodium/Iris/Lithium, Simple Voice Chat) resolves and runs. Bumping is a
  one‑line change in `gradle.properties` once 26.x mappings ship.

### Running the client on this VM (headless / software OpenGL)
- A TigerVNC X server is already running on **`DISPLAY=:1`**; the game window renders there
  (viewable via the Desktop pane). There is no GPU — use Mesa **software** OpenGL:
  ```bash
  export DISPLAY=:1 LIBGL_ALWAYS_SOFTWARE=1 __GLX_VENDOR_LIBRARY_NAME=mesa \
         GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=4.5 MESA_GLSL_VERSION_OVERRIDE=450
  ./gradlew runClient -Penable_render_stack=false
  ```
- **Always pass `-Penable_render_stack=false` when running on the VM.** Sodium/Iris require a
  real GPU / modern GL and will crash under llvmpipe. (The stack still *builds*/resolves fine;
  it's only the live software‑rendered run that needs it off.) On a real machine, omit the flag.
- Expected‑and‑harmless startup noise: `OpenAL`/`ALSA` audio errors (no sound device) and the
  Realms `SignedJWT` auth warning. The dev client uses an offline account, so no login is needed.
- Software rendering is **slow** (single‑digit FPS). When driving the UI, wait several seconds
  between interactions.

### UI architecture notes
- The clean client font is a bundled TTF applied **per‑`Text`** via a font style
  (`StyleSpriteSource.Font`, id `heavenys:heavenys`). It only affects Heavenys' own UI — never
  the vanilla game font — and can be toggled off in the in‑game menu (`Right Shift`).
- HUD panel opacity, module toggles, font on/off and resolution are persisted to
  `<config>/heavenys.json` via the `HeavenysConfig` singleton.
