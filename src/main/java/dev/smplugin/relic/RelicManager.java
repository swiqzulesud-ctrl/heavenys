package dev.smplugin.relic;

import dev.smplugin.SMPlugin;
import dev.smplugin.data.Database;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * L'événement de la Relique Souveraine : programme et anime le combat contre
 * le Gardien Souverain, suit la participation et fait tomber l'unique
 * Hache Fend-Couronne.
 *
 * <p>Le Gardien apparaît à un endroit aléatoire de la carte (dans un rayon
 * configurable autour du spawn du monde, ~200 blocs par défaut) et ses
 * coordonnées exactes sont annoncées dans le chat au moment de son arrivée.</p>
 *
 * <p>La relique est strictement unique : un indicateur en base de données
 * enregistre son existence. Tant qu'elle existe, le Gardien ne peut pas être
 * invoqué ; quand la hache est détruite, l'indicateur s'efface et
 * l'événement redevient disponible.</p>
 */
public final class RelicManager {

    /** A row in the participation log. */
    public record Participant(String name, double damage) {
    }

    private final SMPlugin plugin;
    private final Database database;

    // Scheduling state.
    private long nextScheduledSpawn;   // millis; 0 = no schedule
    private long spawnAt;              // millis; 0 = no countdown running
    private final Set<Integer> firedWarnings = new HashSet<>();

    // Active fight state.
    private WitherSkeleton guardian;
    private BossBar bossBar;
    private BukkitTask fightTask;
    private BukkitTask auraTask;
    /** Where the current (or last) fight takes place; chunk tickets center. */
    private Location fightCenter;
    private long eventStart;
    private boolean enraged;
    private int addsSpawned;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final Map<UUID, String> participantNames = new HashMap<>();
    private final Set<UUID> gearWarned = new HashSet<>();

    private boolean relicExists;

    // The dropped axe currently lying on the ground (null once picked up or
    // destroyed). Tracked so silent removals (/kill, mods) are detected too.
    private Item trackedDrop;
    private boolean dropClaimed;

    public RelicManager(SMPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    // ------------------------------------------------------------ lifecycle

    /** Restores the relic flag + schedule and starts the clock. */
    public void load() {
        database.querySync(conn -> {
            relicExists = "true".equals(Database.getMeta(conn, "relic_exists"));
            String next = Database.getMeta(conn, "relic_next_spawn");
            nextScheduledSpawn = next == null ? 0 : Long.parseLong(next);
            return null;
        });
        if (scheduleEnabled() && nextScheduledSpawn == 0) {
            scheduleNext();
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    /** Aborts any active fight. Called on disable. */
    public void unload() {
        if (isFightActive()) {
            despawnGuardian(false);
        }
    }

    private boolean scheduleEnabled() {
        return plugin.getConfig().getBoolean("relic.enabled", true)
                && plugin.getConfig().getBoolean("relic.schedule.enabled", true);
    }

    private void scheduleNext() {
        long hours = Math.max(1, plugin.getConfig().getLong("relic.schedule.interval-hours", 336));
        nextScheduledSpawn = System.currentTimeMillis() + hours * 3_600_000L;
        long value = nextScheduledSpawn;
        database.runAsync(conn -> Database.setMeta(conn, "relic_next_spawn", String.valueOf(value)));
    }

    // ------------------------------------------------------------ schedule

    /** One-second clock: drives the schedule and the pre-spawn countdown. */
    private void tick() {
        long now = System.currentTimeMillis();

        // Failsafe: if the boss entity died without our death handler running
        // (shouldn't happen, but never leave the event stuck "active").
        if (guardian != null && guardian.isDead()) {
            despawnGuardian(true);
        }

        // Scheduled events kick off their countdown 10 minutes ahead of time.
        if (scheduleEnabled() && nextScheduledSpawn > 0 && spawnAt == 0 && !isFightActive()
                && now >= nextScheduledSpawn - 600_000L) {
            if (relicExists) {
                // Relic still in the world: push the schedule one interval out.
                scheduleNext();
            } else {
                beginCountdown(nextScheduledSpawn);
                scheduleNext();
            }
            return;
        }

        if (spawnAt == 0) {
            return;
        }
        long secondsLeft = (spawnAt - now) / 1000;
        if (secondsLeft <= 0) {
            spawnAt = 0;
            firedWarnings.clear();
            spawnGuardian();
            return;
        }
        for (int mark : new int[]{600, 300, 60}) {
            if (secondsLeft <= mark && !firedWarnings.contains(mark)) {
                firedWarnings.add(mark);
                broadcastWarning(secondsLeft);
            }
        }
    }

    private void beginCountdown(long spawnTime) {
        spawnAt = Math.max(spawnTime, System.currentTimeMillis() + 1000);
        firedWarnings.clear();
    }

    private void broadcastWarning(long secondsLeft) {
        String when = Text.duration(secondsLeft);
        Text.broadcast("<gold>⚠</gold> <white>Un <gold>Gardien Souverain</gold> s'éveille... "
                + "préparez-vous. <gray>(arrivée dans <white>" + when + "</white> — position révélée "
                + "à son apparition)</gray></white>");
        Text.broadcastTitle("<white>⚠ <gold>Un Gardien Souverain s'éveille...</gold> ⚠</white>",
                "<white>Préparez-vous — il reste <gold>" + when + "</gold>");
        Bukkit.getOnlinePlayers().forEach(Fx::ominous);
    }

    /**
     * Admin trigger. {@code immediate} skips the 10-minute build-up.
     * Returns an error string, or null on success.
     */
    public String summon(boolean immediate) {
        if (!plugin.getConfig().getBoolean("relic.enabled", true)) {
            return "L'événement de la Relique Souveraine est désactivé dans la configuration.";
        }
        if (relicExists) {
            return "La Hache Fend-Couronne existe déjà — il ne peut y en avoir qu'une. "
                    + "Le Gardien ne peut renaître tant qu'elle n'est pas détruite.";
        }
        if (isFightActive()) {
            return "Le Gardien Souverain arpente déjà le monde.";
        }
        if (spawnAt != 0) {
            return "Un Gardien est déjà en route — arrivée dans "
                    + Text.duration((spawnAt - System.currentTimeMillis()) / 1000) + ".";
        }
        if (spawnWorld() == null) {
            return "Le monde configuré (relic.spawn.world) n'est pas chargé.";
        }
        if (immediate) {
            spawnGuardian();
        } else {
            beginCountdown(System.currentTimeMillis() + 600_000L);
            broadcastWarning(600);
        }
        return null;
    }

    public boolean relicExists() {
        return relicExists;
    }

    public boolean isFightActive() {
        return guardian != null && !guardian.isDead();
    }

    private void setRelicExists(boolean exists) {
        relicExists = exists;
        database.runAsync(conn -> Database.setMeta(conn, "relic_exists", String.valueOf(exists)));
    }

    /** Called by the listener when the dropped axe is destroyed or despawns. */
    public void onRelicDestroyed() {
        if (!relicExists) {
            return; // already handled (e.g. despawn event + drop tracker)
        }
        trackedDrop = null;
        setRelicExists(false);
        Text.broadcast("<white>La <gold>Hache Fend-Couronne</gold> a été perdue à jamais... "
                + "le <gold>Gardien Souverain</gold> peut renaître.</white>");
    }

    /** Called by the listener when any entity picks the dropped axe up. */
    public void markDropClaimed() {
        dropClaimed = true;
        trackedDrop = null;
    }

    /**
     * Admin escape hatch: clears the one-copy flag when the axe was lost in a
     * way the plugin cannot observe (silent removal by a command or mod).
     * Returns an error string, or null on success.
     */
    public String adminResetRelic() {
        if (!relicExists) {
            return "La Hache Fend-Couronne n'est pas marquée comme existante — rien à réinitialiser.";
        }
        trackedDrop = null;
        setRelicExists(false);
        Text.broadcast("<white>La trace de la <gold>Hache Fend-Couronne</gold> a été effacée des "
                + "annales... le <gold>Gardien Souverain</gold> peut renaître.</white>");
        return null;
    }

    // ---------------------------------------------------------- spawn point

    private World spawnWorld() {
        return Bukkit.getWorld(plugin.getConfig().getString("relic.spawn.world", "world"));
    }

    /**
     * Picks a random surface location within {@code relic.spawn.radius}
     * blocks (~200 by default) of the world spawn.
     */
    private Location pickSpawnLocation() {
        World world = spawnWorld();
        if (world == null) {
            return null;
        }
        double radius = Math.max(16, plugin.getConfig().getDouble("relic.spawn.radius", 200));
        Location center = world.getSpawnLocation();
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        // sqrt keeps the distribution uniform over the disc's area.
        double distance = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * radius;
        int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
        int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
        world.getChunkAt(x >> 4, z >> 4).load();
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    // ------------------------------------------------------------ the boss

    private void spawnGuardian() {
        Location spot = pickSpawnLocation();
        if (spot == null) {
            plugin.getLogger().severe("Événement de la relique annulé : le monde d'apparition n'est pas chargé.");
            return;
        }
        fightCenter = spot.clone();
        // Keep the fight area loaded and ticking for the whole fight, even
        // with nobody nearby — otherwise the boss unloads mid-event.
        setFightChunkTickets(fightCenter, true);

        var cfg = plugin.getConfig();
        double health = cfg.getDouble("relic.boss.health", 420.0);

        guardian = spot.getWorld().spawn(spot, WitherSkeleton.class, boss -> {
            boss.setCustomName(Text.legacy("<white>Gardien <gold>Souverain</gold></white>"));
            boss.setCustomNameVisible(false);
            boss.setPersistent(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCanPickupItems(false);
            boss.getPersistentDataContainer().set(Keys.GUARDIAN, PersistentDataType.BYTE, (byte) 1);

            setAttr(boss, Attribute.MAX_HEALTH, health);
            boss.setHealth(health);
            setAttr(boss, Attribute.ATTACK_DAMAGE, cfg.getDouble("relic.boss.attack-damage", 18.0));
            setAttr(boss, Attribute.KNOCKBACK_RESISTANCE, cfg.getDouble("relic.boss.knockback-resistance", 1.0));
            setAttr(boss, Attribute.MOVEMENT_SPEED, cfg.getDouble("relic.boss.movement-speed", 0.33));
            setAttr(boss, Attribute.ARMOR, cfg.getDouble("relic.boss.armor", 14.0));
            setAttr(boss, Attribute.FOLLOW_RANGE, 64.0);

            var equipment = boss.getEquipment();
            if (equipment != null) {
                equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
                equipment.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                equipment.setItemInMainHandDropChance(0f);
                equipment.setHelmetDropChance(0f);
            }
        });

        eventStart = System.currentTimeMillis();
        enraged = false;
        addsSpawned = 0;
        damageDealt.clear();
        participantNames.clear();
        gearWarned.clear();

        bossBar = Bukkit.createBossBar(Text.legacy("<white><bold>Gardien Souverain</bold></white>"),
                BarColor.WHITE, BarStyle.SEGMENTED_10);

        // Arrival fanfare — the exact coordinates go out in chat.
        Text.broadcast("<gold>⚠</gold> <white>Le <gold>Gardien Souverain</gold> est apparu en "
                + "<gold>" + spot.getBlockX() + ", " + spot.getBlockY() + ", " + spot.getBlockZ()
                + "</gold> <gray>(" + spot.getWorld().getName() + ")</gray> ! "
                + "Le Netherite complet est fortement conseillé.</white>");
        Text.broadcastTitle("<gold><bold>LE GARDIEN SOUVERAIN</bold></gold>",
                "<white>est apparu en <gold>" + spot.getBlockX() + ", " + spot.getBlockZ()
                        + "</gold> — réclamez la relique, si vous l'osez.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1f, 0.6f);
        }
        spot.getWorld().strikeLightningEffect(spot);
        Fx.whiteBurst(spot);

        startFightTasks();
    }

    private void setAttr(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private double fightRadius() {
        return plugin.getConfig().getDouble("relic.spawn.fight-radius", 48);
    }

    private void startFightTasks() {
        int aoeInterval = Math.max(4, plugin.getConfig().getInt("relic.boss.aoe-interval-seconds", 18));

        // Main fight loop (1s): boss bar, viewers, gear warnings, AOE timer, enrage.
        final int[] seconds = {0};
        fightTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (guardian == null || !guardian.isValid()) {
                return; // death handler / failsafe tears everything down
            }
            seconds[0]++;
            updateBossBar(fightRadius());
            warnUnderGeared(fightRadius());
            if (seconds[0] % aoeInterval == 0) {
                aoeWave();
            }
            checkEnrage();
        }, 20L, 20L);

        // White END_ROD spiral aura, ticked frequently for smooth motion.
        final double[] angle = {0};
        auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (guardian == null || !guardian.isValid()) {
                return;
            }
            angle[0] += 0.35;
            Location base = guardian.getLocation();
            Fx.spiralTick(base, angle[0], 1.3, 0.4 + (Math.sin(angle[0] * 0.5) + 1) * 1.1);
            Fx.spiralTick(base, angle[0] + Math.PI, 1.3, 0.4 + (Math.cos(angle[0] * 0.5) + 1) * 1.1);
            Fx.whiteAura(base, 0.8);
        }, 2L, 2L);
    }

    private void updateBossBar(double radius) {
        AttributeInstance max = guardian.getAttribute(Attribute.MAX_HEALTH);
        double progress = max == null ? 0 : guardian.getHealth() / max.getValue();
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        double visibleRange = Math.max(radius * 2, 96);
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean near = player.getWorld().equals(guardian.getWorld())
                    && player.getLocation().distanceSquared(guardian.getLocation()) <= visibleRange * visibleRange;
            if (near) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    /** Soft warning (never a block) for players approaching without full Netherite. */
    private void warnUnderGeared(double radius) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(guardian.getWorld())
                    || player.getLocation().distanceSquared(guardian.getLocation()) > radius * radius
                    || gearWarned.contains(player.getUniqueId())
                    || hasFullNetherite(player)) {
                continue;
            }
            gearWarned.add(player.getUniqueId());
            Text.msg(player, "<gold>⚠</gold> <white>Vous approchez du Gardien sans armure en "
                    + "<gold>Netherite</gold> complète. Il sera sans pitié — à vos risques et périls.</white>");
            Text.title(player, "<gold>⚠</gold>", "<white>Vous n'êtes pas équipé pour ce combat.");
            Fx.ominous(player);
        }
    }

    private boolean hasFullNetherite(Player player) {
        var inv = player.getInventory();
        return isNetherite(inv.getHelmet()) && isNetherite(inv.getChestplate())
                && isNetherite(inv.getLeggings()) && isNetherite(inv.getBoots());
    }

    private boolean isNetherite(ItemStack piece) {
        return piece != null && piece.getType().name().startsWith("NETHERITE_");
    }

    /** Lightning + summoned adds, the Guardian's periodic area attack. */
    private void aoeWave() {
        Location center = guardian.getLocation();
        double aoeDamage = plugin.getConfig().getDouble("relic.boss.aoe-damage", 7.0);
        World world = center.getWorld();

        world.strikeLightningEffect(center);
        // FLASH requires a Color data value on 26.x.
        world.spawnParticle(org.bukkit.Particle.FLASH, center.clone().add(0, 1, 0), 2,
                0, 0, 0, 0, org.bukkit.Color.WHITE);
        Fx.whiteBurst(center);
        for (Entity nearby : world.getNearbyEntities(center, 6, 6, 6)) {
            if (!(nearby instanceof Player player)) {
                continue;
            }
            player.damage(aoeDamage, guardian);
            player.setVelocity(player.getLocation().toVector().subtract(center.toVector())
                    .normalize().multiply(0.8).setY(0.4));
            Text.actionBar(player, "<gold>⚠ La fureur du Gardien s'abat sur vous !</gold>");
        }

        // Summon minor adds up to the configured ceiling.
        int perWave = plugin.getConfig().getInt("relic.boss.adds-per-wave", 3);
        int maxAdds = plugin.getConfig().getInt("relic.boss.max-adds", 6);
        long alive = world.getNearbyEntities(center, 32, 16, 32).stream()
                .filter(e -> e.getPersistentDataContainer().has(Keys.GUARDIAN_ADD, PersistentDataType.BYTE))
                .count();
        for (int i = 0; i < perWave && alive + i < maxAdds && addsSpawned < 40; i++) {
            addsSpawned++;
            Location spot = center.clone().add(
                    ThreadLocalRandom.current().nextDouble(-4, 4), 0,
                    ThreadLocalRandom.current().nextDouble(-4, 4));
            // Don't spawn adds inside walls: nudge up to open air if needed.
            for (int lift = 0; lift < 6 && (!spot.getBlock().isPassable()
                    || !spot.clone().add(0, 1, 0).getBlock().isPassable()); lift++) {
                spot.add(0, 1, 0);
            }
            world.spawn(spot, Skeleton.class, add -> {
                add.setCustomName(Text.legacy("<white>Écho Souverain</white>"));
                add.getPersistentDataContainer().set(Keys.GUARDIAN_ADD, PersistentDataType.BYTE, (byte) 1);
                var equipment = add.getEquipment();
                if (equipment != null) {
                    equipment.setItemInMainHand(new ItemStack(Material.BOW));
                    equipment.setItemInMainHandDropChance(0f);
                }
                setAttr(add, Attribute.MAX_HEALTH, 30.0);
                add.setHealth(30.0);
            });
            world.spawnParticle(org.bukkit.Particle.CLOUD, spot.add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private void checkEnrage() {
        if (enraged) {
            return;
        }
        double threshold = plugin.getConfig().getDouble("relic.boss.enrage-threshold", 0.30);
        AttributeInstance maxAttr = guardian.getAttribute(Attribute.MAX_HEALTH);
        double max = maxAttr == null ? 1 : maxAttr.getValue();
        if (guardian.getHealth() / max > threshold) {
            return;
        }
        enraged = true;
        double multiplier = plugin.getConfig().getDouble("relic.boss.enrage-damage-multiplier", 1.5);
        AttributeInstance damage = guardian.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * multiplier);
        }
        AttributeInstance speed = guardian.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.2);
        }
        bossBar.setTitle(Text.legacy("<gold><bold>Gardien Souverain</bold> — ENRAGÉ</gold>"));
        Text.broadcast("<gold>⚠</gold> <white>Le <gold>Gardien Souverain</gold> est blessé... "
                + "et <bold>enragé</bold> !</white>");
        guardian.getWorld().playSound(guardian.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 2f, 0.5f);
        Fx.whiteBurst(guardian.getLocation());
    }

    // -------------------------------------------------------- participation

    /** Called by the listener whenever a player damages the Guardian. */
    public void recordDamage(Player player, double damage) {
        damageDealt.merge(player.getUniqueId(), damage, Double::sum);
        participantNames.put(player.getUniqueId(), player.getName());
    }

    public boolean isGuardian(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(Keys.GUARDIAN, PersistentDataType.BYTE);
    }

    /** True only for the boss entity of the currently running fight. */
    public boolean isCurrentGuardian(Entity entity) {
        return guardian != null && guardian.equals(entity);
    }

    public boolean isGuardianAdd(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(Keys.GUARDIAN_ADD, PersistentDataType.BYTE);
    }

    // --------------------------------------------------------------- death

    /** Called by the listener when the Guardian dies. Drops the relic. */
    public void onGuardianDeath(Location deathLocation) {
        cleanupFight(false);
        // Keep the fight area loaded for the scramble; release after 5 minutes.
        Location ticketCenter = fightCenter;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ticketCenter != null && !isFightActive()) {
                setFightChunkTickets(ticketCenter, false);
            }
        }, 6000L);

        Text.broadcast("<gold>✦</gold> <white>Le <gold>Gardien Souverain</gold> est tombé ! La "
                + "<gold>Hache Fend-Couronne</gold> gît sans maître en <gold>"
                + deathLocation.getBlockX() + ", " + deathLocation.getBlockY() + ", "
                + deathLocation.getBlockZ() + "</gold> — <bold>courez</bold>.</white>");
        Text.broadcastTitle("<white>LE GARDIEN <gold>EST TOMBÉ</gold></white>",
                "<white>La Hache Fend-Couronne attend son propriétaire...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1f, 0.7f);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // The relic drops naturally: no auto-assign, pure scramble.
        World world = deathLocation.getWorld();
        Item drop = world.dropItemNaturally(deathLocation.clone().add(0, 0.5, 0), RelicItems.createRelic());
        drop.setGlowing(true);
        drop.setCustomName(Text.legacy("<gold><bold>✦ Hache Fend-Couronne ✦</bold></gold>"));
        drop.setCustomNameVisible(true);
        setRelicExists(true);
        trackedDrop = drop;
        dropClaimed = false;

        // Watch the drop: a sky-high END_ROD beam for the first ~60s (with the
        // item's age reset so it can't despawn mid-scramble), then a silent
        // watchdog that notices removals no event is fired for.
        final int[] ticks = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            ticks[0] += 10;
            if (dropClaimed || !relicExists || trackedDrop != drop) {
                task.cancel();
                return;
            }
            if (!drop.isValid()) {
                if (drop.getLocation().getChunk().isLoaded()) {
                    // Gone from a loaded chunk with no despawn/damage event:
                    // removed silently (/kill, a mod, ...). The axe is lost.
                    onRelicDestroyed();
                } else {
                    trackedDrop = null; // resting in an unloaded chunk; stop watching
                }
                task.cancel();
                return;
            }
            if (ticks[0] <= 1200) {
                drop.setTicksLived(1);
                Fx.beamTick(drop.getLocation());
            }
        }, 10L, 10L);

        persistParticipants();
    }

    /** Adds/removes plugin chunk tickets covering the fight radius. */
    private void setFightChunkTickets(Location center, boolean add) {
        int chunkRadius = (int) Math.ceil(fightRadius() / 16.0) + 1;
        int centerX = center.getBlockX() >> 4;
        int centerZ = center.getBlockZ() >> 4;
        World world = center.getWorld();
        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
            for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
                if (add) {
                    world.addPluginChunkTicket(x, z, plugin);
                } else {
                    world.removePluginChunkTicket(x, z, plugin);
                }
            }
        }
    }

    private void cleanupFight(boolean releaseArea) {
        if (releaseArea && fightCenter != null) {
            setFightChunkTickets(fightCenter, false);
        }
        if (fightTask != null) {
            fightTask.cancel();
            fightTask = null;
        }
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        guardian = null;
    }

    /** Removes the boss and adds without a drop (plugin disable / abort). */
    private void despawnGuardian(boolean announce) {
        if (guardian != null && guardian.isValid()) {
            Location loc = guardian.getLocation();
            guardian.getWorld().getNearbyEntities(loc, 64, 32, 64).stream()
                    .filter(this::isGuardianAdd)
                    .forEach(Entity::remove);
            guardian.remove();
            if (announce) {
                Text.broadcast("<gray>Le <gold>Gardien Souverain</gold> retourne à la légende...</gray>");
            }
        }
        cleanupFight(true);
    }

    private void persistParticipants() {
        long start = eventStart;
        Map<UUID, Double> damage = new HashMap<>(damageDealt);
        Map<UUID, String> names = new HashMap<>(participantNames);
        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO relic_participants(event_start, uuid, name, damage) VALUES(?, ?, ?, ?)
                    ON CONFLICT(event_start, uuid) DO UPDATE SET damage = excluded.damage""")) {
                for (var entry : damage.entrySet()) {
                    ps.setLong(1, start);
                    ps.setString(2, entry.getKey().toString());
                    ps.setString(3, names.getOrDefault(entry.getKey(), "?"));
                    ps.setDouble(4, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
    }

    /** Fetches the last event's participants for /relic log (async). */
    public void lastParticipants(Consumer<List<Participant>> callback) {
        database.query(conn -> {
            List<Participant> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT name, damage FROM relic_participants
                    WHERE event_start = (SELECT MAX(event_start) FROM relic_participants)
                    ORDER BY damage DESC""");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Participant(rs.getString(1), rs.getDouble(2)));
                }
            }
            return out;
        }, callback);
    }

    /** Status line for admins. */
    public String statusLine() {
        if (isFightActive()) {
            return "Le Gardien Souverain est en plein combat.";
        }
        if (spawnAt != 0) {
            return "Un Gardien arrive dans " + Text.duration((spawnAt - System.currentTimeMillis()) / 1000) + ".";
        }
        if (relicExists) {
            return "La Hache Fend-Couronne existe quelque part ; le Gardien sommeille "
                    + "tant qu'elle n'est pas détruite.";
        }
        if (scheduleEnabled() && nextScheduledSpawn > 0) {
            return "Prochaine apparition programmée dans "
                    + Text.duration((nextScheduledSpawn - System.currentTimeMillis()) / 1000) + ".";
        }
        return "Aucun événement programmé. Utilisez /relic summon pour en déclencher un.";
    }
}
