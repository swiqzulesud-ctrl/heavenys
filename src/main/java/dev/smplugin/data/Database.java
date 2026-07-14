package dev.smplugin.data;

import dev.smplugin.SMPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Async-safe SQLite access layer.
 *
 * <p>All statements run on a single background thread (SQLite performs best
 * with one writer), so the main server thread is never blocked. Results are
 * delivered back on the main thread via {@link #supplyAsync}'s callback
 * variant or as {@link CompletableFuture}s.</p>
 */
public final class Database {

    private final SMPlugin plugin;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "SMPlugin-Database"));
    private Connection connection;

    public Database(SMPlugin plugin) {
        this.plugin = plugin;
    }

    /** Opens the database and creates the schema. Called once, on enable (sync). */
    public void init(String fileName) throws SQLException {
        File file = new File(plugin.getDataFolder(), fileName);
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS kills (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        kills INTEGER NOT NULL DEFAULT 0
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS crown_holders (
                        crown TEXT PRIMARY KEY,
                        uuid TEXT,
                        name TEXT,
                        since INTEGER
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS bonus_hearts (
                        uuid TEXT PRIMARY KEY,
                        hearts INTEGER NOT NULL DEFAULT 0
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS build_cycles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        started INTEGER NOT NULL,
                        winner_uuid TEXT,
                        winner_name TEXT,
                        winner_build TEXT,
                        winner_votes INTEGER,
                        finished INTEGER
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS build_submissions (
                        cycle INTEGER NOT NULL,
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        build_name TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                        PRIMARY KEY (cycle, uuid)
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS build_votes (
                        cycle INTEGER NOT NULL,
                        voter_uuid TEXT NOT NULL,
                        target_uuid TEXT NOT NULL,
                        PRIMARY KEY (cycle, voter_uuid)
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        key TEXT PRIMARY KEY,
                        value TEXT
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS relic_participants (
                        event_start INTEGER NOT NULL,
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        damage REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY (event_start, uuid)
                    )""");
        }
    }

    /** Runs a write/read on the database thread; ignores the result. */
    public void runAsync(SqlConsumer task) {
        executor.execute(() -> {
            try {
                task.accept(connection);
            } catch (SQLException e) {
                plugin.getLogger().severe("Database task failed: " + e.getMessage());
            }
        });
    }

    /** Runs a query on the database thread and returns the result as a future. */
    public <T> CompletableFuture<T> supplyAsync(SqlFunction<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(task.apply(connection));
            } catch (SQLException e) {
                plugin.getLogger().severe("Database query failed: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /** Runs a query async, then hands the result to {@code callback} on the main thread. */
    public <T> void query(SqlFunction<T> task, Consumer<T> callback) {
        supplyAsync(task).thenAccept(result -> {
            if (plugin.isEnabled()) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
            }
        });
    }

    /** Blocking variant used only during onEnable/onDisable. */
    public <T> T querySync(SqlFunction<T> task) {
        try {
            return task.apply(connection);
        } catch (SQLException e) {
            plugin.getLogger().severe("Database query failed: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------- meta helpers

    /** Reads a value from the meta key/value table (database thread). */
    public static String getMeta(Connection conn, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT value FROM meta WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Writes a value to the meta key/value table (database thread). */
    public static void setMeta(Connection conn, String key, String value) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO meta(key, value) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    /** Flushes pending tasks and closes the connection. Called on disable. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Database executor did not flush within 10s.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database: " + e.getMessage());
        }
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
