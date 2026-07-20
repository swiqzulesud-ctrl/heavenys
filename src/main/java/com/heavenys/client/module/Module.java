/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module;

import com.heavenys.client.module.setting.KeybindSetting;
import com.heavenys.client.module.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every toggleable feature in Heavenys Client.
 *
 * <p>A module owns an ordered list of {@link Setting settings}, a {@link Category}
 * and an enabled state. Subclasses override the lifecycle hooks
 * ({@link #onEnable()}, {@link #onDisable()}) to react to being toggled.</p>
 */
public abstract class Module {

    private final String name;
    private final String description;
    private final Category category;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final KeybindSetting keybind =
            new KeybindSetting("Keybind", "Key that toggles this module.", KeybindSetting.UNBOUND);

    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        // Every module exposes a keybind by default.
        this.settings.add(keybind);
    }

    protected <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public KeybindSetting getKeybind() {
        return keybind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Called once when the module becomes enabled. */
    protected void onEnable() {
    }

    /** Called once when the module becomes disabled. */
    protected void onDisable() {
    }

    /**
     * Called every client tick while the game is running, for every module
     * (enabled or not). Modules that apply live effects should check
     * {@link #isEnabled()} and act accordingly. Default: no-op.
     *
     * @param mc the Minecraft client instance
     */
    public void onClientTick(net.minecraft.client.Minecraft mc) {
    }
}
