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

import java.util.List;

/** Displays simple per-session statistics such as elapsed play time. */
public class SessionStatsHud extends TextHudModule {

    private final long sessionStart = System.currentTimeMillis();

    public SessionStatsHud() {
        super("Session Stats", "Shows stats for the current play session.");
        setPosition(4, 118);
    }

    @Override
    protected List<String> getLines() {
        long seconds = (System.currentTimeMillis() - sessionStart) / 1000L;
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return List.of(String.format("Session: %02d:%02d:%02d", h, m, s));
    }
}
