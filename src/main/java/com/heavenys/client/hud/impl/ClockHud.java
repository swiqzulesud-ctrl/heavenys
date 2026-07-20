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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Displays the real-world local time. */
public class ClockHud extends TextHudModule {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClockHud() {
        super("Clock", "Shows the real-world local time.");
        setPosition(4, 46);
    }

    @Override
    protected List<String> getLines() {
        return List.of(LocalTime.now().format(FORMAT));
    }
}
