package dev.smplugin.relic;

import dev.smplugin.SMPlugin;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Event glue for the Sovereign's Relic: participation tracking, the loot-drop
 * moment, relic-loss detection and the axe's white particle trail.
 */
public final class RelicListener implements Listener {

    private final SMPlugin plugin;
    private final RelicManager relic;

    public RelicListener(SMPlugin plugin, RelicManager relic) {
        this.plugin = plugin;
        this.relic = relic;
        // Subtle END_ROD trail while the relic is held in either hand.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (RelicItems.isRelic(player.getInventory().getItemInMainHand())
                        || RelicItems.isRelic(player.getInventory().getItemInOffHand())) {
                    player.getWorld().spawnParticle(Particle.END_ROD,
                            player.getLocation().add(0, 1.1, 0), 2, 0.25, 0.35, 0.25, 0.01);
                }
            }
        }, 10L, 10L);
    }

    // -------------------------------------------------------- participation

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onGuardianDamaged(EntityDamageByEntityEvent event) {
        if (!relic.isGuardian(event.getEntity())) {
            return;
        }
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker != null) {
            relic.recordDamage(attacker, event.getFinalDamage());
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** A swing with the relic itself leaves a stronger white flourish. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRelicSwing(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player
                && RelicItems.isRelic(player.getInventory().getItemInMainHand())) {
            event.getEntity().getWorld().spawnParticle(Particle.END_ROD,
                    event.getEntity().getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.08);
        }
    }

    // --------------------------------------------------------------- death

    @EventHandler
    public void onGuardianDeath(EntityDeathEvent event) {
        if (relic.isGuardianAdd(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            return;
        }
        if (!relic.isGuardian(event.getEntity())) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(250);
        // Sweep away any surviving adds — the fight is over.
        event.getEntity().getWorld()
                .getNearbyEntities(event.getEntity().getLocation(), 64, 32, 64).stream()
                .filter(relic::isGuardianAdd)
                .forEach(Entity::remove);
        relic.onGuardianDeath(event.getEntity().getLocation());
    }

    // ---------------------------------------------------------- relic loss

    /** Removal causes that mean the axe is gone for good (not picked up/unloaded). */
    private static final Set<EntityRemoveEvent.Cause> DESTRUCTION_CAUSES = EnumSet.of(
            EntityRemoveEvent.Cause.DEATH,        // lava, fire, cactus, explosions
            EntityRemoveEvent.Cause.DESPAWN,      // despawn timer (disabled, but just in case)
            EntityRemoveEvent.Cause.OUT_OF_WORLD, // thrown into the void
            EntityRemoveEvent.Cause.EXPLODE);

    @EventHandler
    public void onRelicItemRemoved(EntityRemoveEvent event) {
        if (event.getEntity() instanceof Item item
                && DESTRUCTION_CAUSES.contains(event.getCause())
                && RelicItems.isRelic(item.getItemStack())) {
            relic.onRelicDestroyed();
        }
    }

    // ------------------------------------------------------------- pick up

    /** The scramble is decided: announce whoever grabs the axe. */
    @EventHandler(ignoreCancelled = true)
    public void onRelicPickup(PlayerAttemptPickupItemEvent event) {
        if (!RelicItems.isRelic(event.getItem().getItemStack())) {
            return;
        }
        Player player = event.getPlayer();
        Text.broadcast("<gold>✦</gold> <white><bold>" + player.getName()
                + "</bold> has claimed the <gold>Crown-Splitter Axe</gold>!</white>");
        Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
        Fx.whiteBurst(player.getLocation());
    }
}
