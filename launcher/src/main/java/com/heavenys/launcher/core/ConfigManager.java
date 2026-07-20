package com.heavenys.launcher.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.heavenys.launcher.model.LauncherConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * JSON-backed configuration store with auto-save, versioned migration, profiles and backups.
 *
 * <p>Layout (default base {@code ~/.heavenys}):
 * <pre>
 *   launcher.json            active config
 *   profiles/&lt;name&gt;.json      named profiles
 *   backups/launcher-*.json  timestamped backups
 * </pre>
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path baseDir;
    private final Path configFile;
    private final Path profilesDir;
    private final Path backupsDir;

    private LauncherConfig config;

    public ConfigManager() {
        this(Path.of(System.getProperty("user.home"), ".heavenys"));
    }

    public ConfigManager(Path baseDir) {
        this.baseDir = baseDir;
        this.configFile = baseDir.resolve("launcher.json");
        this.profilesDir = baseDir.resolve("profiles");
        this.backupsDir = baseDir.resolve("backups");
    }

    public Path getBaseDir() { return baseDir; }

    public LauncherConfig get() {
        if (config == null) {
            config = load();
        }
        return config;
    }

    public LauncherConfig load() {
        try {
            Files.createDirectories(baseDir);
            if (Files.exists(configFile)) {
                LauncherConfig loaded = GSON.fromJson(Files.readString(configFile), LauncherConfig.class);
                config = migrate(loaded != null ? loaded : new LauncherConfig());
            } else {
                config = new LauncherConfig();
                save();
            }
        } catch (IOException | RuntimeException e) {
            config = new LauncherConfig();
        }
        return config;
    }

    /** Migrates an older config forward to {@link LauncherConfig#CURRENT_VERSION}. */
    private LauncherConfig migrate(LauncherConfig c) {
        if (c.configVersion < 1) {
            c.configVersion = 1;
        }
        // Future migrations: if (c.configVersion < 2) { ...; c.configVersion = 2; }
        c.configVersion = LauncherConfig.CURRENT_VERSION;
        return c;
    }

    /** Persists the current config (auto-save entry point). */
    public void save() {
        try {
            Files.createDirectories(baseDir);
            Files.writeString(configFile, GSON.toJson(get()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save launcher config", e);
        }
    }

    // ---- Profiles ----

    public void saveProfile(String name) {
        try {
            Files.createDirectories(profilesDir);
            Files.writeString(profilesDir.resolve(sanitize(name) + ".json"), GSON.toJson(get()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save profile", e);
        }
    }

    public LauncherConfig loadProfile(String name) {
        try {
            Path p = profilesDir.resolve(sanitize(name) + ".json");
            if (Files.exists(p)) {
                config = migrate(GSON.fromJson(Files.readString(p), LauncherConfig.class));
                save();
            }
            return get();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load profile", e);
        }
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(profilesDir)) {
            try (Stream<Path> s = Files.list(profilesDir)) {
                s.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> names.add(p.getFileName().toString().replaceFirst("\\.json$", "")));
            } catch (IOException ignored) {
            }
        }
        return names;
    }

    // ---- Backup / restore ----

    public Path backup() {
        try {
            Files.createDirectories(backupsDir);
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path dest = backupsDir.resolve("launcher-" + ts + ".json");
            Files.writeString(dest, GSON.toJson(get()));
            return dest;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to back up config", e);
        }
    }

    public LauncherConfig restore(Path backupFile) {
        try {
            config = migrate(GSON.fromJson(Files.readString(backupFile), LauncherConfig.class));
            save();
            return get();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to restore config", e);
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_ ]", "_").trim();
    }
}
