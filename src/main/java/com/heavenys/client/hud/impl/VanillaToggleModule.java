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

import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/**
 * A HUD module that controls the visibility of an existing vanilla HUD element
 * (such as the scoreboard or boss bar) instead of drawing its own. This reuses
 * the exact vanilla rendering and needs no mixins: the vanilla element is
 * wrapped once via {@link HudElementRegistry#replaceElement} and only forwards
 * to the original when this module is enabled.
 */
public abstract class VanillaToggleModule extends Module {

    private final Identifier vanillaElement;

    protected VanillaToggleModule(String name, String description, Identifier vanillaElement, boolean enabledByDefault) {
        super(name, description, Category.HUD);
        this.vanillaElement = vanillaElement;
        setEnabled(enabledByDefault);
    }

    /** Installs the visibility wrapper around the target vanilla element. */
    public void install() {
        HudElementRegistry.replaceElement(vanillaElement, original ->
                (graphics, deltaTracker) -> {
                    if (isEnabled()) {
                        original.extractRenderState(graphics, deltaTracker);
                    }
                });
    }
}
