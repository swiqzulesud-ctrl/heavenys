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
            Text.broadcast("<white>The <gold>Builder Vote</gold> nominations are open! Stand by your build and use "
                    + "<gold>/build submit <name></gold> within the next <white>"
                    + Text.duration(secondsUntilNextPhase()) + "</white>.</white>");
            Text.broadcastTitle("<gold>✦ Builder Vote ✦</gold>", "<white>Nominations are open — /build submit");
            Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
        } else if (to == Phase.VOTING) {
            if (submissions.isEmpty()) {
                Text.broadcast("<gray>No builds were submitted this cycle — the <gold>Builder Vote</gold> "
                        + "is skipped and rolls over to the next cycle.</gray>");
                startNewCycle(System.currentTimeMillis());
                return;
            }
            Text.broadcast("<white>The <gold>Builder Vote</gold> is open! <white>" + submissions.size()
                    + "</white> build" + (submissions.size() == 1 ? "" : "s") + " compete for the crown. Cast your vote with "
                    + "<gold>/build vote</gold> — closes in <white>" + Text.duration(secondsUntilNextPhase()) + "</white>.</white>");
            Text.broadcastTitle("<gold>✦ Builder Vote ✦</gold>", "<white>Voting is open — /build vote");
            Bukkit.getOnlinePlayers().forEach(Fx::fanfare);
        }
    }

    // ------------------------------------------------------------ actions

    /** Handles /build submit. */
    public void submit(Player player, String buildName) {
        if (!plugin.getConfig().getBoolean("builder-vote.enabled", true)) {
            Text.msg(player, "<gray>The builder vote is currently disabled.</gray>");
            Fx.deny(player);
            return;
        }
        if (phase != Phase.NOMINATION) {
            if (phase == Phase.VOTING) {
                Text.msg(player, "<gray>Nominations have closed — voting is already underway! "
                        + "Use <gold>/build vote</gold>.</gray>");
            } else {
                Text.msg(player, "<gray>Nominations aren't open yet. The window opens in <white>"
                        + Text.duration((nominationOpensAt() - System.currentTimeMillis()) / 1000) + "</white>.</gray>");
            }
            Fx.deny(player);
            return;
        }
        if (buildName.length() > 32) {
            Text.msg(player, "<gray>Build names are limited to <white>32</white> characters.</gray>");
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
                ? "<white>Your entry was updated to <gold>" + buildName + "</gold> at your current spot.</white>"
                : "<white>Your build <gold>" + buildName + "</gold> is entered into the vote! "
                        + "This spot is saved as its showcase point.</white>");
        Fx.success(player);
        Fx.whiteBurst(player.getLocation());
    }

    /** Handles a vote cast from the GUI. Returns true when the vote counted. */
    public boolean vote(Player voter, UUID target) {
        if (phase != Phase.VOTING) {
            Text.msg(voter, "<gray>The voting window isn't open right now.</gray>");
            Fx.deny(voter);
            return false;
        }
        Submission submission = submissions.get(target);
        if (submission == null) {
            Text.msg(voter, "<gray>That build is no longer in the running.</gray>");
            Fx.deny(voter);
            return false;
        }
        if (voter.getUniqueId().equals(target)) {
            Text.msg(voter, "<gray>You can't vote for your own build — let your work speak for itself!</gray>");
            Fx.deny(voter);
            return false;
        }
        UUID previous = votes.get(voter.getUniqueId());
        if (previous != null) {
            if (previous.equals(target)) {
                Text.msg(voter, "<gray>You already voted for this build.</gray>");
                Fx.deny(voter);
                return false;
            }
            if (!plugin.getConfig().getBoolean("builder-vote.allow-revote", true)) {
                Text.msg(voter, "<gray>You already cast your vote this cycle — it cannot be changed.</gray>");
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

        Text.msg(voter, "<white>Vote " + (previous != null ? "changed to" : "cast for") + " <gold>"
                + submission.buildName() + "</gold> by <white>" + submission.playerName() + "</white>.</white>");
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
            Text.broadcast("<gray>No builds were submitted this cycle — the <gold>Builder Vote</gold> "
                    + "is skipped and rolls over to the next cycle.</gray>");
            startNewCycle(System.currentTimeMillis());
            return;
        }

        // Highest votes wins; ties break by earliest submission (map order).
        Submission winner = submissions.values().stream()
                .max(Comparator.comparingInt(s -> voteCount(s.uuid())))
                .orElseThrow();
        int winnerVotes = voteCount(winner.uuid());

        Text.broadcast("<gold>✦</gold> <white>The <gold>Builder Vote</gold> has ended! <bold>"
                + winner.playerName() + "</bold>'s <gold>" + winner.buildName() + "</gold> wins with <white>"
                + winnerVotes + "</white> vote" + (winnerVotes == 1 ? "" : "s") + "!</white>");
        Text.broadcastTitle("<gold>✦ " + winner.playerName() + " ✦</gold>",
                "<white>wins the Builder Vote with <gold>" + winner.buildName());
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
