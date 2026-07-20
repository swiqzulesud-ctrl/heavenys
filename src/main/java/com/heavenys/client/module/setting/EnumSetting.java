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
 * A setting backed by a Java {@link Enum}. Serialized by constant name so the
 * config remains stable even if the enum order changes.
 *
 * @param <E> the enum type
 */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final Class<E> type;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.type = defaultValue.getDeclaringClass();
    }

    public E[] getConstants() {
        return type.getEnumConstants();
    }

    /** Advances to the next constant, wrapping around at the end. */
    public void cycle() {
        E[] values = getConstants();
        int next = (getValue().ordinal() + 1) % values.length;
        setValue(values[next]);
    }

    @Override
    public Object write() {
        return getValue().name();
    }

    @Override
    public void read(Object serialized) {
        if (serialized instanceof String s) {
            try {
                setValue(Enum.valueOf(type, s));
            } catch (IllegalArgumentException ignored) {
                // Unknown constant (e.g. removed in a newer version) - keep the default.
            }
        }
    }
}
