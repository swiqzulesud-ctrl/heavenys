/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package com.heavenys.client.module.setting;

import java.util.function.Supplier;

/**
 * Base class for a single configurable value belonging to a {@link com.heavenys.client.module.Module}.
 *
 * <p>Settings are intentionally UI- and Minecraft-agnostic so that the config
 * system and the ClickGUI can share the exact same objects. Each setting knows
 * how to serialize itself to and from JSON via {@link #write()} / {@link #read(Object)}.</p>
 *
 * @param <T> the type of value stored by this setting
 */
public abstract class Setting<T> {

    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;

    /** Optional predicate controlling whether this setting is visible in the UI. */
    private Supplier<Boolean> visibility = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void reset() {
        this.value = defaultValue;
    }

    public Setting<T> visibleWhen(Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }

    public boolean isVisible() {
        return visibility.get();
    }

    /**
     * @return a JSON-friendly representation (primitive, String or Number) of the current value.
     */
    public abstract Object write();

    /**
     * Restores the value from a previously {@link #write() written} JSON element.
     *
     * @param serialized the raw value produced by Gson while parsing the config
     */
    public abstract void read(Object serialized);
}
