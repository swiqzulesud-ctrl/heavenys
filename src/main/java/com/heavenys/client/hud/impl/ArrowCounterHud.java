/*
 * Know Mods - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Know Mods contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.hud.impl;

import com.heavenys.client.hud.TextHudModule;
import com.heavenys.client.util.InventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

/** Live count of arrows (normal, spectral and tipped) available to fire. */
public class ArrowCounterHud extends TextHudModule {

    public ArrowCounterHud() {
        super("Arrow Counter", "Counts arrows in your inventory.");
        setPosition(4, 102);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        int total = InventoryUtil.count(player,
                Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW);
        return List.of("Arrows: " + total);
    }
}
