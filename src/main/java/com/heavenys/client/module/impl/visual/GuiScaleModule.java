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

import com.heavenys.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/**
 * Overrides the vanilla GUI scale option with a custom value while enabled.
 * A value of {@code 0} means "auto". Applied through the vanilla option, so no
 * mixin is required; it takes effect on the next window resize/redraw.
 */
public class GuiScaleModule extends VisualModule {

    private final NumberSetting scale =
            register(new NumberSetting("Scale", "0 = auto, 1-6 = fixed scale.", 3, 0, 6, 1));

    private Integer previousScale;

    public GuiScaleModule() {
        super("GUI Scale", "Sets a custom GUI scale.");
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        previousScale = mc.options.guiScale().get();
        mc.options.guiScale().set(scale.getInt());
    }

    @Override
    protected void onDisable() {
        if (previousScale != null) {
            Minecraft.getInstance().options.guiScale().set(previousScale);
            previousScale = null;
        }
    }
}
