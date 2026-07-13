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
 * The Sovereign's Relic event: schedules and runs the Sovereign Guardian boss
 * fight, tracks participation, and drops the unique Crown-Splitter Axe.
 *
 * <p>The relic is strictly one-of-a-kind: a flag in the database records
 * whether it currently exists in the world. While it exists, the Guardian
 * cannot be summoned; when the axe is destroyed (despawn, lava, explosion)
 * the flag clears and the event becomes available again.</p>
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
    private long eventStart;
    private boolean enraged;
    private int addsSpawned;
    private final Map<UUID, Double> damageDealt = new HashMap<>();
    private final Map<UUID, String> participantNames = new HashMap<>();
    private final Set<UUID> gearWarned = new HashSet<>();

    private boolean relicExists;

    // The dropped axe currently lying in the arena (null once picked up or
    // destroyed). Tracked so silent removals (/kill, the void, mod quirks)
    // are still detected on the 1.20.1 API, which has no EntityRemoveEvent.
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
        Text.broadcast("<gold>⚠</gold> <white>A <gold>Sovereign Guardian</gold> stirs... "
                + "prepare yourselves. <gray>(arrives in <white>" + when + "</white>)</gray></white>");
        Text.broadcastTitle("<white>⚠ <gold>A Sovereign Guardian stirs...</gold> ⚠</white>",
                "<white>Prepare yourselves — <gold>" + when + "</gold> remain" + (secondsLeft == 1 ? "s" : ""));
        Bukkit.getOnlinePlayers().forEach(Fx::ominous);
    }

    /**
     * Admin trigger. {@code immediate} skips the 10-minute build-up.
     * Returns an error string, or null on success.
     */
    public String summon(boolean immediate) {
        if (!plugin.getConfig().getBoolean("relic.enabled", true)) {
            return "The Sovereign's Relic event is disabled in the config.";
        }
        if (relicExists) {
            return "The Crown-Splitter Axe already exists in the world — only one may exist. "
                    + "The Guardian cannot rise until it is destroyed.";
        }
        if (isFightActive()) {
            return "The Sovereign Guardian is already walking the arena.";
        }
        if (spawnAt != 0) {
            return "A Guardian is already on its way — arrival in "
                    + Text.duration((spawnAt - System.currentTimeMillis()) / 1000) + ".";
        }
        if (arenaLocation() == null) {
            return "The arena world in config.yml (relic.arena.world) is not loaded.";
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
        Text.broadcast("<white>The <gold>Crown-Splitter Axe</gold> has been lost to the world... "
                + "the <gold>Sovereign Guardian</gold> may rise again.</white>");
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
            return "The Crown-Splitter Axe is not marked as existing — nothing to reset.";
        }
        trackedDrop = null;
        setRelicExists(false);
        Text.broadcast("<white>The record of the <gold>Crown-Splitter Axe</gold> has been struck "
                + "from the annals... the <gold>Sovereign Guardian</gold> may rise again.</white>");
        return null;
    }

    public Location arenaLocation() {
        var cfg = plugin.getConfig();
        World world = Bukkit.getWorld(cfg.getString("relic.arena.world", "world"));
        if (world == null) {
            return null;
        }
        return new Location(world,
                cfg.getDouble("relic.arena.x", 0.5),
                cfg.getDouble("relic.arena.y", 80),
                cfg.getDouble("relic.arena.z", 0.5));
    }

    // ------------------------------------------------------------ the boss

    private void spawnGuardian() {
        Location arena = arenaLocation();
        if (arena == null) {
            plugin.getLogger().severe("Relic event aborted: arena world not loaded.");
            return;
        }
        // Keep the arena loaded and ticking for the whole fight, even with
        // nobody nearby — otherwise the boss unloads mid-event.
        setArenaChunkTickets(arena, true);

        var cfg = plugin.getConfig();
        double health = cfg.getDouble("relic.boss.health", 420.0);

        guardian = arena.getWorld().spawn(arena, WitherSkeleton.class, boss -> {
            boss.setCustomName(Text.legacy("<white>Sovereign <gold>Guardian</gold></white>"));
            boss.setCustomNameVisible(false);
            boss.setPersistent(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCanPickupItems(false);
            boss.getPersistentDataContainer().set(Keys.GUARDIAN, PersistentDataType.BYTE, (byte) 1);

            setAttr(boss, Attribute.GENERIC_MAX_HEALTH, health);
            boss.setHealth(health);
            setAttr(boss, Attribute.GENERIC_ATTACK_DAMAGE, cfg.getDouble("relic.boss.attack-damage", 18.0));
            setAttr(boss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, cfg.getDouble("relic.boss.knockback-resistance", 1.0));
            setAttr(boss, Attribute.GENERIC_MOVEMENT_SPEED, cfg.getDouble("relic.boss.movement-speed", 0.33));
            setAttr(boss, Attribute.GENERIC_ARMOR, cfg.getDouble("relic.boss.armor", 14.0));
            setAttr(boss, Attribute.GENERIC_FOLLOW_RANGE, 64.0);

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

        bossBar = Bukkit.createBossBar(Text.legacy("<white><bold>Sovereign Guardian</bold></white>"),
                BarColor.WHITE, BarStyle.SEGMENTED_10);

        // Arrival fanfare.
        Text.broadcast("<gold>⚠</gold> <white>The <gold>Sovereign Guardian</gold> has risen at "
                + "<white>" + arena.getBlockX() + ", " + arena.getBlockY() + ", " + arena.getBlockZ()
                + "</white>! Full Netherite is strongly advised.</white>");
        Text.broadcastTitle("<gold><bold>THE SOVEREIGN GUARDIAN</bold></gold>", "<white>has risen. Claim the relic — if you dare.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1f, 0.6f);
        }
        arena.getWorld().strikeLightningEffect(arena);
        Fx.whiteBurst(arena);

        startFightTasks();
    }

    private void setAttr(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private void startFightTasks() {
        int aoeInterval = Math.max(4, plugin.getConfig().getInt("relic.boss.aoe-interval-seconds", 18));
        double radius = plugin.getConfig().getDouble("relic.arena.radius", 48);

        // Main fight loop (1s): boss bar, viewers, gear warnings, AOE timer, enrage.
        final int[] seconds = {0};
        fightTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (guardian == null || !guardian.isValid()) {
                return; // death handler / failsafe tears everything down
            }
            seconds[0]++;
            updateBossBar(radius);
            warnUnderGeared(radius);
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
        AttributeInstance max = guardian.getAttribute(Attribute.GENERIC_MAX_HEALTH);
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

    /** Soft warning (never a block) for players entering without full Netherite. */
    private void warnUnderGeared(double radius) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(guardian.getWorld())
                    || player.getLocation().distanceSquared(guardian.getLocation()) > radius * radius
                    || gearWarned.contains(player.getUniqueId())
                    || hasFullNetherite(player)) {
                continue;
            }
            gearWarned.add(player.getUniqueId());
            Text.msg(player, "<gold>⚠</gold> <white>You are entering the Guardian's arena without full "
                    + "<gold>Netherite</gold>. It will not show mercy — proceed at your own risk.</white>");
            Text.title(player, "<gold>⚠</gold>", "<white>You are not geared for this fight.");
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
        world.spawnParticle(org.bukkit.Particle.FLASH, center.clone().add(0, 1, 0), 2);
        Fx.whiteBurst(center);
        for (Entity nearby : world.getNearbyEntities(center, 6, 6, 6)) {
            if (!(nearby instanceof Player player)) {
                continue;
            }
            player.damage(aoeDamage, guardian);
            player.setVelocity(player.getLocation().toVector().subtract(center.toVector())
                    .normalize().multiply(0.8).setY(0.4));
            Text.actionBar(player, "<gold>⚠ The Guardian's wrath washes over you!</gold>");
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
                add.setCustomName(Text.legacy("<white>Sovereign Echo</white>"));
                add.getPersistentDataContainer().set(Keys.GUARDIAN_ADD, PersistentDataType.BYTE, (byte) 1);
                var equipment = add.getEquipment();
                if (equipment != null) {
                    equipment.setItemInMainHand(new ItemStack(Material.BOW));
                    equipment.setItemInMainHandDropChance(0f);
                }
                setAttr(add, Attribute.GENERIC_MAX_HEALTH, 30.0);
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
        AttributeInstance maxAttr = guardian.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxAttr == null ? 1 : maxAttr.getValue();
        if (guardian.getHealth() / max > threshold) {
            return;
        }
        enraged = true;
        double multiplier = plugin.getConfig().getDouble("relic.boss.enrage-damage-multiplier", 1.5);
        AttributeInstance damage = guardian.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(damage.getBaseValue() * multiplier);
        }
        AttributeInstance speed = guardian.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * 1.2);
        }
        bossBar.setTitle(Text.legacy("<gold><bold>Sovereign Guardian</bold> — ENRAGED</gold>"));
        Text.broadcast("<gold>⚠</gold> <white>The <gold>Sovereign Guardian</gold> is wounded... and <bold>enraged</bold>!</white>");
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
        // Keep the arena loaded for the scramble; release after 5 minutes.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location arena = arenaLocation();
            if (arena != null && !isFightActive()) {
                setArenaChunkTickets(arena, false);
            }
        }, 6000L);

        Text.broadcast("<gold>✦</gold> <white>The <gold>Sovereign Guardian</gold> has fallen! The "
                + "<gold>Crown-Splitter Axe</gold> lies unclaimed in the arena — <bold>run</bold>.</white>");
        Text.broadcastTitle("<white>THE GUARDIAN <gold>HAS FALLEN</gold></white>",
                "<white>The Crown-Splitter Axe awaits its owner...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1f, 0.7f);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // The relic drops naturally: no auto-assign, pure scramble.
        World world = deathLocation.getWorld();
        Item drop = world.dropItemNaturally(deathLocation.clone().add(0, 0.5, 0), RelicItems.createRelic());
        drop.setGlowing(true);
        drop.setCustomName(Text.legacy("<gold><bold>✦ Crown-Splitter Axe ✦</bold></gold>"));
        drop.setCustomNameVisible(true);
        setRelicExists(true);
        trackedDrop = drop;
        dropClaimed = false;

        // Watch the drop: a sky-high END_ROD beam for the first ~60s (with the
        // item's age reset so it can't despawn mid-scramble), then a silent
        // watchdog that notices removals the 1.20.1 API fires no event for.
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

    /** Adds/removes plugin chunk tickets covering the arena radius. */
    private void setArenaChunkTickets(Location arena, boolean add) {
        int chunkRadius = (int) Math.ceil(plugin.getConfig().getDouble("relic.arena.radius", 48) / 16.0) + 1;
        int centerX = arena.getBlockX() >> 4;
        int centerZ = arena.getBlockZ() >> 4;
        World world = arena.getWorld();
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

    private void cleanupFight(boolean releaseArena) {
        Location arena = arenaLocation();
        if (releaseArena && arena != null) {
            setArenaChunkTickets(arena, false);
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
                Text.broadcast("<gray>The <gold>Sovereign Guardian</gold> fades back into legend...</gray>");
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
            return "The Sovereign Guardian is fighting right now.";
        }
        if (spawnAt != 0) {
            return "A Guardian arrives in " + Text.duration((spawnAt - System.currentTimeMillis()) / 1000) + ".";
        }
        if (relicExists) {
            return "The Crown-Splitter Axe exists in the world; the Guardian sleeps until it is destroyed.";
        }
        if (scheduleEnabled() && nextScheduledSpawn > 0) {
            return "Next scheduled appearance in "
                    + Text.duration((nextScheduledSpawn - System.currentTimeMillis()) / 1000) + ".";
        }
        return "No event scheduled. Use /relic summon to trigger one.";
    }
}
