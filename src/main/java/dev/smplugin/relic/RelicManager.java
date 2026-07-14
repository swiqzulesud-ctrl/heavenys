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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * L'événement de la Relique Souveraine : programme et anime les combats de
 * boss, suit la participation et fait tomber les reliques uniques.
 *
 * <p>Plusieurs boss existent ({@link RelicBoss}) ; chaque événement en tire
 * un au hasard parmi ceux dont la relique n'existe pas encore. Le boss
 * apparaît à un endroit aléatoire de la carte (dans un rayon configurable
 * autour du spawn du monde, ~5000 blocs par défaut) et ses coordonnées
 * exactes sont annoncées dans le chat au moment de son arrivée.</p>
 *
 * <p>Chaque relique est strictement unique : un indicateur en base de données
 * enregistre son existence. Tant qu'une relique existe, son boss ne peut pas
 * être invoqué ; quand elle est détruite, l'indicateur s'efface et le boss
 * redevient disponible.</p>
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
    private RelicBoss pendingBoss;     // chosen when the countdown begins
    private final Set<Integer> firedWarnings = new HashSet<>();

    // Active fight state.
    private Mob guardian;
    private RelicBoss activeBoss;
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

    /** Which relics currently exist in the world (one-copy invariant). */
    private final Set<RelicBoss> existingRelics = EnumSet.noneOf(RelicBoss.class);

    /**
     * Relic drops currently lying on the ground, keyed by item entity id.
     * Each drop has its own watchdog so silent removals (/kill on items
     * fires no damage event, mods, ...) are detected for every drop, even
     * when several relics are on the ground at once.
     */
    private final Map<UUID, RelicBoss> activeDrops = new HashMap<>();

    public RelicManager(SMPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    // ------------------------------------------------------------ lifecycle

    /** Restores the relic flags + schedule and starts the clock. */
    public void load() {
        database.querySync(conn -> {
            for (RelicBoss boss : RelicBoss.values()) {
                if ("true".equals(Database.getMeta(conn, boss.existsKey()))) {
                    existingRelics.add(boss);
                }
            }
            // Migration: the pre-multi-boss flag belonged to the Gardien's axe.
            if ("true".equals(Database.getMeta(conn, "relic_exists"))) {
                existingRelics.add(RelicBoss.GARDIEN);
                Database.setMeta(conn, "relic_exists", "false");
                Database.setMeta(conn, RelicBoss.GARDIEN.existsKey(), "true");
            }
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

    /** Bosses whose relic does not currently exist. */
    public List<RelicBoss> availableBosses() {
        List<RelicBoss> out = new ArrayList<>();
        for (RelicBoss boss : RelicBoss.values()) {
            if (!existingRelics.contains(boss)) {
                out.add(boss);
            }
        }
        return out;
    }

    private RelicBoss pickRandomAvailable() {
        List<RelicBoss> available = availableBosses();
        if (available.isEmpty()) {
            return null;
        }
        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
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
            RelicBoss boss = pickRandomAvailable();
            if (boss == null) {
                // Every relic exists: push the schedule one interval out.
                scheduleNext();
            } else {
                pendingBoss = boss;
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
        String who = pendingBoss == null ? "Un <gold>fléau</gold>" : pendingBoss.announceArticle();
        Text.broadcast("<gold>⚠</gold> <white>" + who + " s'éveille... "
                + "préparez-vous. <gray>(arrivée dans <white>" + when + "</white> — position révélée "
                + "à son apparition)</gray></white>");
        Text.broadcastTitle("<white>⚠ " + who + " s'éveille... ⚠</white>",
                "<white>Préparez-vous — il reste <gold>" + when + "</gold>");
        Bukkit.getOnlinePlayers().forEach(Fx::ominous);
    }

    /**
     * Admin trigger. {@code immediate} skips the 10-minute build-up;
     * {@code requested} forces a specific boss (null = random among available).
     * Returns an error string, or null on success.
     */
    public String summon(boolean immediate, RelicBoss requested) {
        if (!plugin.getConfig().getBoolean("relic.enabled", true)) {
            return "L'événement de la Relique Souveraine est désactivé dans la configuration.";
        }
        if (isFightActive()) {
            return "Un boss arpente déjà le monde : " + activeBoss.bossName() + ".";
        }
        if (spawnAt != 0) {
            return "Un boss est déjà en route — arrivée dans "
                    + Text.duration((spawnAt - System.currentTimeMillis()) / 1000) + ".";
        }
        if (requested != null && existingRelics.contains(requested)) {
            return "La relique « " + requested.relicName() + " » existe déjà — il ne peut y en avoir "
                    + "qu'une. Le " + requested.bossName() + " ne peut renaître tant qu'elle n'est pas détruite.";
        }
        RelicBoss boss = requested != null ? requested : pickRandomAvailable();
        if (boss == null) {
            return "Toutes les reliques existent déjà — aucun boss ne peut renaître "
                    + "tant qu'elles ne sont pas détruites (/relic reset pour forcer).";
        }
        if (spawnWorld() == null) {
            return "Le monde configuré (relic.spawn.world) n'est pas chargé.";
        }
        pendingBoss = boss;
        if (immediate) {
            spawnGuardian();
        } else {
            beginCountdown(System.currentTimeMillis() + 600_000L);
            broadcastWarning(600);
        }
        return null;
    }

    public boolean relicExists(RelicBoss boss) {
        return existingRelics.contains(boss);
    }

    public boolean isFightActive() {
        return guardian != null && !guardian.isDead();
    }

    private void setRelicExists(RelicBoss boss, boolean exists) {
        if (exists) {
            existingRelics.add(boss);
        } else {
            existingRelics.remove(boss);
        }
        database.runAsync(conn -> Database.setMeta(conn, boss.existsKey(), String.valueOf(exists)));
    }

    /** Called by the listener when a dropped relic is destroyed or despawns. */
    public void onRelicDestroyed(RelicBoss boss) {
        if (!existingRelics.contains(boss)) {
            return; // already handled (e.g. despawn event + drop tracker)
        }
        activeDrops.values().removeIf(b -> b == boss);
        setRelicExists(boss, false);
        Text.broadcast("<white>La relique <gold>" + boss.relicName() + "</gold> a été perdue à jamais... "
                + "le <gold>" + boss.bossName() + "</gold> peut renaître.</white>");
    }

    /** Called by the listener when any entity or hopper picks a dropped relic up. */
    public void markDropClaimed(Item item) {
        activeDrops.remove(item.getUniqueId());
    }

    /**
     * Admin escape hatch: clears one-copy flags when a relic was lost in a
     * way the plugin cannot observe. {@code boss} null = all relics.
     * Returns an error string, or null on success.
     */
    public String adminResetRelic(RelicBoss boss) {
        if (boss != null) {
            if (!existingRelics.contains(boss)) {
                return "La relique « " + boss.relicName() + " » n'est pas marquée comme existante.";
            }
            activeDrops.values().removeIf(b -> b == boss);
            setRelicExists(boss, false);
            Text.broadcast("<white>La trace de la relique <gold>" + boss.relicName() + "</gold> a été "
                    + "effacée des annales... le <gold>" + boss.bossName() + "</gold> peut renaître.</white>");
            return null;
        }
        if (existingRelics.isEmpty()) {
            return "Aucune relique n'est marquée comme existante — rien à réinitialiser.";
        }
        for (RelicBoss each : RelicBoss.values()) {
            if (existingRelics.contains(each)) {
                setRelicExists(each, false);
            }
        }
        activeDrops.clear();
        Text.broadcast("<white>La trace de toutes les <gold>reliques</gold> a été effacée des annales... "
                + "les boss peuvent renaître.</white>");
        return null;
    }

    // ---------------------------------------------------------- spawn point

    private World spawnWorld() {
        return Bukkit.getWorld(plugin.getConfig().getString("relic.spawn.world", "world"));
    }

    /**
     * Picks a random surface location within {@code relic.spawn.radius}
     * blocks (~5000 by default) of the world spawn.
     */
    private Location pickSpawnLocation() {
        World world = spawnWorld();
        if (world == null) {
            return null;
        }
        double radius = Math.max(16, plugin.getConfig().getDouble("relic.spawn.radius", 5000));
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
        RelicBoss boss = pendingBoss != null ? pendingBoss : pickRandomAvailable();
        pendingBoss = null;
        if (boss == null) {
            plugin.getLogger().warning("Événement de la relique annulé : toutes les reliques existent déjà.");
            return;
        }
        Location spot = pickSpawnLocation();
        if (spot == null) {
            plugin.getLogger().severe("Événement de la relique annulé : le monde d'apparition n'est pas chargé.");
            return;
        }
        activeBoss = boss;
        fightCenter = spot.clone();
        // Keep the fight area loaded and ticking for the whole fight, even
        // with nobody nearby — otherwise the boss unloads mid-event.
        setFightChunkTickets(fightCenter, true);

        var cfg = plugin.getConfig();
        double health = cfg.getDouble("relic.boss.health", 420.0) * boss.healthMultiplier();
        double damage = cfg.getDouble("relic.boss.attack-damage", 18.0) * boss.damageMultiplier();

        guardian = spot.getWorld().spawn(spot, boss.entityClass(), mob -> {
            mob.setCustomName(Text.legacy("<white>" + boss.bossName() + "</white>"));
            mob.setCustomNameVisible(false);
            mob.setPersistent(true);
            mob.setRemoveWhenFarAway(false);
            mob.setCanPickupItems(false);
            mob.getPersistentDataContainer().set(Keys.GUARDIAN, PersistentDataType.BYTE, (byte) 1);

            setAttr(mob, Attribute.MAX_HEALTH, health);
            mob.setHealth(health);
            setAttr(mob, Attribute.ATTACK_DAMAGE, damage);
            setAttr(mob, Attribute.KNOCKBACK_RESISTANCE, cfg.getDouble("relic.boss.knockback-resistance", 1.0));
            setAttr(mob, Attribute.MOVEMENT_SPEED, boss.movementSpeed());
            setAttr(mob, Attribute.ARMOR, cfg.getDouble("relic.boss.armor", 14.0));
            setAttr(mob, Attribute.FOLLOW_RANGE, 64.0);

            if (boss == RelicBoss.GARDIEN) {
                var equipment = mob.getEquipment();
                if (equipment != null) {
                    equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
                    equipment.setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                    equipment.setItemInMainHandDropChance(0f);
                    equipment.setHelmetDropChance(0f);
                }
            }
        });

        eventStart = System.currentTimeMillis();
        enraged = false;
        addsSpawned = 0;
        damageDealt.clear();
        participantNames.clear();
        gearWarned.clear();

        bossBar = Bukkit.createBossBar(Text.legacy("<white><bold>" + boss.bossName() + "</bold></white>"),
                BarColor.WHITE, BarStyle.SEGMENTED_10);

        // Arrival fanfare — the exact coordinates go out in chat.
        Text.broadcast("<gold>⚠</gold> <white>Le <gold>" + boss.bossName() + "</gold> est apparu en "
                + "<gold>" + spot.getBlockX() + ", " + spot.getBlockY() + ", " + spot.getBlockZ()
                + "</gold> <gray>(" + spot.getWorld().getName() + ")</gray> ! "
                + "Le Netherite complet est fortement conseillé.</white>");
        Text.broadcastTitle("<gold><bold>" + boss.bossName().toUpperCase() + "</bold></gold>",
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
            Text.msg(player, "<gold>⚠</gold> <white>Vous approchez du " + activeBoss.bossName()
                    + " sans armure en <gold>Netherite</gold> complète. Il sera sans pitié — "
                    + "à vos risques et périls.</white>");
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

    /** Lightning + summoned adds, the boss's periodic area attack. */
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
            Text.actionBar(player, "<gold>⚠ La fureur du " + activeBoss.bossName() + " s'abat sur vous !</gold>");
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
            world.spawn(spot, activeBoss.addClass(), add -> {
                add.setCustomName(Text.legacy("<white>" + activeBoss.addName() + "</white>"));
                add.getPersistentDataContainer().set(Keys.GUARDIAN_ADD, PersistentDataType.BYTE, (byte) 1);
                var equipment = add.getEquipment();
                if (equipment != null && activeBoss == RelicBoss.GARDIEN) {
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
        bossBar.setTitle(Text.legacy("<gold><bold>" + activeBoss.bossName() + "</bold> — ENRAGÉ</gold>"));
        Text.broadcast("<gold>⚠</gold> <white>Le <gold>" + activeBoss.bossName() + "</gold> est blessé... "
                + "et <bold>enragé</bold> !</white>");
        guardian.getWorld().playSound(guardian.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 2f, 0.5f);
        Fx.whiteBurst(guardian.getLocation());
    }

    // -------------------------------------------------------- participation

    /** Called by the listener whenever a player damages the boss. */
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

    /** Called by the listener when the boss dies. Drops its relic. */
    public void onGuardianDeath(Location deathLocation) {
        RelicBoss boss = activeBoss;
        cleanupFight(false);
        // Keep the fight area loaded for the scramble; release after 5 minutes.
        Location ticketCenter = fightCenter;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (ticketCenter != null && !isFightActive()) {
                setFightChunkTickets(ticketCenter, false);
            }
        }, 6000L);

        Text.broadcast("<gold>✦</gold> <white>Le <gold>" + boss.bossName() + "</gold> est tombé ! La relique "
                + "<gold>" + boss.relicName() + "</gold> gît sans maître en <gold>"
                + deathLocation.getBlockX() + ", " + deathLocation.getBlockY() + ", "
                + deathLocation.getBlockZ() + "</gold> — <bold>courez</bold>.</white>");
        Text.broadcastTitle("<white>LE " + boss.bossName().toUpperCase() + " <gold>EST TOMBÉ</gold></white>",
                "<white>" + boss.relicName() + " attend son propriétaire...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1f, 0.7f);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // The relic drops naturally: no auto-assign, pure scramble.
        World world = deathLocation.getWorld();
        Item drop = world.dropItemNaturally(deathLocation.clone().add(0, 0.5, 0), RelicItems.create(boss));
        drop.setGlowing(true);
        drop.setCustomName(Text.legacy("<gold><bold>✦ " + boss.relicName() + " ✦</bold></gold>"));
        drop.setCustomNameVisible(true);
        setRelicExists(boss, true);
        activeDrops.put(drop.getUniqueId(), boss);

        // Watch the drop: a sky-high END_ROD beam for the first ~60s (with the
        // item's age reset so it can't despawn mid-scramble), then a silent
        // watchdog that notices removals no event is fired for.
        final int[] ticks = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            ticks[0] += 10;
            if (!activeDrops.containsKey(drop.getUniqueId()) || !existingRelics.contains(boss)) {
                task.cancel();
                return;
            }
            if (!drop.isValid()) {
                if (drop.getLocation().getChunk().isLoaded()) {
                    // Gone from a loaded chunk with no despawn/damage event:
                    // removed silently (/kill, a mod, ...). The relic is lost.
                    onRelicDestroyed(boss);
                } else {
                    activeDrops.remove(drop.getUniqueId()); // unloaded chunk; stop watching
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
            if (announce && activeBoss != null) {
                Text.broadcast("<gray>Le <gold>" + activeBoss.bossName() + "</gold> retourne à la légende...</gray>");
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
            return "Le " + activeBoss.bossName() + " est en plein combat.";
        }
        if (spawnAt != 0) {
            return "Un boss arrive dans " + Text.duration((spawnAt - System.currentTimeMillis()) / 1000) + ".";
        }
        StringBuilder sb = new StringBuilder();
        if (!existingRelics.isEmpty()) {
            List<String> names = existingRelics.stream().map(RelicBoss::relicName).toList();
            sb.append("Reliques en circulation : ").append(String.join(", ", names)).append(". ");
        }
        if (availableBosses().isEmpty()) {
            sb.append("Tous les boss sommeillent tant que leurs reliques existent.");
        } else if (scheduleEnabled() && nextScheduledSpawn > 0) {
            sb.append("Prochaine apparition programmée dans ")
                    .append(Text.duration((nextScheduledSpawn - System.currentTimeMillis()) / 1000))
                    .append(".");
        } else {
            sb.append("Aucun événement programmé. Utilisez /relic summon pour en déclencher un.");
        }
        return sb.toString();
    }
}
