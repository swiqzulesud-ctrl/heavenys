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

import com.heavenys.client.module.setting.BooleanSetting;

/** Renders dropped items lying flat on the ground with light physics. */
public class ItemPhysicsModule extends VisualModule {

    private final BooleanSetting randomRotation =
            register(new BooleanSetting("Random Rotation", "Randomise ground item rotation.", true));

    public ItemPhysicsModule() {
        super("Item Physics", "Dropped items rest naturally on the ground.");
    }

    public boolean randomRotation() {
        return randomRotation.getValue();
    }
}
