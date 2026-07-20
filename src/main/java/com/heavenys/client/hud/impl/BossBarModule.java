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

import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Toggles the vanilla boss health bar overlay. */
public class BossBarModule extends VanillaToggleModule {

    public BossBarModule() {
        super("Boss Bar", "Show or hide the boss health bar.", VanillaHudElements.BOSS_BAR, true);
    }
}
