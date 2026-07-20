/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.hud.impl;

import com.heavenys.client.hud.HudModule;
import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Renders WASD movement keys plus the two mouse buttons as themed rounded keys
 * that light up with the accent color while held. Reads the live keybind state
 * so it respects the player's rebindings.
 */
public class KeystrokesHud extends HudModule {

    private static final int KEY = 16;
    private static final int GAP = 2;

    public KeystrokesHud() {
        super("Keystrokes", "Shows WASD and mouse buttons as they are pressed.");
        setPosition(4, 172);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, Font font, int x, int y) {
        Options options = Minecraft.getInstance().options;
        // Row 1: W centered.
        drawKey(g, font, x + KEY + GAP, y, "W", options.keyUp.isDown());
        // Row 2: A S D.
        int row2 = y + KEY + GAP;
        drawKey(g, font, x, row2, "A", options.keyLeft.isDown());
        drawKey(g, font, x + KEY + GAP, row2, "S", options.keyDown.isDown());
        drawKey(g, font, x + (KEY + GAP) * 2, row2, "D", options.keyRight.isDown());
        // Row 3: LMB and RMB as wide keys.
        int row3 = y + (KEY + GAP) * 2;
        int wide = KEY + (KEY + GAP) / 2 - GAP;
        drawWide(g, font, x, row3, wide, "L", isDown(options.keyAttack));
        drawWide(g, font, x + wide + GAP, row3, wide, "R", isDown(options.keyUse));
    }

    private void drawKey(GuiGraphicsExtractor g, Font font, int x, int y, String label, boolean pressed) {
        int radius = Math.min(4, theme().cornerRadius());
        int fill = pressed
                ? HeavenysColors.withAlpha(theme().accent(), 220)
                : HeavenysColors.scaleAlpha(theme().background(), theme().backgroundOpacity());
        int textColor = pressed ? 0xFF111111 : HeavenysColors.withAlpha(theme().outline(), 255);
        UIRenderer.roundedRect(g, x, y, KEY, KEY, radius, fill);
        UIRenderer.roundedOutline(g, x, y, KEY, KEY, radius, HeavenysColors.withAlpha(theme().outline(), 40));
        UIRenderer.centeredText(g, font, label, x + KEY / 2, y + (KEY - font.lineHeight) / 2 + 1, textColor);
    }

    private void drawWide(GuiGraphicsExtractor g, Font font, int x, int y, int width, String label, boolean pressed) {
        int radius = Math.min(4, theme().cornerRadius());
        int fill = pressed
                ? HeavenysColors.withAlpha(theme().accent(), 220)
                : HeavenysColors.scaleAlpha(theme().background(), theme().backgroundOpacity());
        int textColor = pressed ? 0xFF111111 : HeavenysColors.withAlpha(theme().outline(), 255);
        UIRenderer.roundedRect(g, x, y, width, KEY, radius, fill);
        UIRenderer.roundedOutline(g, x, y, width, KEY, radius, HeavenysColors.withAlpha(theme().outline(), 40));
        UIRenderer.centeredText(g, font, label, x + width / 2, y + (KEY - font.lineHeight) / 2 + 1, textColor);
    }

    private static boolean isDown(KeyMapping mapping) {
        return mapping.isDown();
    }

    @Override
    protected int contentWidth(Font font) {
        return (KEY + GAP) * 3 - GAP;
    }

    @Override
    protected int contentHeight(Font font) {
        return (KEY + GAP) * 3 - GAP;
    }
}
