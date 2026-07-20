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

import com.heavenys.client.hud.HudModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Displays the player's currently worn armor and held item with durability counts. */
public class ArmorStatusHud extends HudModule {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
    };
    private static final int SLOT = 18;

    public ArmorStatusHud() {
        super("Armor Status", "Shows worn armor and the held item.");
        setPosition(220, 4);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, Font font, int x, int y) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int drawX = x;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                g.item(stack, drawX, y);
                g.itemDecorations(font, stack, drawX, y);
            }
            drawX += SLOT;
        }
    }

    @Override
    protected int contentWidth(Font font) {
        return SLOT * SLOTS.length;
    }

    @Override
    protected int contentHeight(Font font) {
        return SLOT;
    }
}
