package dev.heaven.essentials.listener;

import dev.heaven.essentials.service.BackService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Records death locations so {@code /back} can return players to where
 * they fell.
 */
public final class BackListener implements Listener {

    private final BackService backService;

    public BackListener(BackService backService) {
        this.backService = backService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        backService.recordLocation(event.getPlayer().getUniqueId(),
                event.getPlayer().getLocation());
    }
}
