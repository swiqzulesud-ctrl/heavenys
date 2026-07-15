package dev.heaven.essentials.listener;

import dev.heaven.essentials.service.GodService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Cancels all damage and hunger loss for players with god mode enabled.
 */
public final class GodListener implements Listener {

    private final GodService godService;

    public GodListener(GodService godService) {
        this.godService = godService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && godService.isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getFoodLevel() < player.getFoodLevel()
                && godService.isGod(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
