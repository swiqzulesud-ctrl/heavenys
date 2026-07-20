/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.ModuleManager;
import com.heavenys.client.module.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON-backed configuration system for Heavenys Client.
 *
 * <p>Each named <em>profile</em> is a standalone JSON document under
 * {@code config/heavenys/profiles/<name>.json}. A tiny {@code state.json} tracks
 * which profile is active. The manager supports auto-saving, importing an
 * external JSON file as a new profile, and exporting a profile to an arbitrary
 * path - covering the requested "JSON / Auto Save / Profiles / Import / Export"
 * feature set.</p>
 */
public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("Heavenys/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DEFAULT_PROFILE = "default";

    private final ModuleManager moduleManager;
    private final Path root;
    private final Path profilesDir;
    private final Path stateFile;

    private String activeProfile = DEFAULT_PROFILE;
    private boolean autoSave = true;

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.root = FabricLoader.getInstance().getConfigDir().resolve("heavenys");
        this.profilesDir = root.resolve("profiles");
        this.stateFile = root.resolve("state.json");
        try {
            Files.createDirectories(profilesDir);
        } catch (IOException e) {
            LOGGER.error("Could not create config directory {}", profilesDir, e);
        }
    }

    // ------------------------------------------------------------------ state

    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public String getActiveProfile() {
        return activeProfile;
    }

    public List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        if (Files.isDirectory(profilesDir)) {
            try {
                Files.list(profilesDir)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> names.add(stripExtension(p)));
            } catch (IOException e) {
                LOGGER.error("Could not list profiles", e);
            }
        }
        if (!names.contains(DEFAULT_PROFILE)) {
            names.add(DEFAULT_PROFILE);
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    // ----------------------------------------------------------- persistence

    /** Loads {@code state.json} then the active profile. Called once at startup. */
    public void loadAll() {
        if (Files.exists(stateFile)) {
            try (Reader reader = Files.newBufferedReader(stateFile)) {
                JsonObject state = GSON.fromJson(reader, JsonObject.class);
                if (state != null) {
                    if (state.has("activeProfile")) {
                        activeProfile = state.get("activeProfile").getAsString();
                    }
                    if (state.has("autoSave")) {
                        autoSave = state.get("autoSave").getAsBoolean();
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read state.json", e);
            }
        }
        loadProfile(activeProfile);
    }

    /** Serialises the current module states into the active profile. */
    public void save() {
        saveProfile(activeProfile);
        writeState();
    }

    /** Convenience used by UI callbacks - only persists when auto-save is on. */
    public void saveIfAuto() {
        if (autoSave) {
            save();
        }
    }

    public void saveProfile(String name) {
        JsonObject json = serialize();
        Path file = profileFile(name);
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save profile {}", name, e);
        }
    }

    public void loadProfile(String name) {
        Path file = profileFile(name);
        if (!Files.exists(file)) {
            // Fresh profile: keep current (default) values and write them out.
            saveProfile(name);
            activeProfile = name;
            writeState();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            deserialize(json);
            activeProfile = name;
            writeState();
        } catch (Exception e) {
            LOGGER.error("Failed to load profile {}", name, e);
        }
    }

    public void deleteProfile(String name) {
        if (DEFAULT_PROFILE.equals(name)) {
            return;
        }
        try {
            Files.deleteIfExists(profileFile(name));
        } catch (IOException e) {
            LOGGER.error("Failed to delete profile {}", name, e);
        }
        if (activeProfile.equals(name)) {
            loadProfile(DEFAULT_PROFILE);
        }
    }

    // ------------------------------------------------------- import / export

    /** Imports an external JSON file as a new profile and activates it. */
    public void importProfile(Path source, String newName) throws IOException {
        try (Reader reader = Files.newBufferedReader(source)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            deserialize(json);
        }
        activeProfile = newName;
        saveProfile(newName);
        writeState();
    }

    /** Exports the active profile (as currently applied) to an arbitrary path. */
    public void exportProfile(Path target) throws IOException {
        JsonObject json = serialize();
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (Writer writer = Files.newBufferedWriter(target)) {
            GSON.toJson(json, writer);
        }
    }

    // ------------------------------------------------------- (de)serialization

    private JsonObject serialize() {
        JsonObject root = new JsonObject();
        root.addProperty("client", "Heavenys Client");
        root.addProperty("format", 1);

        JsonObject modules = new JsonObject();
        for (Module module : moduleManager.getModules()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                addSetting(settings, setting.getName(), setting.write());
            }
            moduleJson.add("settings", settings);
            modules.add(module.getName(), moduleJson);
        }
        root.add("modules", modules);
        return root;
    }

    private void deserialize(JsonObject json) {
        if (json == null || !json.has("modules")) {
            return;
        }
        JsonObject modules = json.getAsJsonObject("modules");
        for (Module module : moduleManager.getModules()) {
            if (!modules.has(module.getName())) {
                continue;
            }
            JsonObject moduleJson = modules.getAsJsonObject(module.getName());
            if (moduleJson.has("settings")) {
                JsonObject settings = moduleJson.getAsJsonObject("settings");
                for (Setting<?> setting : module.getSettings()) {
                    if (settings.has(setting.getName())) {
                        setting.read(unwrap(settings, setting.getName()));
                    }
                }
            }
            if (moduleJson.has("enabled")) {
                module.setEnabled(moduleJson.get("enabled").getAsBoolean());
            }
        }
    }

    private static void addSetting(JsonObject target, String name, Object value) {
        if (value instanceof Boolean b) {
            target.addProperty(name, b);
        } else if (value instanceof Number n) {
            target.addProperty(name, n);
        } else if (value != null) {
            target.addProperty(name, String.valueOf(value));
        }
    }

    private static Object unwrap(JsonObject object, String name) {
        var element = object.get(name);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        var primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            return primitive.getAsDouble();
        }
        return primitive.getAsString();
    }

    private void writeState() {
        JsonObject state = new JsonObject();
        state.addProperty("activeProfile", activeProfile);
        state.addProperty("autoSave", autoSave);
        try (Writer writer = Files.newBufferedWriter(stateFile)) {
            GSON.toJson(state, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write state.json", e);
        }
    }

    private Path profileFile(String name) {
        return profilesDir.resolve(sanitize(name) + ".json");
    }

    private static String sanitize(String name) {
        String cleaned = name.trim().replaceAll("[^a-zA-Z0-9-_ ]", "_");
        return cleaned.isEmpty() ? DEFAULT_PROFILE : cleaned;
    }

    private static String stripExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
