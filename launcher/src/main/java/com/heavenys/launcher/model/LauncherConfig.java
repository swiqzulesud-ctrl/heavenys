package com.heavenys.launcher.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The complete, JSON-serialisable launcher configuration.
 *
 * <p>{@link #configVersion} drives forward-migration in {@code ConfigManager}. All defaults are
 * chosen to be sensible for a competitive PvP setup out of the box.
 */
public class LauncherConfig {
    /** Bump when the schema changes; ConfigManager migrates older files up to this. */
    public static final int CURRENT_VERSION = 1;

    public int configVersion = CURRENT_VERSION;

    // Accounts
    public List<Account> accounts = new ArrayList<>();
    public String activeAccountUuid;

    // Java / memory
    public String javaPath = "";           // empty => auto-detect
    public int minRamMb = 2048;
    public int maxRamMb = 4096;
    public String gcType = "G1GC";          // G1GC | ZGC | ShenandoahGC
    public String extraJvmArgs = "";

    // Window / video
    public int resolutionWidth = 1280;
    public int resolutionHeight = 720;
    public boolean fullscreen = false;

    // Directories
    public String gameDirectory = "";       // empty => default .heavenys/instance
    public String selectedVersion = "1.21.11";

    // Launcher UI preferences
    public int launcherOpacity = 100;       // 0-100
    public String uiFont = "Poppins";
    public double uiScale = 1.0;            // 0.8 - 1.4
    public boolean darkMode = true;
    public boolean discordRichPresence = true;
    public boolean rememberAccounts = true;

    // Optimization toggles (mod id -> enabled)
    public Map<String, Boolean> optimizationMods = new LinkedHashMap<>();
    public String performancePreset = "Balanced"; // Quality | Balanced | Competitive | Ultra FPS
}
