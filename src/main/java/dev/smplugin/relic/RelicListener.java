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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;

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

    /** A dropped relic timed out on the ground without anyone claiming it. */
    @EventHandler
    public void onRelicDespawn(ItemDespawnEvent event) {
        RelicBoss boss = RelicItems.relicBoss(event.getEntity().getItemStack());
        if (boss != null) {
            relic.onRelicDestroyed(boss);
        }
    }

    /**
     * Lava, fire, cactus, explosions and the void all hurt item entities.
     * The check runs one tick later: only if the hit actually destroyed the
     * item does it count as lost.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRelicItemDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }
        RelicBoss boss = RelicItems.relicBoss(item.getItemStack());
        if (boss == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!item.isValid()) {
                relic.onRelicDestroyed(boss);
            }
        });
    }

    /** Sweeps up boss entities orphaned by a crash mid-fight. */
    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if ((relic.isGuardian(entity) && !relic.isCurrentGuardian(entity))
                    || (relic.isGuardianAdd(entity) && !relic.isFightActive())) {
                entity.remove();
            }
        }
    }

    // ------------------------------------------------------------- pick up

    /** The scramble is decided: announce whoever grabs the relic. */
    @EventHandler(ignoreCancelled = true)
    public void onRelicPickup(EntityPickupItemEvent event) {
        RelicBoss boss = RelicItems.relicBoss(event.getItem().getItemStack());
        if (boss == null) {
            return;
        }
        relic.markDropClaimed(event.getItem());
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Text.broadcast("<gold>✦</gold> <white><bold>" + player.getName()
                + "</bold> s'est emparé de la relique <gold>" + boss.relicName() + "</gold> !</white>");
        Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
        Fx.whiteBurst(player.getLocation());
    }

    /** A hopper/container swallowed the relic: it still exists, stop watching it. */
    @EventHandler(ignoreCancelled = true)
    public void onRelicHopperPickup(org.bukkit.event.inventory.InventoryPickupItemEvent event) {
        if (RelicItems.isRelic(event.getItem().getItemStack())) {
            relic.markDropClaimed(event.getItem());
        }
    }
}
