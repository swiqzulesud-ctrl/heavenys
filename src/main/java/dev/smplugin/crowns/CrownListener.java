package dev.smplugin.crowns;

import dev.smplugin.SMPlugin;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Keeps crown presentation in sync with the player lifecycle and runs the
 * subtle white-ash aura around online crown holders.
 */
public final class CrownListener implements Listener {

    private final SMPlugin plugin;
    private final CrownManager crowns;

    public CrownListener(SMPlugin plugin, CrownManager crowns) {
        this.plugin = plugin;
        this.crowns = crowns;
        // Ambient aura tick for crown holders (every 2s, deliberately subtle).
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (crowns.isHolder(player.getUniqueId())) {
                    Fx.whiteAura(player.getLocation(), 0.6);
                }
            }
        }, 40L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Delay one tick so the player entity is fully spawned before mounting.
        Bukkit.getScheduler().runTask(plugin, () -> crowns.applyPresentation(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        crowns.removeHologram(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHolderDeath(PlayerDeathEvent event) {
        crowns.removeHologram(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> crowns.applyPresentation(event.getPlayer()));
    }

    /** Removes stray hologram entities left over from crashes. */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getPersistentDataContainer().has(Keys.CROWN_HOLO, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }
}
