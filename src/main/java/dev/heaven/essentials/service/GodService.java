package dev.heaven.essentials.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have god mode enabled through {@code /god}.
 *
 * <p>God mode is session-scoped by design: it resets when a player leaves so
 * nobody stays permanently invulnerable by accident.</p>
 */
public final class GodService {

    private final Set<UUID> godPlayers = ConcurrentHashMap.newKeySet();

    /** Returns whether god mode is active for the given player. */
    public boolean isGod(UUID uuid) {
        return godPlayers.contains(uuid);
    }

    /**
     * Toggles god mode for the given player.
     *
     * @return the new state ({@code true} = enabled)
     */
    public boolean toggle(UUID uuid) {
        if (godPlayers.remove(uuid)) {
            return false;
        }
        godPlayers.add(uuid);
        return true;
    }

    /** Removes a player from the god list, e.g. when they disconnect. */
    public void clear(UUID uuid) {
        godPlayers.remove(uuid);
    }
}
