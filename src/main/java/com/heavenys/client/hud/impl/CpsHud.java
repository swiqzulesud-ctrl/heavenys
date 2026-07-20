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
import com.heavenys.client.input.InputTracker;

import java.util.List;

/** Displays left/right mouse clicks-per-second. */
public class CpsHud extends TextHudModule {

    public CpsHud() {
        super("CPS", "Shows your clicks per second.");
        setPosition(4, 60);
    }

    @Override
    protected List<String> getLines() {
        InputTracker tracker = HeavenysClient.getInstance().getInputTracker();
        return List.of("CPS: " + tracker.getLeftCps() + " | " + tracker.getRightCps());
    }
}
