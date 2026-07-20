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

import com.heavenys.client.HeavenysClient;
import com.heavenys.client.hud.TextHudModule;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Displays an estimate of the server tick rate. */
public class TpsHud extends TextHudModule {

    public TpsHud() {
        super("TPS", "Shows an estimate of the server tick rate.");
        setPosition(4, 88);
    }

    @Override
    protected List<String> getLines() {
        if (Minecraft.getInstance().level == null) {
            return List.of("TPS: -");
        }
        double tps = HeavenysClient.getInstance().getTpsTracker().getTps();
        return List.of(String.format("TPS: %.1f", tps));
    }
}
