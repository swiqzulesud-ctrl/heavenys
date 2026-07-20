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
import com.heavenys.client.module.setting.BooleanSetting;
import com.heavenys.client.util.InventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

/** Live count of Totems of Undying carried, with an optional separate off-hand tally. */
public class TotemCounterHud extends TextHudModule {

    private final BooleanSetting showOffhand =
            register(new BooleanSetting("Show Offhand", "Also show the off-hand totem count.", true));

    public TotemCounterHud() {
        super("Totem Counter", "Counts Totems of Undying in your inventory and off-hand.");
        setPosition(4, 60);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        int total = InventoryUtil.count(player, Items.TOTEM_OF_UNDYING);
        if (showOffhand.getValue()) {
            int offhand = InventoryUtil.countOffhand(player, Items.TOTEM_OF_UNDYING);
            return List.of("Totems: " + total + " [" + offhand + "]");
        }
        return List.of("Totems: " + total);
    }
}
