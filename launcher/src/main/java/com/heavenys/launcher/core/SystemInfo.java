package com.heavenys.launcher.core;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small helper for detecting the host Java runtime and available memory. */
public final class SystemInfo {
    private SystemInfo() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Path to the {@code java} executable of the current runtime. */
    public static String detectJavaPath() {
        String home = System.getProperty("java.home");
        Path bin = Path.of(home, "bin", isWindows() ? "java.exe" : "java");
        return bin.toString();
    }

    /** Total physical RAM in MB (best-effort; 8192 fallback). */
    public static long totalRamMb() {
        try {
            java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
                return sun.getTotalMemorySize() / (1024 * 1024);
            }
        } catch (Throwable ignored) {
        }
        return 8192;
    }

    public static boolean isValidJava(String javaPath) {
        if (javaPath == null || javaPath.isBlank()) {
            return false;
        }
        return Files.isRegularFile(Path.of(javaPath));
    }
}
