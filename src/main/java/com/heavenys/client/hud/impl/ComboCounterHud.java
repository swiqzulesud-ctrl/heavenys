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

import java.util.List;

/** Displays a rolling click-combo counter. */
public class ComboCounterHud extends TextHudModule {

    public ComboCounterHud() {
        super("Combo Counter", "Shows a rolling click combo count.");
        setPosition(4, 132);
    }

    @Override
    protected List<String> getLines() {
        int combo = HeavenysClient.getInstance().getInputTracker().getCombo();
        return List.of("Combo: " + combo);
    }
}
