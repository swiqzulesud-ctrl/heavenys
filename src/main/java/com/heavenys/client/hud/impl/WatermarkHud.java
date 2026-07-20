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
import com.heavenys.client.module.setting.StringSetting;
import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Branded watermark. Renders an accent-colored "K" mark followed by the client
 * name, matching the minimal premium Know Mods theme.
 */
public class WatermarkHud extends HudModule {

    private final StringSetting text =
            register(new StringSetting("Text", "Watermark label.", "Know Mods"));

    public WatermarkHud() {
        super("Watermark", "Branded client watermark.");
        setPosition(4, 104);
        setEnabled(true);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, Font font, int contentX, int contentY) {
        String mark = "K";
        String label = " " + text.getValue();
        int accent = HeavenysColors.withAlpha(theme().accent(), 255);
        int white = HeavenysColors.withAlpha(theme().outline(), 255);
        UIRenderer.text(g, font, mark, contentX, contentY, accent, true);
        UIRenderer.text(g, font, label, contentX + font.width(mark), contentY, white, true);
    }

    @Override
    protected int contentWidth(Font font) {
        return font.width("K " + text.getValue());
    }

    @Override
    protected int contentHeight(Font font) {
        return font.lineHeight;
    }
}
