/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.theme;

/**
 * Fonts selectable for the client UI. The TrueType files for Inter and Poppins
 * are bundled under {@code assets/heavenys/fonts} (client UI only). Rendering
 * always falls back to the vanilla font if a custom font fails to load, so this
 * enum is safe regardless of resource-pack state.
 */
public enum FontChoice {
    VANILLA("Vanilla"),
    INTER("Inter"),
    POPPINS("Poppins");

    private final String displayName;

    FontChoice(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
