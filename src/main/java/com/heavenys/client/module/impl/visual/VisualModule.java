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

import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;

/** Convenience base for visual modules (all sit in the {@link Category#VISUAL} tab). */
public abstract class VisualModule extends Module {

    protected VisualModule(String name, String description) {
        super(name, description, Category.VISUAL);
    }
}
