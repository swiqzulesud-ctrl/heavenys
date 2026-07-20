/*
 * Know Mods (KnowClient) - Windows/desktop installer.
 * Copyright (C) 2026 Know Mods contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.knowmods.installer;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Tiny, dependency-free installer for the Know Mods Fabric client.
 *
 * <p>The Know Mods mod jar is embedded inside this program as the classpath
 * resource {@code /knowmods.jar}. Running the installer copies that jar into the
 * player's Minecraft {@code mods} directory so it is picked up by the Fabric
 * loader on the next launch. It does nothing else: no network access, no
 * registry edits, no elevated permissions.</p>
 *
 * <p>Wrapped into {@code KnowMods-Installer.exe} for Windows via launch4j, but it
 * runs anywhere a JRE is available. On a desktop it shows a small Swing dialog;
 * when headless or invoked with {@code --console} it prints to standard out. Use
 * {@code --dir <path>} to install into a specific mods folder (used by tests).</p>
 */
public final class KnowModsInstaller {

    private static final String EMBEDDED_JAR = "/knowmods.jar";
    private static final String INSTALLED_NAME = "knowmods.jar";
    private static final String TITLE = "Know Mods Installer";

    private KnowModsInstaller() {
    }

    public static void main(String[] args) {
        boolean console = GraphicsEnvironment.isHeadless();
        Path override = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--console".equals(arg)) {
                console = true;
            } else if ("--dir".equals(arg) && i + 1 < args.length) {
                override = Path.of(args[++i]);
            }
        }

        try {
            Path modsDir = override != null ? override : defaultModsDir();
            Path installed = install(modsDir);
            String message = "Know Mods was installed successfully.\n\n"
                    + "Installed to:\n" + installed + "\n\n"
                    + "Make sure the Fabric loader is installed for Minecraft 26.2,\n"
                    + "then launch that profile to use Know Mods.";
            report(console, message, false);
        } catch (Exception e) {
            String message = "Know Mods could not be installed.\n\n" + e.getMessage()
                    + "\n\nYou can install manually by copying knowmods.jar into your\n"
                    + "Minecraft 'mods' folder.";
            report(console, message, true);
            System.exit(1);
        }
    }

    /**
     * Copies the embedded mod jar into {@code modsDir} (creating it if needed) and
     * returns the path of the installed jar.
     */
    public static Path install(Path modsDir) throws IOException {
        Files.createDirectories(modsDir);
        Path target = modsDir.resolve(INSTALLED_NAME);
        try (InputStream in = KnowModsInstaller.class.getResourceAsStream(EMBEDDED_JAR)) {
            if (in == null) {
                throw new IOException("The installer is missing its bundled mod jar (" + EMBEDDED_JAR + ").");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    /** Best-effort default Minecraft {@code mods} directory for the current OS. */
    public static Path defaultModsDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path base = (appData != null && !appData.isBlank()) ? Path.of(appData) : Path.of(home, "AppData", "Roaming");
            return base.resolve(".minecraft").resolve("mods");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return Path.of(home, "Library", "Application Support", "minecraft", "mods");
        }
        return Path.of(home, ".minecraft", "mods");
    }

    private static void report(boolean console, String message, boolean error) {
        if (console) {
            (error ? System.err : System.out).println((error ? "[ERROR] " : "[OK] ") + message);
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel.
        }
        JOptionPane.showMessageDialog(null, message, TITLE,
                error ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }
}
