package com.heaven.essentials.listeners;

import com.heaven.essentials.HeavenEssentials;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Records the location a player leaves behind whenever they teleport or die,
 * powering the {@code /back} command.
 */
public final class BackListener implements Listener {

    private final HeavenEssentials plugin;

    public BackListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // Ignore no-op teleports within the same block.
        if (event.getFrom().getWorld() != null
                && event.getFrom().distanceSquared(event.getTo()) < 1.0D
                && event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        plugin.back().setPrevious(event.getPlayer().getUniqueId(), event.getFrom());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        plugin.back().setPrevious(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }
}
