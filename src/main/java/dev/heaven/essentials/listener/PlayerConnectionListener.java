package dev.heaven.essentials.listener;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Applies stored Lifesteal hearts on join and cleans up session-scoped
 * state (god mode, /back history, /reply targets) on quit.
 */
public final class PlayerConnectionListener implements Listener {

    private final HeavenEssentials plugin;

    public PlayerConnectionListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        plugin.heartManager().initializePlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.godService().clear(uuid);
        plugin.backService().clear(uuid);
        plugin.privateMessageService().clearSession(uuid);
    }
}
