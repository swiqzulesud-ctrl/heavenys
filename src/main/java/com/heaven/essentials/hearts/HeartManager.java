package com.heaven.essentials.hearts;

import com.heaven.essentials.HeavenEssentials;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Central authority for the Lifesteal heart system.
 *
 * <p>Hearts are stored per-player by UUID in {@code data.yml}, cached in memory
 * for fast access, and mirrored onto the live {@link Attribute#MAX_HEALTH}
 * attribute. All mutations are clamped into the configured bounds and persisted
 * asynchronously so no gameplay thread ever blocks on disk I/O.</p>
 */
public final class HeartManager {

    private final HeavenEssentials plugin;
    private final File dataFile;
    private final Map<UUID, Integer> cache = new ConcurrentHashMap<>();
    private final Object ioLock = new Object();

    private FileConfiguration data;

    public HeartManager(HeavenEssentials plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    /** Loads {@code data.yml} from disk into the in-memory cache. */
    public void load() {
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        cache.clear();

        ConfigurationSection players = data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    cache.put(UUID.fromString(key), players.getInt(key));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Skipping malformed UUID in data.yml: " + key);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    //  Queries
    // ---------------------------------------------------------------------

    public boolean isTracked(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public int getHearts(UUID uuid) {
        return cache.getOrDefault(uuid, plugin.configs().getDefaultHearts());
    }

    // ---------------------------------------------------------------------
    //  Mutations
    // ---------------------------------------------------------------------

    /** Ensures a brand-new player is initialised with the configured default hearts. */
    public void initialise(Player player) {
        UUID uuid = player.getUniqueId();
        if (!cache.containsKey(uuid)) {
            int def = clamp(plugin.configs().getDefaultHearts());
            cache.put(uuid, def);
            saveAsync();
        }
        apply(player);
    }

    /**
     * Sets a player's hearts to an exact value (clamped), persists the change,
     * and updates the live max-health attribute if the player is online.
     *
     * @return the value stored after clamping
     */
    public int setHearts(UUID uuid, int hearts) {
        int clamped = clamp(hearts);
        cache.put(uuid, clamped);
        saveAsync();

        Player online = plugin.getServer().getPlayer(uuid);
        if (online != null) {
            apply(online);
        }
        return clamped;
    }

    public int addHearts(UUID uuid, int amount) {
        return setHearts(uuid, getHearts(uuid) + amount);
    }

    public int removeHearts(UUID uuid, int amount) {
        return setHearts(uuid, getHearts(uuid) - amount);
    }

    /** True when the player is already at (or above) the configured maximum. */
    public boolean isAtMax(UUID uuid) {
        return getHearts(uuid) >= plugin.configs().getMaxHearts();
    }

    /** True when the player is already at (or below) the configured minimum. */
    public boolean isAtMin(UUID uuid) {
        return getHearts(uuid) <= plugin.configs().getMinHearts();
    }

    /**
     * Clamps every stored player down to the current maximum. Used when an
     * admin lowers the maximum via the GUI.
     *
     * @return the number of players whose hearts were reduced
     */
    public int clampAllToMax() {
        int max = plugin.configs().getMaxHearts();
        int affected = 0;
        for (Map.Entry<UUID, Integer> entry : cache.entrySet()) {
            if (entry.getValue() > max) {
                entry.setValue(max);
                affected++;
                Player online = plugin.getServer().getPlayer(entry.getKey());
                if (online != null) {
                    apply(online);
                }
            }
        }
        if (affected > 0) {
            saveAsync();
        }
        return affected;
    }

    /** Applies the cached heart total to the player's live max-health attribute. */
    public void apply(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double maxHealth = getHearts(player.getUniqueId()) * 2.0D;
        attribute.setBaseValue(maxHealth);
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    private int clamp(int hearts) {
        int min = plugin.configs().getMinHearts();
        int max = plugin.configs().getMaxHearts();
        return Math.max(min, Math.min(hearts, max));
    }

    // ---------------------------------------------------------------------
    //  Persistence
    // ---------------------------------------------------------------------

    /** Writes the cache to disk on an async thread. */
    public void saveAsync() {
        Map<UUID, Integer> snapshot = new HashMap<>(cache);
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> writeToDisk(snapshot));
        } else {
            writeToDisk(snapshot);
        }
    }

    /** Writes the cache to disk on the calling thread (used on shutdown). */
    public void saveSync() {
        writeToDisk(new HashMap<>(cache));
    }

    private void writeToDisk(Map<UUID, Integer> snapshot) {
        synchronized (ioLock) {
            FileConfiguration out = new YamlConfiguration();
            for (Map.Entry<UUID, Integer> entry : snapshot.entrySet()) {
                out.set("players." + entry.getKey(), entry.getValue());
            }
            try {
                out.save(dataFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player hearts to data.yml", ex);
            }
        }
    }
}
