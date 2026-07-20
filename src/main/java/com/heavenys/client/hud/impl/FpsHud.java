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

import com.heavenys.client.hud.TextHudModule;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Displays the current client frame rate. */
public class FpsHud extends TextHudModule {

    public FpsHud() {
        super("FPS", "Shows the current frames per second.");
        setPosition(4, 4);
        setEnabled(true);
    }

    @Override
    protected List<String> getLines() {
        return List.of(Minecraft.getInstance().getFps() + " FPS");
    }
}
