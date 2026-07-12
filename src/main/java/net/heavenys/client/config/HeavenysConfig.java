package net.heavenys.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Flat JSON configuration file for Heavenys Client.
 * Persisted to {@code .minecraft/config/heavenys-client.json}.
 *
 * All fields are public primitives so Gson can serialize them without
 * reflection hacks, keeping the config human-editable.
 */
public class HeavenysConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("HeavenysConfig");
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE   = "heavenys-client.json";

    // ---- Module toggles -------------------------------------------------------
    public boolean armorStatusEnabled   = true;
    public boolean potionStatusEnabled  = true;
    public boolean keystrokesEnabled    = true;
    public boolean fpsEnabled           = true;
    public boolean pingEnabled          = true;

    // ---- Module positions (top-left anchor, in scaled pixels) ----------------
    public int armorStatusX   = 4;   public int armorStatusY   = 4;
    public int potionStatusX  = 4;   public int potionStatusY  = 52;
    public int keystrokesX    = 4;   public int keystrokesY    = 90;
    public int fpsX           = 4;   public int fpsY           = 4;   // anchored top-right by renderer
    public int pingX          = 4;   public int pingY          = 14;

    // ---- Visual options -------------------------------------------------------
    /** Show FPS anchored to the top-right corner instead of absolute position. */
    public boolean fpsTopRight = true;

    /** Show armor durability bars below the item icons. */
    public boolean armorDurabilityBar = true;

    // ---- Animation -----------------------------------------------------------
    /** Use 1.7-style item swing animation multiplier (range 0.5 – 2.0). */
    public float swingSpeedMultiplier = 1.0f;

    // ---- Persistence ---------------------------------------------------------

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    public static HeavenysConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                HeavenysConfig cfg = GSON.fromJson(reader, HeavenysConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                LOGGER.warn("[Heavenys] Failed to read config, using defaults. Cause: {}", e.getMessage());
            }
        }
        HeavenysConfig defaults = new HeavenysConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configPath())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("[Heavenys] Failed to save config: {}", e.getMessage());
        }
    }
}
