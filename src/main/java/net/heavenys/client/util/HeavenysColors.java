package net.heavenys.client.util;

/**
 * Central color palette for Heavenys Client.
 *
 * Primary aesthetic: clean White (#FFFFFF) paired with Butter Yellow (#F7E78E / #FFFACD).
 * All ARGB values are encoded as 0xAARRGGBB integers for use with DrawContext helpers.
 */
public final class HeavenysColors {

    private HeavenysColors() {}

    // ---- Core Palette -------------------------------------------------------

    /** Pure white — used for primary text and bright accents. */
    public static final int WHITE          = 0xFFFFFFFF;

    /** Butter Yellow — primary brand color. Hex: #F7E78E */
    public static final int BUTTER_YELLOW  = 0xFFF7E78E;

    /** Light Butter — softer variant for backgrounds. Hex: #FFFACD */
    public static final int BUTTER_LIGHT   = 0xFFFFFACD;

    /** Warm off-white used for secondary labels. */
    public static final int OFF_WHITE      = 0xFFEEEEEE;

    // ---- UI Backgrounds -----------------------------------------------------

    /** Dark translucent panel backing — 72 % opacity black. */
    public static final int PANEL_BG       = 0xB8101010;

    /** Slightly lighter row background for alternating lists. */
    public static final int PANEL_BG_ALT   = 0x88181818;

    /** Border/outline color — Butter Yellow at 80 % opacity. */
    public static final int BORDER         = 0xCCF7E78E;

    /** Hover highlight for interactive rows — Yellow at 25 % opacity. */
    public static final int HOVER          = 0x40F7E78E;

    // ---- Functional Tints ---------------------------------------------------

    /** Green tint for "enabled" state indicators. */
    public static final int ENABLED_GREEN  = 0xFF66EE88;

    /** Muted red for "disabled" state indicators. */
    public static final int DISABLED_RED   = 0xFFEE6655;

    /** Warning amber — e.g. low HP / armor durability. */
    public static final int WARN_AMBER     = 0xFFFFAA33;

    // ---- Helpers ------------------------------------------------------------

    /**
     * Returns a copy of {@code argb} with its alpha channel replaced.
     *
     * @param argb  source color
     * @param alpha 0–255
     */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * Linearly interpolates between two ARGB colors.
     *
     * @param a first color
     * @param b second color
     * @param t blend factor, 0.0 = full a, 1.0 = full b
     */
    public static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >> 24) & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int blue = (int) (ab + (bb - ab) * t);
        int alpha = (int) (aa + (ba - aa) * t);
        return (alpha << 24) | (r << 16) | (g << 8) | blue;
    }
}
