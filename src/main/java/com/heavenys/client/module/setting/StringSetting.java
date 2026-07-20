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

/** A free-form text setting (used for the watermark text, custom labels, etc.). */
public class StringSetting extends Setting<String> {

    public StringSetting(String name, String description, String defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public Object write() {
        return getValue();
    }

    @Override
    public void read(Object serialized) {
        if (serialized != null) {
            setValue(String.valueOf(serialized));
        }
    }
}
