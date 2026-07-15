package dev.heaven.essentials.lifesteal;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.lifesteal.gui.HeartsGui;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Transfers hearts from the victim to the killer on player-versus-player
 * kills.
 *
 * <p>{@link Player#getKiller()} is only non-null when a player dealt the
 * killing blow, so deaths from mobs, lava, fall damage, explosions,
 * drowning, the void and every other environmental source never transfer
 * hearts. Self-kills are ignored as well.</p>
 */
public final class LifestealListener implements Listener {

    private final HeavenEssentials plugin;

    public LifestealListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        HeartManager hearts = plugin.heartManager();
        if (!hearts.isLifestealEnabled()) {
            return;
        }

        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        int perKill = hearts.heartsPerKill();
        int victimBefore = hearts.getHearts(victim.getUniqueId());
        int killerBefore = hearts.getHearts(killer.getUniqueId());

        // The victim can only give what they have above the minimum, and the
        // killer can only receive up to the configured maximum.
        int victimGives = Math.min(perKill, victimBefore - hearts.minHearts());
        int killerGains = Math.min(perKill, hearts.maxHearts() - killerBefore);
        int transferred = Math.min(victimGives, killerGains);

        if (victimGives <= 0) {
            plugin.messages().send(killer, "lifesteal.victim-at-minimum",
                    Placeholder.unparsed("victim", victim.getName()));
            return;
        }
        if (killerGains <= 0) {
            plugin.messages().send(killer, "lifesteal.max-reached",
                    Placeholder.unparsed("max", String.valueOf(hearts.maxHearts())));
            return;
        }

        // setHearts saves instantly, so both sides persist even on a crash.
        hearts.setHearts(victim.getUniqueId(), victimBefore - transferred);
        hearts.setHearts(killer.getUniqueId(), killerBefore + transferred);

        plugin.messages().send(killer, "lifesteal.gain",
                Placeholder.unparsed("hearts", String.valueOf(transferred)),
                Placeholder.unparsed("victim", victim.getName()));
        plugin.messages().send(victim, "lifesteal.lose",
                Placeholder.unparsed("hearts", String.valueOf(transferred)),
                Placeholder.unparsed("killer", killer.getName()));

        // Keep any open admin GUIs in sync with the world.
        HeartsGui gui = plugin.heartsGui();
        gui.refreshOpenViews();
    }
}
