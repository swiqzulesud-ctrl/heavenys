package com.heaven.essentials.managers;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the last significant location for each player so {@code /back} can
 * return them to where they were before their most recent teleport or death.
 * Session-scoped; not persisted across restarts.
 */
public final class BackManager {

    private final Map<UUID, Location> previous = new ConcurrentHashMap<>();

    public void setPrevious(UUID uuid, Location location) {
        if (location != null && location.getWorld() != null) {
            previous.put(uuid, location.clone());
        }
    }

    public Location getPrevious(UUID uuid) {
        Location location = previous.get(uuid);
        return location == null ? null : location.clone();
    }

    public boolean has(UUID uuid) {
        return previous.containsKey(uuid);
    }

    public void clear(UUID uuid) {
        previous.remove(uuid);
    }
}
