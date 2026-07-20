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
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;

/** Displays the player's network latency to the current server. */
public class PingHud extends TextHudModule {

    public PingHud() {
        super("Ping", "Shows your latency to the server.");
        setPosition(4, 74);
    }

    @Override
    protected List<String> getLines() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.player == null) {
            return List.of("Ping: -");
        }
        PlayerInfo info = connection.getPlayerInfo(mc.player.getUUID());
        int ping = info != null ? info.getLatency() : 0;
        return List.of("Ping: " + ping + "ms");
    }
}
