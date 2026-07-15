package dev.heaven.essentials.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last location of each player before a plugin teleport or a
 * death, powering {@code /back}.
 *
 * <p>Locations are intentionally kept in memory only; a fresh session starts
 * with a clean history.</p>
 */
public final class BackService {

    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    /** Records the player's current position as their /back target. */
    public void recordLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    /** Records an explicit location (used for deaths). */
    public void recordLocation(UUID uuid, Location location) {
        lastLocations.put(uuid, location.clone());
    }

    /** Returns the stored /back target, or {@code null} if none exists. */
    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    /** Clears the history of a player, e.g. when they disconnect. */
    public void clear(UUID uuid) {
        lastLocations.remove(uuid);
    }
}
