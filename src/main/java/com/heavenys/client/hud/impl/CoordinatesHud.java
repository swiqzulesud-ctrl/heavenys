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
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/** Displays the player's block coordinates. */
public class CoordinatesHud extends TextHudModule {

    public CoordinatesHud() {
        super("Coordinates", "Shows your current XYZ position.");
        setPosition(4, 18);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of("XYZ: -");
        }
        return List.of(String.format("XYZ: %.1f, %.1f, %.1f",
                player.getX(), player.getY(), player.getZ()));
    }
}
