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

import com.heavenys.client.module.setting.BooleanSetting;
import com.heavenys.client.module.setting.NumberSetting;

/** Chat appearance tweaks: opacity, scale, width and infinite-height toggle. */
public class ChatCustomizerModule extends VisualModule {

    private final NumberSetting opacity =
            register(new NumberSetting("Opacity", "Chat background opacity.", 60, 0, 100, 1));
    private final NumberSetting scale =
            register(new NumberSetting("Scale", "Chat text scale.", 1.0, 0.5, 2.0, 0.1));
    private final NumberSetting width =
            register(new NumberSetting("Width", "Chat box width.", 320, 100, 640, 10));
    private final BooleanSetting compact =
            register(new BooleanSetting("Compact", "Reduce line spacing.", false));

    public ChatCustomizerModule() {
        super("Chat Customizer", "Customise chat opacity, scale and width.");
    }

    public double opacity() {
        return opacity.getValue() / 100.0;
    }

    public double scale() {
        return scale.getValue();
    }

    public int width() {
        return width.getInt();
    }

    public boolean compact() {
        return compact.getValue();
    }
}
