package com.heavenys.launcher;

import com.heavenys.launcher.core.LaunchService;
import com.heavenys.launcher.model.Account;
import com.heavenys.launcher.model.LauncherConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LaunchServiceTest {
    private final LaunchService svc = new LaunchService();

    @Test
    void buildsMemoryAndGcArgs() {
        LauncherConfig cfg = new LauncherConfig();
        cfg.minRamMb = 1024;
        cfg.maxRamMb = 3072;
        cfg.gcType = "ZGC";
        cfg.extraJvmArgs = "-XX:+AlwaysPreTouch";
        List<String> args = svc.buildJvmArgs(cfg);
        assertTrue(args.contains("-Xms1024m"));
        assertTrue(args.contains("-Xmx3072m"));
        assertTrue(args.contains("-XX:+UseZGC"));
        assertTrue(args.contains("-XX:+AlwaysPreTouch"));
    }

    @Test
    void validationCatchesBadRamAndMissingAccount() {
        LauncherConfig cfg = new LauncherConfig();
        cfg.minRamMb = 8192;
        cfg.maxRamMb = 2048;
        List<String> problems = svc.validate(cfg, null);
        assertTrue(problems.stream().anyMatch(p -> p.contains("No account")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("exceeds maximum")));
    }

    @Test
    void dryRunReportsJavaVersion() throws Exception {
        LauncherConfig cfg = new LauncherConfig();
        cfg.minRamMb = 512;
        cfg.maxRamMb = 1024;
        String out = svc.runDryRun(cfg);
        assertTrue(out.toLowerCase().contains("version"), "dry-run output should include the java version: " + out);
    }

    @Test
    void fullCommandIncludesAccountAndResolution() {
        LauncherConfig cfg = new LauncherConfig();
        Account acc = Account.offline("Tester");
        List<String> cmd = svc.buildLaunchCommand(cfg, acc);
        assertTrue(cmd.stream().anyMatch(s -> s.contains("heavenys.username=Tester")));
        assertTrue(cmd.stream().anyMatch(s -> s.contains("heavenys.width=" + cfg.resolutionWidth)));
    }
}
