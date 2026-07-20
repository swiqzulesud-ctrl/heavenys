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
 * A keyboard binding stored as a GLFW key code. A value of {@code -1} means
 * "unbound". This is kept independent of Minecraft's KeyMapping so it can live
 * in the shared settings/config layer.
 */
public class KeybindSetting extends Setting<Integer> {

    public static final int UNBOUND = -1;

    public KeybindSetting(String name, String description, int glfwKey) {
        super(name, description, glfwKey);
    }

    public boolean isBound() {
        return getValue() != UNBOUND;
    }

    public boolean matches(int keyCode) {
        return isBound() && getValue() == keyCode;
    }

    @Override
    public Object write() {
        return getValue();
    }

    @Override
    public void read(Object serialized) {
        if (serialized instanceof Number n) {
            setValue(n.intValue());
        }
    }
}
