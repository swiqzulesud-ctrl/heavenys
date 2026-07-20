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

import com.heavenys.client.HeavenysClient;
import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.impl.client.InterfaceModule;
import com.heavenys.client.module.setting.BooleanSetting;
import com.heavenys.client.module.setting.NumberSetting;
import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Base class for every on-screen HUD element. Each HUD module owns its screen
 * position and scale (persisted as normal settings) plus a toggle for drawing a
 * themed background chip. Subclasses implement {@link #renderContent} and report
 * their {@link #contentWidth}/{@link #contentHeight} so the HUD editor can draw
 * accurate drag boxes.
 */
public abstract class HudModule extends Module {

    protected static final int PADDING = 3;

    private final NumberSetting x = register(new NumberSetting("X", "Horizontal position.", 4, 0, 8192, 1));
    private final NumberSetting y = register(new NumberSetting("Y", "Vertical position.", 4, 0, 8192, 1));
    private final NumberSetting scale = register(new NumberSetting("Scale", "Element scale.", 1.0, 0.5, 3.0, 0.1));
    private final BooleanSetting background =
            register(new BooleanSetting("Background", "Draw a themed background chip.", true));

    protected HudModule(String name, String description) {
        super(name, description, Category.HUD);
    }

    public int getX() {
        return x.getInt();
    }

    public int getY() {
        return y.getInt();
    }

    public void setPosition(int newX, int newY) {
        x.setValue((double) newX);
        y.setValue((double) newY);
    }

    public float getScale() {
        return scale.getFloat();
    }

    protected boolean drawBackground() {
        return background.getValue();
    }

    protected InterfaceModule theme() {
        return HeavenysClient.getInstance().getInterface();
    }

    /** Total rendered width in pixels, including padding and scale. */
    public int getWidth(Font font) {
        return Math.round((contentWidth(font) + PADDING * 2) * getScale());
    }

    /** Total rendered height in pixels, including padding and scale. */
    public int getHeight(Font font) {
        return Math.round((contentHeight(font) + PADDING * 2) * getScale());
    }

    /**
     * Draws the module. The themed background chip and glow are handled here; the
     * subclass only draws its content via {@link #renderContent}.
     */
    public void render(GuiGraphicsExtractor g, Font font) {
        InterfaceModule theme = theme();
        int px = getX();
        int py = getY();
        int w = getWidth(font);
        int h = getHeight(font);

        if (drawBackground()) {
            int bg = HeavenysColors.scaleAlpha(theme.background(), theme.backgroundOpacity());
            if (theme.glow()) {
                UIRenderer.glow(g, px, py, w, h, theme.cornerRadius(),
                        HeavenysColors.withAlpha(theme.accent(), 60), 3);
            }
            UIRenderer.roundedRect(g, px, py, w, h, theme.cornerRadius(), bg);
            UIRenderer.roundedOutline(g, px, py, w, h, theme.cornerRadius(),
                    HeavenysColors.withAlpha(theme.outline(), 40));
        }
        renderContent(g, font, px + PADDING, py + PADDING);
    }

    /** Draws the element content starting at the given top-left content origin. */
    protected abstract void renderContent(GuiGraphicsExtractor g, Font font, int contentX, int contentY);

    protected abstract int contentWidth(Font font);

    protected abstract int contentHeight(Font font);
}
