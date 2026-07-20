package com.heavenys.launcher.ui;

import javafx.scene.Parent;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the bundled UI fonts and applies the launcher font/scale to the scene graph.
 *
 * <p>Only the <b>launcher</b> font is affected here — the in-game Minecraft font is controlled
 * separately by the client mod and is never touched.
 */
public final class Theme {
    /** Fonts offered in the launcher font selector (some fall back to system if not installed). */
    public static final List<String> FONT_OPTIONS =
            List.of("Poppins", "Inter", "Montserrat", "JetBrains Mono", "Roboto", "SF Pro");

    private static final Map<String, String> LOADED = new LinkedHashMap<>();
    private static boolean initialised;

    private Theme() {
    }

    /** Loads bundled TTFs once. Safe to call multiple times. */
    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        load("Poppins", "/fonts/Poppins-Regular.ttf");
        load("Inter", "/fonts/Inter-Regular.ttf");
        load("Montserrat", "/fonts/Montserrat-Regular.ttf");
        load("JetBrains Mono", "/fonts/JetBrainsMono-Regular.ttf");
    }

    private static void load(String name, String resource) {
        try (InputStream in = Theme.class.getResourceAsStream(resource)) {
            if (in != null) {
                Font f = Font.loadFont(in, 14);
                if (f != null) {
                    LOADED.put(name, f.getFamily());
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** Resolves a display name to a usable family, falling back to a bundled font. */
    public static String resolveFamily(String name) {
        return LOADED.getOrDefault(name, LOADED.getOrDefault("Poppins", "Poppins"));
    }

    /** Applies the chosen font family and UI scale to the root node. */
    public static void applyFontAndScale(Parent root, String fontName, double scale) {
        String family = resolveFamily(fontName);
        double px = Math.max(10, Math.round(13 * scale));
        root.setStyle("-fx-font-family: '" + family + "'; -fx-font-size: " + px + "px;");
    }
}
