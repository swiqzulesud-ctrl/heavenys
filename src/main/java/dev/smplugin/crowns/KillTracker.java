package dev.smplugin.crowns;

import dev.smplugin.SMPlugin;
import dev.smplugin.data.Database;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player-vs-player kills for the Crown of Kills.
 *
 * <p>Counts are cached in memory (loaded once on enable) so the crown can be
 * recalculated instantly and synchronously on every PvP death; writes are
 * flushed to SQLite asynchronously.</p>
 */
public final class KillTracker implements Listener {

    /** A leaderboard row. */
    public record Entry(UUID uuid, String name, int kills) {
    }

    private final SMPlugin plugin;
    private final Database database;
    private final CrownManager crowns;
    private final Map<UUID, Entry> counts = new ConcurrentHashMap<>();

    public KillTracker(SMPlugin plugin, Database database, CrownManager crowns) {
        this.plugin = plugin;
        this.database = database;
        this.crowns = crowns;
    }

    /** Loads all kill counts into memory. Called on enable. */
    public void load() {
        database.querySync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, name, kills FROM kills");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    counts.put(uuid, new Entry(uuid, rs.getString("name"), rs.getInt("kills")));
                }
            }
            return null;
        });
    }

    public int kills(UUID uuid) {
        Entry entry = counts.get(uuid);
        return entry == null ? 0 : entry.kills();
    }

    /** Top {@code limit} killers, sorted descending. */
    public List<Entry> top(int limit) {
        List<Entry> sorted = new ArrayList<>(counts.values());
        sorted.sort(Comparator.comparingInt(Entry::kills).reversed());
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("crowns.enabled", true)) {
            return;
        }
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        // getKiller() covers direct kills; indirect causes (TNT, fall after a
        // hit, mob-assist) also resolve to a killer, so when only direct PvP
        // should count we require the final damage to come from an entity hit.
        if (plugin.getConfig().getBoolean("crowns.kills.count-only-direct-pvp", true)) {
            var lastDamage = victim.getLastDamageCause();
            if (lastDamage == null || !(lastDamage instanceof org.bukkit.event.entity.EntityDamageByEntityEvent)) {
                return;
            }
        }

        Entry updated = counts.merge(killer.getUniqueId(),
                new Entry(killer.getUniqueId(), killer.getName(), 1),
                (old, one) -> new Entry(old.uuid(), killer.getName(), old.kills() + 1));

        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO kills(uuid, name, kills) VALUES(?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, kills = excluded.kills""")) {
                ps.setString(1, updated.uuid().toString());
                ps.setString(2, updated.name());
                ps.setInt(3, updated.kills());
                ps.executeUpdate();
            }
        });

        recalculateCrown();
    }

    /** Awards the Crown of Kills to whoever currently has the most kills. */
    public void recalculateCrown() {
        Entry best = null;
        for (Entry entry : counts.values()) {
            if (best == null || entry.kills() > best.kills()) {
                best = entry;
            }
        }
        if (best == null || best.kills() == 0) {
            return;
        }
        CrownManager.Holder current = crowns.holder(CrownType.KILLS);
        // The crown only moves when someone strictly surpasses the holder.
        if (current != null && !current.uuid().equals(best.uuid())
                && kills(current.uuid()) >= best.kills()) {
            return;
        }
        crowns.setHolder(CrownType.KILLS, best.uuid(), best.name(), true);
    }
}
