package com.heaven.essentials.listeners;

import com.heaven.essentials.HeavenEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Cancels all incoming damage to players who currently have god mode enabled.
 */
public final class GodListener implements Listener {

    private final HeavenEssentials plugin;

    public GodListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.god().isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
