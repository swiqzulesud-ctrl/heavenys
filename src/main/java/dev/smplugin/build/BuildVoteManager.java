package dev.smplugin.build;

import dev.smplugin.SMPlugin;
import dev.smplugin.crowns.CrownType;
import dev.smplugin.data.Database;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs the recurring builder vote that awards the Crown of the Builder.
 *
 * <p>Each cycle lasts {@code builder-vote.interval-hours} and ends with three
 * phases: a quiet waiting period, a nomination window ({@code nomination-hours}
 * long) where players register builds with {@code /build submit}, and a vote
 * window ({@code vote-hours} long, the final stretch of the cycle) where
 * players pick their favourite in a GUI. Results fire automatically.</p>
 */
public final class BuildVoteManager {

    public enum Phase {WAITING, NOMINATION, VOTING}

    /** A build registered for the current cycle. */
    public record Submission(UUID uuid, String playerName, String buildName,
                             String world, double x, double y, double z) {

        public Location location() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z);
        }
    }

    /** Outcome of a finished cycle, for /build results. */
    public record Result(String winnerName, String buildName, int votes, long finished) {
    }

    private final SMPlugin plugin;
    private final Database database;
    private final RewardManager rewards;

    private long cycleId = -1;
    private long cycleStart;
    private Phase phase = Phase.WAITING;
    /** Submissions keyed by builder UUID; insertion order = submission order. */
    private final Map<UUID, Submission> submissions = new LinkedHashMap<>();
    /** voter -> builder they voted for. */
    private final Map<UUID, UUID> votes = new LinkedHashMap<>();

    public BuildVoteManager(SMPlugin plugin, Database database, RewardManager rewards) {
        this.plugin = plugin;
        this.database = database;
        this.rewards = rewards;
    }

    // ------------------------------------------------------------ lifecycle

    /** Restores cycle state from the database, then starts the phase clock. */
    public void load() {
        database.querySync(conn -> {
            String id = Database.getMeta(conn, "build_cycle_id");
            String start = Database.getMeta(conn, "build_cycle_start");
            if (id != null && start != null) {
                cycleId = Long.parseLong(id);
                cycleStart = Long.parseLong(start);
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT uuid, name, build_name, world, x, y, z FROM build_submissions WHERE cycle = ?")) {
                    ps.setLong(1, cycleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            submissions.put(uuid, new Submission(uuid, rs.getString("name"),
                                    rs.getString("build_name"), rs.getString("world"),
                                    rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z")));
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT voter_uuid, target_uuid FROM build_votes WHERE cycle = ?")) {
                    ps.setLong(1, cycleId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            votes.put(UUID.fromString(rs.getString(1)), UUID.fromString(rs.getString(2)));
                        }
                    }
                }
            }
            return null;
        });
        if (cycleId < 0) {
            startNewCycle(System.currentTimeMillis());
        } else {
            phase = phaseAt(System.currentTimeMillis());
        }
        // Phase clock: checks transitions every 10 seconds.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L);
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("builder-vote.enabled", true)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= cycleEnd()) {
            finishCycle();
            return;
        }
        Phase current = phaseAt(now);
        if (current != phase) {
            Phase old = phase;
            phase = current;
            announcePhase(old, current);
        }
    }

    // ------------------------------------------------------------ timing

    private long hoursMs(String path, long def) {
        return Math.max(1, plugin.getConfig().getLong(path, def)) * 3_600_000L;
    }

    public long cycleEnd() {
        return cycleStart + hoursMs("builder-vote.interval-hours", 96);
    }

    public long voteOpensAt() {
        return cycleEnd() - hoursMs("builder-vote.vote-hours", 12);
    }

    public long nominationOpensAt() {
        return voteOpensAt() - hoursMs("builder-vote.nomination-hours", 24);
    }

    private Phase phaseAt(long now) {
        if (now >= voteOpensAt()) {
            return Phase.VOTING;
        }
        if (now >= nominationOpensAt()) {
            return Phase.NOMINATION;
        }
        return Phase.WAITING;
    }

    public Phase phase() {
        return phase;
    }

    /** Seconds until the next phase boundary (for status messages). */
    public long secondsUntilNextPhase() {
        long now = System.currentTimeMillis();
        long next = switch (phase) {
            case WAITING -> nominationOpensAt();
            case NOMINATION -> voteOpensAt();
            case VOTING -> cycleEnd();
        };
        return Math.max(0, (next - now) / 1000);
    }

    private void announcePhase(Phase from, Phase to) {
        if (to == Phase.NOMINATION) {
            Text.broadcast("<white>Les candidatures du <gold>Vote de Construction</gold> sont ouvertes ! "
                    + "Placez-vous devant votre création et utilisez <gold>/build submit <nom></gold> "
                    + "dans les prochaines <white>" + Text.duration(secondsUntilNextPhase()) + "</white>.</white>");
            Text.broadcastTitle("<gold>✦ Vote de Construction ✦</gold>",
                    "<white>Candidatures ouvertes — /build submit");
            Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
        } else if (to == Phase.VOTING) {
            if (submissions.isEmpty()) {
                Text.broadcast("<gray>Aucune construction n'a été proposée ce cycle — le <gold>Vote de "
                        + "Construction</gold> est reporté au prochain cycle.</gray>");
                startNewCycle(System.currentTimeMillis());
                return;
            }
            Text.broadcast("<white>Le <gold>Vote de Construction</gold> est ouvert ! <white>" + submissions.size()
                    + "</white> construction" + (submissions.size() == 1 ? "" : "s") + " se disputent la couronne. "
                    + "Votez avec <gold>/build vote</gold> — clôture dans <white>"
                    + Text.duration(secondsUntilNextPhase()) + "</white>.</white>");
            Text.broadcastTitle("<gold>✦ Vote de Construction ✦</gold>", "<white>Le vote est ouvert — /build vote");
            Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
        }
    }

    // ------------------------------------------------------- admin controls

    private void persistCycleStart() {
        long start = cycleStart;
        database.runAsync(conn -> Database.setMeta(conn, "build_cycle_start", String.valueOf(start)));
    }

    /**
     * Admin: opens the nomination window immediately (the vote window follows
     * after the configured nomination length). Returns an error, or null.
     */
    public String forceNominations() {
        if (!plugin.getConfig().getBoolean("builder-vote.enabled", true)) {
            return "Le vote de construction est désactivé dans la configuration.";
        }
        if (phase == Phase.NOMINATION) {
            return "Les candidatures sont déjà ouvertes.";
        }
        if (phase == Phase.VOTING) {
            return "Le vote est déjà en cours — clôturez-le d'abord avec /build finish.";
        }
        // Shift the cycle clock so the nomination window starts right now.
        cycleStart = System.currentTimeMillis()
                - (hoursMs("builder-vote.interval-hours", 96)
                - hoursMs("builder-vote.nomination-hours", 24)
                - hoursMs("builder-vote.vote-hours", 12));
        persistCycleStart();
        Phase old = phase;
        phase = Phase.NOMINATION;
        announcePhase(old, Phase.NOMINATION);
        return null;
    }

    /** Admin: opens the vote window immediately. Returns an error, or null. */
    public String forceVote() {
        if (!plugin.getConfig().getBoolean("builder-vote.enabled", true)) {
            return "Le vote de construction est désactivé dans la configuration.";
        }
        if (phase == Phase.VOTING) {
            return "Le vote est déjà en cours.";
        }
        if (submissions.isEmpty()) {
            return "Aucune construction n'a été proposée — ouvrez d'abord les candidatures "
                    + "avec /build startnominations.";
        }
        // Shift the cycle clock so the vote window starts right now.
        cycleStart = System.currentTimeMillis()
                - (hoursMs("builder-vote.interval-hours", 96)
                - hoursMs("builder-vote.vote-hours", 12));
        persistCycleStart();
        Phase old = phase;
        phase = Phase.VOTING;
        announcePhase(old, Phase.VOTING);
        return null;
    }

    /** Admin: tallies the votes and ends the cycle immediately. Returns an error, or null. */
    public String forceFinish() {
        if (phase != Phase.VOTING) {
            return "Aucun vote en cours à clôturer — ouvrez-en un avec /build startvote.";
        }
        finishCycle();
        return null;
    }

    /** Admin: throws the current cycle away and starts a fresh one. */
    public void cancelCycle() {
        Text.broadcast("<gray>Le cycle actuel du <gold>Vote de Construction</gold> a été annulé "
                + "par un administrateur. Un nouveau cycle démarre.</gray>");
        startNewCycle(System.currentTimeMillis());
    }

    // ------------------------------------------------------------ actions

    /** Handles /build submit. */
    public void submit(Player player, String buildName) {
        if (!plugin.getConfig().getBoolean("builder-vote.enabled", true)) {
            Text.msg(player, "<gray>Le vote de construction est désactivé pour le moment.</gray>");
            Fx.deny(player);
            return;
        }
        if (phase != Phase.NOMINATION) {
            if (phase == Phase.VOTING) {
                Text.msg(player, "<gray>Les candidatures sont closes — le vote a déjà commencé ! "
                        + "Utilisez <gold>/build vote</gold>.</gray>");
            } else {
                Text.msg(player, "<gray>Les candidatures ne sont pas encore ouvertes. Elles ouvrent dans <white>"
                        + Text.duration((nominationOpensAt() - System.currentTimeMillis()) / 1000) + "</white>.</gray>");
            }
            Fx.deny(player);
            return;
        }
        if (buildName.length() > 32) {
            Text.msg(player, "<gray>Le nom d'une construction est limité à <white>32</white> caractères.</gray>");
            Fx.deny(player);
            return;
        }
        boolean replacing = submissions.containsKey(player.getUniqueId());
        Location loc = player.getLocation();
        Submission submission = new Submission(player.getUniqueId(), player.getName(), buildName,
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        submissions.put(player.getUniqueId(), submission);

        long cycle = cycleId;
        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO build_submissions(cycle, uuid, name, build_name, world, x, y, z)
                    VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(cycle, uuid) DO UPDATE SET name = excluded.name,
                        build_name = excluded.build_name, world = excluded.world,
                        x = excluded.x, y = excluded.y, z = excluded.z""")) {
                ps.setLong(1, cycle);
                ps.setString(2, submission.uuid().toString());
                ps.setString(3, submission.playerName());
                ps.setString(4, submission.buildName());
                ps.setString(5, submission.world());
                ps.setDouble(6, submission.x());
                ps.setDouble(7, submission.y());
                ps.setDouble(8, submission.z());
                ps.executeUpdate();
            }
        });

        Text.msg(player, replacing
                ? "<white>Votre candidature a été mise à jour : <gold>" + buildName
                        + "</gold>, à votre position actuelle.</white>"
                : "<white>Votre construction <gold>" + buildName + "</gold> est inscrite au vote ! "
                        + "Cet endroit devient son point de présentation.</white>");
        Fx.success(player);
        Fx.whiteBurst(player.getLocation());
    }

    /** Handles a vote cast from the GUI. Returns true when the vote counted. */
    public boolean vote(Player voter, UUID target) {
        if (phase != Phase.VOTING) {
            Text.msg(voter, "<gray>La fenêtre de vote n'est pas ouverte actuellement.</gray>");
            Fx.deny(voter);
            return false;
        }
        Submission submission = submissions.get(target);
        if (submission == null) {
            Text.msg(voter, "<gray>Cette construction n'est plus en lice.</gray>");
            Fx.deny(voter);
            return false;
        }
        if (voter.getUniqueId().equals(target)) {
            Text.msg(voter, "<gray>Impossible de voter pour votre propre construction — laissez "
                    + "votre œuvre parler d'elle-même !</gray>");
            Fx.deny(voter);
            return false;
        }
        UUID previous = votes.get(voter.getUniqueId());
        if (previous != null) {
            if (previous.equals(target)) {
                Text.msg(voter, "<gray>Vous avez déjà voté pour cette construction.</gray>");
                Fx.deny(voter);
                return false;
            }
            if (!plugin.getConfig().getBoolean("builder-vote.allow-revote", true)) {
                Text.msg(voter, "<gray>Vous avez déjà voté ce cycle — votre choix est définitif.</gray>");
                Fx.deny(voter);
                return false;
            }
        }

        votes.put(voter.getUniqueId(), target);
        long cycle = cycleId;
        UUID voterId = voter.getUniqueId();
        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO build_votes(cycle, voter_uuid, target_uuid) VALUES(?, ?, ?)
                    ON CONFLICT(cycle, voter_uuid) DO UPDATE SET target_uuid = excluded.target_uuid""")) {
                ps.setLong(1, cycle);
                ps.setString(2, voterId.toString());
                ps.setString(3, target.toString());
                ps.executeUpdate();
            }
        });

        Text.msg(voter, "<white>Vote " + (previous != null ? "modifié pour" : "enregistré pour") + " <gold>"
                + submission.buildName() + "</gold> de <white>" + submission.playerName() + "</white>.</white>");
        Fx.success(voter);
        return true;
    }

    public List<Submission> submissions() {
        return new ArrayList<>(submissions.values());
    }

    public UUID votedFor(UUID voter) {
        return votes.get(voter);
    }

    public int voteCount(UUID builder) {
        return (int) votes.values().stream().filter(builder::equals).count();
    }

    // ------------------------------------------------------------ results

    /** Tallies votes, crowns the winner and rolls into the next cycle. */
    private void finishCycle() {
        if (submissions.isEmpty()) {
            Text.broadcast("<gray>Aucune construction n'a été proposée ce cycle — le <gold>Vote de "
                    + "Construction</gold> est reporté au prochain cycle.</gray>");
            startNewCycle(System.currentTimeMillis());
            return;
        }

        // Highest votes wins; ties break by earliest submission (map order).
        Submission winner = submissions.values().stream()
                .max(Comparator.comparingInt(s -> voteCount(s.uuid())))
                .orElseThrow();
        int winnerVotes = voteCount(winner.uuid());

        Text.broadcast("<gold>✦</gold> <white>Le <gold>Vote de Construction</gold> est terminé ! <gold>"
                + winner.buildName() + "</gold> de <bold>" + winner.playerName() + "</bold> l'emporte avec <white>"
                + winnerVotes + "</white> voix !</white>");
        Text.broadcastTitle("<gold>✦ " + winner.playerName() + " ✦</gold>",
                "<white>remporte le Vote de Construction avec <gold>" + winner.buildName());
        Bukkit.getOnlinePlayers().forEach(Fx::fanfare);

        // White fireworks over the winning build's showcase point.
        Location showcase = winner.location();
        if (showcase != null && showcase.getWorld()
                .isChunkLoaded(showcase.getBlockX() >> 4, showcase.getBlockZ() >> 4)) {
            launchCelebration(showcase);
        }

        plugin.crowns().setHolder(CrownType.BUILDER, winner.uuid(), winner.playerName(), true);
        rewards.grantPendingReward(winner.uuid(), winner.playerName());

        long finished = System.currentTimeMillis();
        long cycle = cycleId;
        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE build_cycles SET winner_uuid = ?, winner_name = ?, winner_build = ?,
                        winner_votes = ?, finished = ? WHERE id = ?""")) {
                ps.setString(1, winner.uuid().toString());
                ps.setString(2, winner.playerName());
                ps.setString(3, winner.buildName());
                ps.setInt(4, winnerVotes);
                ps.setLong(5, finished);
                ps.setLong(6, cycle);
                ps.executeUpdate();
            }
        });

        startNewCycle(finished);
    }

    private void launchCelebration(Location showcase) {
        for (int i = 0; i < 6; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location spread = showcase.clone().add(
                        ThreadLocalRandom.current().nextDouble(-4, 4), 1,
                        ThreadLocalRandom.current().nextDouble(-4, 4));
                Fx.whiteFirework(spread);
            }, i * 12L);
        }
    }

    private void startNewCycle(long start) {
        submissions.clear();
        votes.clear();
        cycleStart = start;
        phase = phaseAt(start);
        Long newId = database.querySync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO build_cycles(started) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, start);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    return rs.next() ? rs.getLong(1) : -1L;
                }
            }
        });
        cycleId = newId == null ? -1 : newId;
        long id = cycleId;
        database.runAsync(conn -> {
            Database.setMeta(conn, "build_cycle_id", String.valueOf(id));
            Database.setMeta(conn, "build_cycle_start", String.valueOf(start));
        });
    }

    /** Fetches the last finished cycle's result for /build results (async). */
    public void lastResult(java.util.function.Consumer<Result> callback) {
        database.query(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT winner_name, winner_build, winner_votes, finished FROM build_cycles
                    WHERE finished IS NOT NULL AND winner_name IS NOT NULL
                    ORDER BY finished DESC LIMIT 1""");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Result(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getLong(4));
                }
                return null;
            }
        }, callback);
    }
}
