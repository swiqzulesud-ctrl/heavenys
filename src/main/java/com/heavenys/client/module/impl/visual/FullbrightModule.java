/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.impl.visual;

import net.minecraft.client.Minecraft;

/**
 * Maximises the brightness (gamma) option while enabled and restores the
 * previous value when disabled. Uses the vanilla brightness option so it needs
 * no rendering mixins.
 */
public class FullbrightModule extends VisualModule {

    private Double previousGamma;

    public FullbrightModule() {
        super("Fullbright", "Boosts brightness to the maximum while enabled.");
    }

    @Override
    public void onClientTick(Minecraft mc) {
        if (isEnabled()) {
            if (previousGamma == null) {
                previousGamma = mc.options.gamma().get();
            }
            mc.options.gamma().set(1.0);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (previousGamma != null) {
            mc.options.gamma().set(previousGamma);
            previousGamma = null;
        }
    }
}
