package com.heaven.essentials.config;

import com.heaven.essentials.HeavenEssentials;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Wraps {@code config.yml} and exposes typed, validated access to all
 * plugin settings. Runtime-mutable values (the maximum hearts and the global
 * spawn) are written straight back to disk so they persist across restarts.
 */
public final class ConfigManager {

    /** Absolute floor for hearts; players may never drop below this. */
    public static final int ABSOLUTE_MIN_HEARTS = 1;
    /** Absolute ceiling for hearts (vanilla max-health attribute caps at 1024 health). */
    public static final int ABSOLUTE_MAX_HEARTS = 512;

    private final HeavenEssentials plugin;

    public ConfigManager(HeavenEssentials plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public void save() {
        plugin.saveConfig();
    }

    // ---------------------------------------------------------------------
    //  Hearts settings
    // ---------------------------------------------------------------------

    public int getDefaultHearts() {
        return clampToBounds(config().getInt("hearts.default", 10));
    }

    public int getMaxHearts() {
        return clampToBounds(config().getInt("hearts.maximum", 20));
    }

    public int getMinHearts() {
        int min = config().getInt("hearts.minimum", ABSOLUTE_MIN_HEARTS);
        return Math.max(ABSOLUTE_MIN_HEARTS, Math.min(min, getMaxHearts()));
    }

    public int getHeartsPerKill() {
        return Math.max(1, config().getInt("lifesteal.hearts-per-kill", 1));
    }

    public boolean areEffectsEnabled() {
        return config().getBoolean("lifesteal.effects", true);
    }

    public boolean shouldAnnounceClamp() {
        return config().getBoolean("settings.announce-clamp", true);
    }

    /**
     * Persists a new maximum-hearts value, keeping it inside the absolute
     * bounds and never below the configured minimum.
     *
     * @return the value that was actually stored after clamping
     */
    public int setMaxHearts(int value) {
        int clamped = Math.max(getMinHearts(), Math.min(value, ABSOLUTE_MAX_HEARTS));
        config().set("hearts.maximum", clamped);
        save();
        return clamped;
    }

    /** Clamps an arbitrary value into the absolute [MIN, MAX] hearts range. */
    private int clampToBounds(int value) {
        return Math.max(ABSOLUTE_MIN_HEARTS, Math.min(value, ABSOLUTE_MAX_HEARTS));
    }

    // ---------------------------------------------------------------------
    //  Spawn
    // ---------------------------------------------------------------------

    public boolean hasSpawn() {
        String world = config().getString("spawn.world", "");
        return world != null && !world.isEmpty() && plugin.getServer().getWorld(world) != null;
    }

    public Location getSpawn() {
        if (!hasSpawn()) {
            return null;
        }
        World world = plugin.getServer().getWorld(config().getString("spawn.world", ""));
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                config().getDouble("spawn.x"),
                config().getDouble("spawn.y"),
                config().getDouble("spawn.z"),
                (float) config().getDouble("spawn.yaw"),
                (float) config().getDouble("spawn.pitch"));
    }

    public void setSpawn(Location location) {
        config().set("spawn.world", location.getWorld().getName());
        config().set("spawn.x", location.getX());
        config().set("spawn.y", location.getY());
        config().set("spawn.z", location.getZ());
        config().set("spawn.yaw", location.getYaw());
        config().set("spawn.pitch", location.getPitch());
        save();
    }
}
