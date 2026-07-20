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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shows durability of the held item as remaining points and a percentage. */
public class DurabilityHud extends TextHudModule {

    public DurabilityHud() {
        super("Durability", "Shows the held item's remaining durability.");
        setPosition(4, 116);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of("Durability: -");
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return List.of("Durability: -");
        }
        int max = stack.getMaxDamage();
        int remaining = max - stack.getDamageValue();
        int percent = max > 0 ? Math.round(remaining * 100f / max) : 0;
        return List.of("Durability: " + remaining + "/" + max + " (" + percent + "%)");
    }
}
