package dev.heaven.essentials.data;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Thin wrapper around data.yml, the plugin's persistent storage for player
 * hearts, the global spawn point and private-message ignore lists.
 *
 * <p>Every mutating method here is paired with a call to {@link #save()} by
 * the owning manager so data survives crashes, reloads and restarts.</p>
 */
public final class DataStore {

    private static final String PLAYERS_ROOT = "players";
    private static final String IGNORES_ROOT = "ignores";
    private static final String SPAWN_KEY = "spawn";

    private final HeavenEssentials plugin;
    private final File file;
    private FileConfiguration data;

    public DataStore(HeavenEssentials plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        reload();
    }

    /** Re-reads data.yml from disk, creating it from the jar if absent. */
    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("data.yml", false);
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    /** Writes the current state to disk immediately. */
    public void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", exception);
        }
    }

    // ------------------------------------------------------------------
    // Hearts
    // ------------------------------------------------------------------

    /** Returns whether the given player has a stored heart value. */
    public boolean hasHearts(UUID uuid) {
        return data.contains(PLAYERS_ROOT + "." + uuid + ".hearts");
    }

    /** Returns the stored heart value, or {@code fallback} if none exists. */
    public int getHearts(UUID uuid, int fallback) {
        return data.getInt(PLAYERS_ROOT + "." + uuid + ".hearts", fallback);
    }

    /** Stores a heart value for the given player (does not save). */
    public void setHearts(UUID uuid, int hearts) {
        data.set(PLAYERS_ROOT + "." + uuid + ".hearts", hearts);
    }

    /** Returns the UUIDs of every player with stored heart data. */
    public Set<UUID> storedPlayers() {
        ConfigurationSection section = data.getConfigurationSection(PLAYERS_ROOT);
        Set<UUID> uuids = new HashSet<>();
        if (section == null) {
            return uuids;
        }
        for (String key : section.getKeys(false)) {
            try {
                uuids.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID in data.yml: " + key);
            }
        }
        return uuids;
    }

    // ------------------------------------------------------------------
    // Spawn
    // ------------------------------------------------------------------

    /** Returns the global spawn location, or {@code null} if not set. */
    public Location getSpawn() {
        return data.getLocation(SPAWN_KEY);
    }

    /** Stores the global spawn location and saves immediately. */
    public void setSpawn(Location location) {
        data.set(SPAWN_KEY, location);
        save();
    }

    // ------------------------------------------------------------------
    // Ignore lists
    // ------------------------------------------------------------------

    /** Returns a mutable copy of the ignore list for the given player. */
    public Set<UUID> getIgnored(UUID uuid) {
        List<String> stored = data.getStringList(IGNORES_ROOT + "." + uuid);
        Set<UUID> ignored = new HashSet<>();
        for (String entry : stored) {
            try {
                ignored.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignoredException) {
                plugin.getLogger().warning("Ignoring invalid UUID in ignore list: " + entry);
            }
        }
        return ignored;
    }

    /** Persists the ignore list for the given player and saves immediately. */
    public void setIgnored(UUID uuid, Set<UUID> ignored) {
        if (ignored.isEmpty()) {
            data.set(IGNORES_ROOT + "." + uuid, null);
        } else {
            data.set(IGNORES_ROOT + "." + uuid, ignored.stream().map(UUID::toString).sorted().toList());
        }
        save();
    }
}
