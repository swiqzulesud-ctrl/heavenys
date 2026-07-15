package dev.heaven.essentials.util;

import dev.heaven.essentials.service.BackService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Teleport helper that records the origin for {@code /back} before moving
 * the player.
 */
public final class Teleports {

    private Teleports() {
    }

    /**
     * Records the player's current position in the back history and then
     * teleports them asynchronously (chunk loading off the main thread).
     */
    public static void teleport(BackService backService, Player player, Location destination) {
        backService.recordLocation(player);
        player.teleportAsync(destination);
    }
}
