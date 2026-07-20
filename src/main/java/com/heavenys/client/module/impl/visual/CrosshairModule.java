/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.impl.visual;

import com.heavenys.client.module.setting.ColorSetting;
import com.heavenys.client.module.setting.EnumSetting;
import com.heavenys.client.module.setting.NumberSetting;

/** Custom crosshair editor: style, size, thickness, gap and color. */
public class CrosshairModule extends VisualModule {

    public enum Style {
        CROSS, DOT, CIRCLE, T_SHAPE
    }

    private final EnumSetting<Style> style =
            register(new EnumSetting<>("Style", "Crosshair shape.", Style.CROSS));
    private final NumberSetting size =
            register(new NumberSetting("Size", "Crosshair size.", 5, 1, 20, 1));
    private final NumberSetting thickness =
            register(new NumberSetting("Thickness", "Line thickness.", 1, 1, 4, 1));
    private final NumberSetting gap =
            register(new NumberSetting("Gap", "Center gap.", 2, 0, 10, 1));
    private final ColorSetting color =
            register(new ColorSetting("Color", "Crosshair color.", 0xFFFFD54A));

    public CrosshairModule() {
        super("Crosshair", "Customise the crosshair appearance.");
    }

    public Style style() {
        return style.getValue();
    }

    public int size() {
        return size.getInt();
    }

    public int thickness() {
        return thickness.getInt();
    }

    public int gap() {
        return gap.getInt();
    }

    public int color() {
        return color.getValue();
    }
}
