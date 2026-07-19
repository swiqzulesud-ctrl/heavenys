package com.heaven.essentials.listeners;

import com.heaven.essentials.HeavenEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Wires player sessions into the plugin: initialises hearts for new players,
 * re-applies stored hearts on join, and tidies up session state on quit.
 */
public final class ConnectionListener implements Listener {

    private final HeavenEssentials plugin;

    public ConnectionListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Initialise (new players get the default) and apply the max-health attribute.
        plugin.hearts().initialise(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.hearts().saveAsync();
        plugin.god().clear(event.getPlayer().getUniqueId());
        plugin.chat().clear(event.getPlayer().getUniqueId());
    }
}
