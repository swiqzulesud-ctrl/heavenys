package net.heavenys.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.gui.HeavenysHudScreen;
import net.heavenys.client.hud.HudModuleRegistry;
import net.heavenys.client.hud.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Heavenys Client — Fabric client mod entrypoint.
 *
 * Responsibilities:
 * <ol>
 *   <li>Load config from disk.</li>
 *   <li>Build the {@link HudModuleRegistry} and {@link HudRenderer}.</li>
 *   <li>Register the HUD-settings keybind (Right Shift by default).</li>
 *   <li>Expose a singleton accessor for mixins and the GUI screen.</li>
 * </ol>
 *
 * Design notes
 * ─────────────
 * • All mutation goes through the config + registry. Mixins call only
 *   {@link #getConfig()}, {@link #getRegistry()}, and {@link #getHudRenderer()}.
 * • {@code saveConfig()} is called automatically when the HUD screen closes
 *   and during the client shutdown event.
 */
public class HeavenysClient implements ClientModInitializer {

    public static final String MOD_ID = "heavenys-client";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HeavenysClient INSTANCE;

    private HeavenysConfig     config;
    private HudModuleRegistry  registry;
    private HudRenderer        hudRenderer;
    private KeyBinding         hudMenuKey;

    // ---- Singleton ----------------------------------------------------------

    public static HeavenysClient getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("HeavenysClient not yet initialised");
        return INSTANCE;
    }

    // ---- Initialisation -----------------------------------------------------

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("[Heavenys] Initialising Heavenys Client…");

        config      = HeavenysConfig.load();
        registry    = new HudModuleRegistry(config);
        hudRenderer = new HudRenderer(registry);

        registerKeybinds();
        registerTickEvents();
        registerShutdownHook();

        LOGGER.info("[Heavenys] Ready. Modules loaded: {}", registry.all().size());
    }

    // ---- Keybinds -----------------------------------------------------------

    private void registerKeybinds() {
        hudMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.heavenys-client.hud_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.heavenys-client"
        ));
    }

    // ---- Tick events --------------------------------------------------------

    private void registerTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hudMenuKey.wasPressed()) {
                client.setScreen(new HeavenysHudScreen());
            }
        });
    }

    // ---- Shutdown -----------------------------------------------------------

    private void registerShutdownHook() {
        // Save config when the client closes cleanly.
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig, "heavenys-shutdown-save"));
    }

    // ---- Public API ---------------------------------------------------------

    public HeavenysConfig    getConfig()      { return config; }
    public HudModuleRegistry getRegistry()    { return registry; }
    public HudRenderer       getHudRenderer() { return hudRenderer; }

    /** Sync module states to config and persist to disk. */
    public void saveConfig() {
        if (registry != null && config != null) {
            registry.syncToConfig(config);
            config.save();
        }
    }
}
