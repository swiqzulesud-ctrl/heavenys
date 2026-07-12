package net.heavenys.client.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Thin wrappers around DrawContext to keep module rendering code clean.
 */
public final class RenderUtil {

    private RenderUtil() {}

    /**
     * Draws a filled, rounded-looking rectangle with a 1-px border.
     * Uses plain fill() calls for compatibility with all Sodium-patched render paths.
     */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        // Background
        ctx.fill(x, y, x + w, y + h, HeavenysColors.PANEL_BG);
        // Top & bottom border
        ctx.fill(x, y,         x + w, y + 1,     HeavenysColors.BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h,     HeavenysColors.BORDER);
        // Left & right border
        ctx.fill(x,         y, x + 1,     y + h, HeavenysColors.BORDER);
        ctx.fill(x + w - 1, y, x + w,     y + h, HeavenysColors.BORDER);
    }

    /**
     * Draws a filled rectangle with no border.
     */
    public static void drawFilledRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    /**
     * Draws a horizontal progress bar (e.g. durability) using a two-tone fill.
     *
     * @param filled fraction [0.0, 1.0]
     */
    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h,
                                       float filled, int filledColor, int emptyColor) {
        int filledPx = Math.round(w * Math.max(0f, Math.min(1f, filled)));
        if (emptyColor != 0) ctx.fill(x, y, x + w, y + h, emptyColor);
        if (filledPx > 0)    ctx.fill(x, y, x + filledPx, y + h, filledColor);
    }
}
