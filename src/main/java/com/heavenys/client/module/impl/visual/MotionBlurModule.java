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

/** Configurable motion-blur strength for the camera. */
public class MotionBlurModule extends VisualModule {

    private final NumberSetting strength =
            register(new NumberSetting("Strength", "Blur amount.", 0.5, 0.0, 1.0, 0.05));

    public MotionBlurModule() {
        super("Motion Blur", "Applies a subtle motion blur to camera movement.");
    }

    public double getStrength() {
        return strength.getValue();
    }
}
