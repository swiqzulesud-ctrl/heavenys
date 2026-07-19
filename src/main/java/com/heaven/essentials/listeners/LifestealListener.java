package com.heaven.essentials.listeners;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.config.Messages;
import com.heaven.essentials.hearts.HeartManager;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/**
 * Implements the core Lifesteal rule: a heart is transferred only when a player
 * is killed by another player. Environmental deaths (mobs, lava, fall, void,
 * drowning, explosions, ...) never move hearts because {@link Player#getKiller()}
 * returns {@code null} for them.
 */
public final class LifestealListener implements Listener {

    private final HeavenEssentials plugin;

    public LifestealListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Only player-vs-player kills transfer hearts, and never self-kills.
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        HeartManager hearts = plugin.hearts();
        Messages messages = plugin.messages();
        int perKill = plugin.configs().getHeartsPerKill();

        UUID victimId = victim.getUniqueId();
        UUID killerId = killer.getUniqueId();

        // Victim loses hearts (never below the configured minimum).
        if (!hearts.isAtMin(victimId)) {
            int remaining = hearts.removeHearts(victimId, perKill);
            messages.send(victim, "lifesteal.lost",
                    Messages.ph("killer", killer.getName()),
                    Messages.ph("hearts", String.valueOf(remaining)));
        }

        // Killer gains hearts (never above the configured maximum).
        if (hearts.isAtMax(killerId)) {
            messages.send(killer, "lifesteal.at-maximum", Messages.ph("victim", victim.getName()));
        } else {
            int total = hearts.addHearts(killerId, perKill);
            messages.send(killer, "lifesteal.gained",
                    Messages.ph("victim", victim.getName()),
                    Messages.ph("hearts", String.valueOf(total)));
            playEffects(killer, victim);
        }
    }

    /** Reapplies the stored heart total one tick after respawn. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.hearts().apply(player));
    }

    private void playEffects(Player killer, Player victim) {
        if (!plugin.configs().areEffectsEnabled()) {
            return;
        }
        plugin.audiences().player(killer).playSound(Sound.sound(Key.key("minecraft:entity.player.levelup"),
                Sound.Source.PLAYER, 0.7f, 1.4f));
        plugin.audiences().player(victim).playSound(Sound.sound(Key.key("minecraft:entity.wither.hurt"),
                Sound.Source.PLAYER, 0.5f, 1.2f));
    }
}
