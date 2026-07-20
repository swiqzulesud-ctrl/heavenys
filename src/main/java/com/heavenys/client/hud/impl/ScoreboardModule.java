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

/** Toggles the vanilla scoreboard sidebar. */
public class ScoreboardModule extends VanillaToggleModule {

    public ScoreboardModule() {
        super("Scoreboard", "Show or hide the scoreboard sidebar.", VanillaHudElements.SCOREBOARD, true);
    }
}
