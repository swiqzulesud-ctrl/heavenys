/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Thin, theme-aware drawing helpers built on top of Minecraft 26.2's
 * {@link GuiGraphicsExtractor}. Only primitive fills and text are used, so the
 * client never touches raw OpenGL/Vulkan calls (which 26.2 discourages) and no
 * extra mixins are required.
 */
public final class UIRenderer {

    private UIRenderer() {
    }

    /** Filled axis-aligned rectangle. */
    public static void rect(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + height, color);
    }

    /**
     * Filled rounded rectangle. Corners are drawn as true quarter discs; the
     * radius is clamped so it can never exceed half the smaller side.
     */
    public static void roundedRect(GuiGraphicsExtractor g, int x, int y, int width, int height, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            rect(g, x, y, width, height, color);
            return;
        }
        int x2 = x + width;
        int y2 = y + height;

        // Body: a full-height middle band plus two side bands between the corners.
        g.fill(x + r, y, x2 - r, y2, color);
        g.fill(x, y + r, x + r, y2 - r, color);
        g.fill(x2 - r, y + r, x2, y2 - r, color);

        // Four corner quarter-discs.
        fillQuarter(g, x + r, y + r, r, -1, -1, color);
        fillQuarter(g, x2 - r, y + r, r, 1, -1, color);
        fillQuarter(g, x + r, y2 - r, r, -1, 1, color);
        fillQuarter(g, x2 - r, y2 - r, r, 1, 1, color);
    }

    /** Rounded outline drawn as four thin rounded bars (1px thick by default). */
    public static void roundedOutline(GuiGraphicsExtractor g, int x, int y, int width, int height, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int x2 = x + width;
        int y2 = y + height;
        // Top and bottom edges (inset by the corner radius).
        g.fill(x + r, y, x2 - r, y + 1, color);
        g.fill(x + r, y2 - 1, x2 - r, y2, color);
        // Left and right edges.
        g.fill(x, y + r, x + 1, y2 - r, color);
        g.fill(x2 - 1, y + r, x2, y2 - r, color);
    }

    /**
     * Soft glow around a rectangle, drawn as a few progressively larger and more
     * transparent rounded rectangles. Cheap and mixin-free.
     */
    public static void glow(GuiGraphicsExtractor g, int x, int y, int width, int height, int radius, int color, int layers) {
        int baseAlpha = HeavenysColors.alpha(color);
        for (int i = layers; i >= 1; i--) {
            int spread = i * 2;
            int a = Math.max(1, baseAlpha / (i * 3));
            int layerColor = HeavenysColors.withAlpha(color, a);
            roundedRect(g, x - spread, y - spread, width + spread * 2, height + spread * 2,
                    radius + spread, layerColor);
        }
    }

    /** Draws left-aligned text with an optional drop shadow. */
    public static void text(GuiGraphicsExtractor g, Font font, String value, int x, int y, int color, boolean shadow) {
        g.text(font, value, x, y, color, shadow);
    }

    /** Draws horizontally-centered text around {@code centerX}. */
    public static void centeredText(GuiGraphicsExtractor g, Font font, String value, int centerX, int y, int color) {
        g.text(font, value, centerX - font.width(value) / 2, y, color, true);
    }

    private static void fillQuarter(GuiGraphicsExtractor g, int cx, int cy, int r, int signX, int signY, int color) {
        for (int oy = 0; oy < r; oy++) {
            int ox = (int) Math.floor(Math.sqrt(Math.max(0, (double) r * r - (double) oy * oy)));
            int py = cy + signY * oy;
            int rowY = signY < 0 ? py - 1 : py;
            int x0 = signX < 0 ? cx - ox : cx;
            int x1 = signX < 0 ? cx : cx + ox;
            g.fill(x0, rowY, x1, rowY + 1, color);
        }
    }
}
