package dev.smplugin.crowns;

import dev.smplugin.SMPlugin;
import dev.smplugin.data.Database;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.joml.Vector3f;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the three crowns: who holds them, persistence, and the full in-game
 * presentation (broadcast + title + sound, tab prefix via scoreboard teams,
 * white glow, and a golden crown hologram above the holder's head).
 */
public final class CrownManager {

    /** Immutable snapshot of a crown holder. */
    public record Holder(UUID uuid, String name, long since) {
    }

    private final SMPlugin plugin;
    private final Database database;
    private final Map<CrownType, Holder> holders = new EnumMap<>(CrownType.class);
    private final Map<UUID, TextDisplay> holograms = new HashMap<>();

    public CrownManager(SMPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Loads holders from the database and sets up teams. Called on enable. */
    public void load() {
        setupTeams();
        database.querySync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT crown, uuid, name, since FROM crown_holders");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    if (uuid == null) {
                        continue;
                    }
                    CrownType type = CrownType.valueOf(rs.getString("crown"));
                    holders.put(type, new Holder(UUID.fromString(uuid), rs.getString("name"), rs.getLong("since")));
                }
            }
            return null;
        });
        Bukkit.getOnlinePlayers().forEach(this::applyPresentation);
    }

    /** Removes holograms and glow. Called on disable and before reload. */
    public void unload() {
        holograms.values().forEach(TextDisplay::remove);
        holograms.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isHolder(player.getUniqueId())) {
                player.setGlowing(false);
            }
        }
    }

    public Holder holder(CrownType type) {
        return holders.get(type);
    }

    public boolean isHolder(UUID uuid) {
        return holders.values().stream().anyMatch(h -> h.uuid().equals(uuid));
    }

    /**
     * Crowns a new holder (or clears the crown when {@code uuid} is null),
     * persists the change and plays the full ceremony if the holder changed.
     */
    public void setHolder(CrownType type, UUID uuid, String name, boolean announce) {
        Holder previous = holders.get(type);
        if (previous != null && uuid != null && previous.uuid().equals(uuid)) {
            return; // unchanged
        }
        if (previous == null && uuid == null) {
            return;
        }

        Holder next = uuid == null ? null : new Holder(uuid, name, System.currentTimeMillis());
        if (next == null) {
            holders.remove(type);
        } else {
            holders.put(type, next);
        }
        persist(type, next);

        // Strip presentation from the previous holder.
        if (previous != null) {
            Player prevPlayer = Bukkit.getPlayer(previous.uuid());
            if (prevPlayer != null) {
                applyPresentation(prevPlayer);
            } else {
                removeFromTeam(type, previous.name());
            }
        }

        if (next != null) {
            Player nextPlayer = Bukkit.getPlayer(next.uuid());
            if (nextPlayer != null) {
                applyPresentation(nextPlayer);
                Fx.whiteBurst(nextPlayer.getLocation());
            }
            if (announce) {
                Text.broadcast("<gold>👑</gold> <white><bold>" + name + "</bold> is now wearing the <gold>"
                        + type.crownName() + "</gold>!</white>");
                Text.broadcastTitle(
                        "<gold>👑 " + type.crownName() + "</gold>",
                        "<white>" + name + " <gray>claims the throne of <white>" + type.category());
                Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
            }
        } else if (announce) {
            Text.broadcast("<white>The <gold>" + type.crownName()
                    + "</gold> sits unclaimed once more.</white>");
        }
    }

    private void persist(CrownType type, Holder holder) {
        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO crown_holders(crown, uuid, name, since) VALUES(?, ?, ?, ?)
                    ON CONFLICT(crown) DO UPDATE SET uuid = excluded.uuid,
                        name = excluded.name, since = excluded.since""")) {
                ps.setString(1, type.name());
                ps.setString(2, holder == null ? null : holder.uuid().toString());
                ps.setString(3, holder == null ? null : holder.name());
                ps.setLong(4, holder == null ? 0 : holder.since());
                ps.executeUpdate();
            }
        });
    }

    // --------------------------------------------------------- presentation

    /**
     * Syncs a player's crown presentation (team membership, glow, hologram)
     * with the current holder state. Safe to call any time.
     */
    public void applyPresentation(Player player) {
        boolean holdsAny = false;
        for (CrownType type : CrownType.values()) {
            Holder holder = holders.get(type);
            boolean holdsThis = holder != null && holder.uuid().equals(player.getUniqueId());
            Team team = team(type);
            if (holdsThis) {
                // A player can hold several crowns; tab shows the last applied one.
                team.addEntry(player.getName());
                holdsAny = true;
            } else {
                team.removeEntry(player.getName());
            }
        }

        if (plugin.getConfig().getBoolean("crowns.holder-glow", true)) {
            player.setGlowing(holdsAny);
        }

        if (holdsAny && plugin.getConfig().getBoolean("crowns.hologram", true)) {
            spawnHologram(player);
        } else {
            removeHologram(player.getUniqueId());
        }
    }

    /** Spawns (or refreshes) the golden crown hologram riding the player. */
    private void spawnHologram(Player player) {
        removeHologram(player.getUniqueId());
        TextDisplay display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, d -> {
            d.text(Text.mm("<gold><bold>♛</bold></gold>"));
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setDefaultBackground(false);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(false);
            d.setSeeThrough(false);
            var transformation = d.getTransformation();
            transformation.getTranslation().set(new Vector3f(0f, 0.85f, 0f));
            d.setTransformation(transformation);
            d.getPersistentDataContainer().set(Keys.CROWN_HOLO, PersistentDataType.BYTE, (byte) 1);
        });
        player.addPassenger(display);
        holograms.put(player.getUniqueId(), display);
    }

    public void removeHologram(UUID uuid) {
        TextDisplay existing = holograms.remove(uuid);
        if (existing != null) {
            existing.remove();
        }
    }

    // --------------------------------------------------------------- teams

    private void setupTeams() {
        for (CrownType type : CrownType.values()) {
            team(type);
        }
    }

    private Team team(CrownType type) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(type.teamName());
        if (team == null) {
            team = board.registerNewTeam(type.teamName());
        }
        team.prefix(Text.mm("<gold>" + type.tabPrefix() + "</gold> <white>"));
        team.color(NamedTextColor.WHITE); // white name + white glow outline
        return team;
    }

    private void removeFromTeam(CrownType type, String entry) {
        if (entry != null) {
            team(type).removeEntry(entry);
        }
    }

    /** Component describing a crown's holder for GUIs and chat. */
    public Component holderLine(CrownType type) {
        Holder holder = holders.get(type);
        if (holder == null) {
            return Text.mm("<gray>Currently <white>unclaimed</white>.</gray>");
        }
        return Text.mm("<gray>Held by <white><bold>" + holder.name() + "</bold></white> for <white>"
                + Text.duration((System.currentTimeMillis() - holder.since()) / 1000) + "</white>.</gray>");
    }
}
