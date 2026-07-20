/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.setting;

/**
 * An ARGB color setting. The value is stored as a packed 32-bit integer
 * (0xAARRGGBB) and serialized as a hex string for a human-readable config.
 */
public class ColorSetting extends Setting<Integer> {

    public ColorSetting(String name, String description, int argb) {
        super(name, description, argb);
    }

    public int getAlpha() {
        return (getValue() >> 24) & 0xFF;
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public void setAlpha(int alpha) {
        setValue((getValue() & 0x00FFFFFF) | ((alpha & 0xFF) << 24));
    }

    @Override
    public Object write() {
        return String.format("#%08X", getValue());
    }

    @Override
    public void read(Object serialized) {
        if (serialized instanceof String s) {
            String hex = s.startsWith("#") ? s.substring(1) : s;
            try {
                setValue((int) Long.parseLong(hex, 16));
            } catch (NumberFormatException ignored) {
                // Malformed color - keep the default.
            }
        } else if (serialized instanceof Number n) {
            setValue(n.intValue());
        }
    }
}
