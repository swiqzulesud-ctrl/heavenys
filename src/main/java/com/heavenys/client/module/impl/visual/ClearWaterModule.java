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

import com.heavenys.client.module.setting.NumberSetting;

/** Increases underwater visibility by reducing water fog density. */
public class ClearWaterModule extends VisualModule {

    private final NumberSetting clarity =
            register(new NumberSetting("Clarity", "How clear the water becomes.", 0.8, 0.0, 1.0, 0.05));

    public ClearWaterModule() {
        super("Clear Water", "Improves underwater visibility.");
    }

    public double getClarity() {
        return clarity.getValue();
    }
}
