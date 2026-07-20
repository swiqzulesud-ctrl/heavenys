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
import com.heavenys.client.module.setting.NumberSetting;
import com.heavenys.client.util.InventoryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

/** Live count of End Crystals carried, highlighting when stock runs low. */
public class EndCrystalCounterHud extends TextHudModule {

    private final NumberSetting lowStock =
            register(new NumberSetting("Low Stock", "Warn (via a *) at or below this count.", 8, 0, 64, 1));

    public EndCrystalCounterHud() {
        super("End Crystal Counter", "Counts End Crystals in your inventory.");
        setPosition(4, 74);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        int total = InventoryUtil.count(player, Items.END_CRYSTAL);
        String warn = total <= lowStock.getInt() ? " *" : "";
        return List.of("Crystals: " + total + warn);
    }
}
