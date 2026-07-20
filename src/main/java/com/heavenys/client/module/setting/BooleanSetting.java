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

/** A simple on/off toggle. */
public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() {
        setValue(!getValue());
    }

    @Override
    public Object write() {
        return getValue();
    }

    @Override
    public void read(Object serialized) {
        if (serialized instanceof Boolean b) {
            setValue(b);
        } else if (serialized instanceof Number n) {
            setValue(n.intValue() != 0);
        }
    }
}
