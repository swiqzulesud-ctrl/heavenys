package com.heaven.essentials.managers;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have god mode (invulnerability) enabled.
 * Session-scoped; god mode is intentionally not persisted across restarts.
 */
public final class GodManager {

    private final Set<UUID> godPlayers = ConcurrentHashMap.newKeySet();

    public boolean isGod(UUID uuid) {
        return godPlayers.contains(uuid);
    }

    /**
     * Toggles god mode for the player.
     *
     * @return the new state ({@code true} = god mode now enabled)
     */
    public boolean toggle(UUID uuid) {
        if (godPlayers.contains(uuid)) {
            godPlayers.remove(uuid);
            return false;
        }
        godPlayers.add(uuid);
        return true;
    }

    public void set(UUID uuid, boolean god) {
        if (god) {
            godPlayers.add(uuid);
        } else {
            godPlayers.remove(uuid);
        }
    }

    public void clear(UUID uuid) {
        godPlayers.remove(uuid);
    }
}
