/*
 * Know Mods - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Know Mods contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Read-only helpers for counting items the player is carrying (no cheats). */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Counts every matching item the player carries across the whole inventory,
     * including the hotbar, armor slots and off-hand.
     */
    public static int count(LocalPlayer player, Item... items) {
        if (player == null) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack, items)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Counts matching items held in the off-hand slot only. */
    public static int countOffhand(LocalPlayer player, Item... items) {
        if (player == null) {
            return 0;
        }
        ItemStack stack = player.getOffhandItem();
        return matches(stack, items) ? stack.getCount() : 0;
    }

    private static boolean matches(ItemStack stack, Item... items) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (Item item : items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }
}
