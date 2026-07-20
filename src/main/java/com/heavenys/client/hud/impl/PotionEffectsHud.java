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
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/** Lists the player's active potion effects with remaining duration. */
public class PotionEffectsHud extends TextHudModule {

    public PotionEffectsHud() {
        super("Potion Effects", "Lists your active potion effects.");
        setPosition(4, 150);
    }

    @Override
    protected List<String> getLines() {
        LocalPlayer player = Minecraft.getInstance().player;
        List<String> lines = new ArrayList<>();
        if (player == null || player.getActiveEffects().isEmpty()) {
            lines.add("No active effects");
            return lines;
        }
        for (MobEffectInstance effect : player.getActiveEffects()) {
            String name = effect.getEffect().value().getDisplayName().getString();
            int level = effect.getAmplifier() + 1;
            String duration = effect.isInfiniteDuration()
                    ? "\u221E"
                    : StringUtil.formatTickDuration(effect.getDuration(), 20.0f);
            lines.add(name + " " + level + " (" + duration + ")");
        }
        return lines;
    }
}
