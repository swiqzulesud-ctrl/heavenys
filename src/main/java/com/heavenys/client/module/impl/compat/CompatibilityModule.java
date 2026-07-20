/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.impl.compat;

import com.heavenys.client.compat.ModCompat;
import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.setting.BooleanSetting;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Surfaces the detected third-party integrations as read-only-ish toggles so the
 * player can see what Heavenys is co-operating with. Each toggle defaults to the
 * mod's detected presence and is only meaningful when that mod is installed.
 */
public class CompatibilityModule extends Module {

    private final Map<ModCompat.Integration, BooleanSetting> toggles = new LinkedHashMap<>();

    public CompatibilityModule() {
        super("Integrations", "Detected performance and visual mod integrations.", Category.COMPATIBILITY);
        setEnabled(true);
        for (ModCompat.Integration integration : ModCompat.Integration.values()) {
            if (integration == ModCompat.Integration.SIMPLE_VOICE_CHAT) {
                continue; // Voice chat has its own dedicated module.
            }
            BooleanSetting toggle = register(new BooleanSetting(
                    integration.getDisplayName(),
                    "Cooperate with " + integration.getDisplayName() + " when present.",
                    integration.isPresent()));
            toggles.put(integration, toggle);
        }
    }

    public boolean isCooperating(ModCompat.Integration integration) {
        BooleanSetting toggle = toggles.get(integration);
        return integration.isPresent() && toggle != null && toggle.getValue();
    }
}
