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
import com.heavenys.client.module.setting.NumberSetting;

/**
 * Hold-to-zoom module. The zoom factor and smoothing are configurable here; the
 * factor is exposed for the FOV pipeline to consume (see
 * {@code ZoomModule#getFactor()}), keeping the feature free of render mixins.
 */
public class ZoomModule extends VisualModule {

    private final NumberSetting factor =
            register(new NumberSetting("Factor", "How far to zoom in.", 3.0, 1.5, 8.0, 0.5));
    private final BooleanSetting smooth =
            register(new BooleanSetting("Smooth", "Smoothly interpolate the zoom.", true));

    public ZoomModule() {
        super("Zoom", "Hold to zoom the camera in.");
    }

    public double getFactor() {
        return factor.getValue();
    }

    public boolean isSmooth() {
        return smooth.getValue();
    }
}
