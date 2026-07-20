/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.hud;

import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Convenience base for HUD elements that render one or more lines of text.
 * Subclasses only need to supply {@link #getLines()}; layout, sizing and theme
 * colors are handled here.
 */
public abstract class TextHudModule extends HudModule {

    protected static final int LINE_SPACING = 1;

    protected TextHudModule(String name, String description) {
        super(name, description);
    }

    /** @return the lines of text to draw, top to bottom. Never {@code null}. */
    protected abstract List<String> getLines();

    @Override
    protected void renderContent(GuiGraphicsExtractor g, Font font, int contentX, int contentY) {
        List<String> lines = getLines();
        int textColor = HeavenysColors.withAlpha(theme().outline(), 255);
        int y = contentY;
        for (String line : lines) {
            UIRenderer.text(g, font, line, contentX, y, textColor, true);
            y += font.lineHeight + LINE_SPACING;
        }
    }

    @Override
    protected int contentWidth(Font font) {
        int max = 0;
        for (String line : getLines()) {
            max = Math.max(max, font.width(line));
        }
        return max;
    }

    @Override
    protected int contentHeight(Font font) {
        int count = Math.max(1, getLines().size());
        return count * font.lineHeight + (count - 1) * LINE_SPACING;
    }
}
