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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Central registry of every {@link Module}. Modules register themselves here at
 * construction time so the config system, ClickGUI and HUD renderer can all
 * iterate over a single source of truth.
 */
public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

    public <T extends Module> T register(T module) {
        modules.add(module);
        return module;
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public Optional<Module> getByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return Optional.of(module);
            }
        }
        return Optional.empty();
    }

    /** Toggles any module whose keybind matches the given GLFW key code. */
    public void onKeyPress(int keyCode) {
        for (Module module : modules) {
            if (module.getKeybind().matches(keyCode)) {
                module.toggle();
            }
        }
    }
}
