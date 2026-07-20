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
import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Renders the 3x9 main inventory grid (excluding the hotbar) as a HUD panel. */
public class InventoryHud extends HudModule {

    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final int CELL = 18;
    private static final int FIRST_MAIN_SLOT = 9; // 0-8 are the hotbar.

    public InventoryHud() {
        super("Inventory HUD", "Shows your main inventory on screen.");
        setPosition(4, 200);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, Font font, int x, int y) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slotX = x + col * CELL;
                int slotY = y + row * CELL;
                UIRenderer.rect(g, slotX, slotY, CELL - 1, CELL - 1,
                        HeavenysColors.withAlpha(theme().outline(), 20));
                int index = FIRST_MAIN_SLOT + row * COLS + col;
                ItemStack stack = inventory.getItem(index);
                if (!stack.isEmpty()) {
                    g.item(stack, slotX + 1, slotY + 1);
                    g.itemDecorations(font, stack, slotX + 1, slotY + 1);
                }
            }
        }
    }

    @Override
    protected int contentWidth(Font font) {
        return COLS * CELL;
    }

    @Override
    protected int contentHeight(Font font) {
        return ROWS * CELL;
    }
}
