package com.heavenys.launcher.core;

import com.heavenys.launcher.model.Account;
import com.heavenys.launcher.model.LauncherConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles and validates the JVM command used to launch the game, and can run a real
 * <b>dry-run</b> of the resolved Java runtime to prove the memory/GC arguments are valid.
 *
 * <p>Full vanilla bootstrapping (asset index, libraries, natives, session auth) is handled by
 * the launcher-core roadmap; this service already produces the exact JVM prefix that layer will
 * use, so the two compose cleanly.
 */
public class LaunchService {
    private static String gcFlag(String gcType) {
        return switch (gcType == null ? "G1GC" : gcType) {
            case "ZGC" -> "-XX:+UseZGC";
            case "ShenandoahGC" -> "-XX:+UseShenandoahGC";
            default -> "-XX:+UseG1GC";
        };
    }

    public String resolveJava(LauncherConfig cfg) {
        if (cfg.javaPath != null && !cfg.javaPath.isBlank()) {
            return cfg.javaPath;
        }
        return SystemInfo.detectJavaPath();
    }

    public Path resolveGameDir(LauncherConfig cfg) {
        if (cfg.gameDirectory != null && !cfg.gameDirectory.isBlank()) {
            return Path.of(cfg.gameDirectory);
        }
        return Path.of(System.getProperty("user.home"), ".heavenys", "instance");
    }

    /** JVM memory/GC/window arguments common to a dry-run and the full launch. */
    public List<String> buildJvmArgs(LauncherConfig cfg) {
        List<String> args = new ArrayList<>();
        args.add("-Xms" + cfg.minRamMb + "m");
        args.add("-Xmx" + cfg.maxRamMb + "m");
        args.add(gcFlag(cfg.gcType));
        if (cfg.extraJvmArgs != null && !cfg.extraJvmArgs.isBlank()) {
            for (String a : cfg.extraJvmArgs.trim().split("\\s+")) {
                args.add(a);
            }
        }
        return args;
    }

    /** The full representative launch command (java + JVM args + window/account + game dir). */
    public List<String> buildLaunchCommand(LauncherConfig cfg, Account account) {
        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJava(cfg));
        cmd.addAll(buildJvmArgs(cfg));
        cmd.add("-Dheavenys.version=" + cfg.selectedVersion);
        cmd.add("-Dheavenys.width=" + cfg.resolutionWidth);
        cmd.add("-Dheavenys.height=" + cfg.resolutionHeight);
        if (cfg.fullscreen) {
            cmd.add("-Dheavenys.fullscreen=true");
        }
        cmd.add("-Dminecraft.gameDir=" + resolveGameDir(cfg));
        if (account != null) {
            cmd.add("-Dheavenys.username=" + account.getUsername());
            cmd.add("-Dheavenys.uuid=" + account.getUuid());
        }
        // Placeholder entrypoint; the bootstrap layer replaces this with the real classpath/main.
        cmd.add("net.minecraft.client.main.Main");
        return cmd;
    }

    /** Pre-launch validation: returns human-readable problems (empty list == ready to launch). */
    public List<String> validate(LauncherConfig cfg, Account account) {
        List<String> problems = new ArrayList<>();
        if (account == null) {
            problems.add("No account selected — add or select an account first.");
        }
        String java = resolveJava(cfg);
        if (!SystemInfo.isValidJava(java)) {
            problems.add("Java executable not found: " + java);
        }
        if (cfg.minRamMb <= 0 || cfg.maxRamMb <= 0) {
            problems.add("RAM allocation must be positive.");
        }
        if (cfg.minRamMb > cfg.maxRamMb) {
            problems.add("Minimum RAM (" + cfg.minRamMb + " MB) exceeds maximum (" + cfg.maxRamMb + " MB).");
        }
        try {
            Files.createDirectories(resolveGameDir(cfg));
        } catch (IOException e) {
            problems.add("Cannot create game directory: " + e.getMessage());
        }
        return problems;
    }

    /**
     * Runs {@code <java> <jvmArgs> -version} as a real subprocess — a safe dry-run proving the
     * resolved runtime accepts the configured memory/GC arguments. Returns combined output.
     */
    public String runDryRun(LauncherConfig cfg) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJava(cfg));
        cmd.addAll(buildJvmArgs(cfg));
        cmd.add("-version");
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return output;
    }
}
