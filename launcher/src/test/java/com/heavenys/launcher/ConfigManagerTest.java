package com.heavenys.launcher;

import com.heavenys.launcher.core.ConfigManager;
import com.heavenys.launcher.model.LauncherConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @Test
    void savesAndReloadsConfig(@TempDir Path dir) {
        ConfigManager cm = new ConfigManager(dir);
        LauncherConfig cfg = cm.load();
        cfg.maxRamMb = 6144;
        cfg.uiFont = "Inter";
        cm.save();

        LauncherConfig reloaded = new ConfigManager(dir).load();
        assertEquals(6144, reloaded.maxRamMb);
        assertEquals("Inter", reloaded.uiFont);
        assertEquals(LauncherConfig.CURRENT_VERSION, reloaded.configVersion);
    }

    @Test
    void profilesAndBackupRoundTrip(@TempDir Path dir) {
        ConfigManager cm = new ConfigManager(dir);
        cm.load().selectedVersion = "1.21.11";
        cm.saveProfile("pvp");
        assertTrue(cm.listProfiles().contains("pvp"));

        Path backup = cm.backup();
        assertTrue(backup.toFile().exists());

        cm.get().selectedVersion = "1.21.10";
        cm.save();
        cm.restore(backup);
        assertEquals("1.21.11", cm.get().selectedVersion);
    }
}
