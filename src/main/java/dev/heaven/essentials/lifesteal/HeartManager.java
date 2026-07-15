package dev.heaven.essentials.lifesteal;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.data.DataStore;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Owns every Lifesteal heart value.
 *
 * <p>Hearts are stored by UUID in {@link DataStore} and applied to online
 * players through {@link Attribute#MAX_HEALTH} (1 heart = 2 health points).
 * Every mutation clamps the value to the configured bounds and saves
 * immediately so the data survives disconnects, reloads and restarts.</p>
 */
public final class HeartManager {

    /** Health points represented by a single heart. */
    public static final int HEALTH_PER_HEART = 2;

    /** Hard ceiling for the configurable maximum, keeps the GUI sane. */
    public static final int ABSOLUTE_MAX_HEARTS = 100;

    private final HeavenEssentials plugin;
    private final DataStore dataStore;

    public HeartManager(HeavenEssentials plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    // ------------------------------------------------------------------
    // Configuration accessors
    // ------------------------------------------------------------------

    /** Whether the Lifesteal system transfers hearts on player kills. */
    public boolean isLifestealEnabled() {
        return plugin.getConfig().getBoolean("lifesteal.enabled", true);
    }

    /** Hearts a brand new player starts with. */
    public int defaultHearts() {
        return clampToBounds(plugin.getConfig().getInt("lifesteal.default-hearts", 10));
    }

    /** The lowest number of hearts any player can be reduced to. */
    public int minHearts() {
        return Math.max(1, plugin.getConfig().getInt("lifesteal.min-hearts", 1));
    }

    /** The highest number of hearts any player can reach. */
    public int maxHearts() {
        int configured = plugin.getConfig().getInt("lifesteal.max-hearts", 20);
        return Math.max(minHearts(), Math.min(ABSOLUTE_MAX_HEARTS, configured));
    }

    /** Hearts transferred from victim to killer per kill. */
    public int heartsPerKill() {
        return Math.max(1, plugin.getConfig().getInt("lifesteal.hearts-per-kill", 1));
    }

    /**
     * Updates the configured maximum hearts, saves config.yml instantly and
     * clamps every stored player (online and offline) above the new limit.
     *
     * @return the value that was actually applied after clamping
     */
    public int setMaxHearts(int newMax) {
        int applied = Math.max(minHearts(), Math.min(ABSOLUTE_MAX_HEARTS, newMax));
        plugin.getConfig().set("lifesteal.max-hearts", applied);
        plugin.saveConfig();
        clampAllPlayers();
        return applied;
    }

    // ------------------------------------------------------------------
    // Heart values
    // ------------------------------------------------------------------

    /** Returns the stored hearts for a UUID, falling back to the default. */
    public int getHearts(UUID uuid) {
        return clampToBounds(dataStore.getHearts(uuid, defaultHearts()));
    }

    /**
     * Stores a heart value for a UUID (clamped to the configured bounds),
     * saves immediately and re-applies max health if the player is online.
     *
     * @return the value that was actually stored after clamping
     */
    public int setHearts(UUID uuid, int hearts) {
        int clamped = clampToBounds(hearts);
        dataStore.setHearts(uuid, clamped);
        dataStore.save();

        Player online = plugin.getServer().getPlayer(uuid);
        if (online != null) {
            applyHearts(online);
        }
        return clamped;
    }

    /** Adds (or with a negative amount, removes) hearts for a UUID. */
    public int addHearts(UUID uuid, int amount) {
        return setHearts(uuid, getHearts(uuid) + amount);
    }

    /**
     * Ensures a player has a stored heart value (creating it with the default
     * for first-time joins) and applies it to their max health attribute.
     */
    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (!dataStore.hasHearts(uuid)) {
            dataStore.setHearts(uuid, defaultHearts());
            dataStore.save();
        }
        applyHearts(player);
    }

    /**
     * Applies the stored heart value to the player's
     * {@link Attribute#MAX_HEALTH} base value, clamping current health so it
     * never exceeds the new maximum.
     */
    public void applyHearts(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            plugin.getLogger().warning("Player " + player.getName() + " has no MAX_HEALTH attribute.");
            return;
        }
        double maxHealth = (double) getHearts(player.getUniqueId()) * HEALTH_PER_HEART;
        attribute.setBaseValue(maxHealth);
        if (player.getHealth() > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    /**
     * Clamps every stored player to the current bounds. Called after the
     * maximum is lowered through the admin GUI or a reload.
     */
    public void clampAllPlayers() {
        boolean changed = false;
        int max = maxHearts();
        int min = minHearts();
        for (UUID uuid : dataStore.storedPlayers()) {
            int stored = dataStore.getHearts(uuid, defaultHearts());
            int clamped = Math.max(min, Math.min(max, stored));
            if (clamped != stored) {
                dataStore.setHearts(uuid, clamped);
                changed = true;
            }
        }
        if (changed) {
            dataStore.save();
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyHearts(player);
        }
    }

    private int clampToBounds(int hearts) {
        int min = Math.max(1, plugin.getConfig().getInt("lifesteal.min-hearts", 1));
        int max = Math.max(min, Math.min(ABSOLUTE_MAX_HEARTS,
                plugin.getConfig().getInt("lifesteal.max-hearts", 20)));
        return Math.max(min, Math.min(max, hearts));
    }
}
