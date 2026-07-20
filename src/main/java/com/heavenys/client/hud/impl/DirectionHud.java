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

/** Displays the cardinal direction the player is facing. */
public class DirectionHud extends TextHudModule {

    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public DirectionHud() {
        super("Direction", "Shows the cardinal direction you are facing.");
        setPosition(4, 32);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of("Facing: -");
        }
        float yaw = player.getYRot() % 360.0f;
        if (yaw < 0) {
            yaw += 360.0f;
        }
        int index = Math.round(yaw / 45.0f) % 8;
        return List.of("Facing: " + DIRECTIONS[index]);
    }
}
