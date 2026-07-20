package com.heavenys.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.heavenys.HeavenysClient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny, dependency-free persistence layer for Heavenys settings.
 *
 * <p>Stores module toggles plus a few appearance/gameplay preferences as JSON in
 * {@code <config>/heavenys.json}. A single shared instance is exposed via {@link #get()} so
 * the theme, fonts and HUD modules can all read the live settings without extra plumbing.
 */
public final class HeavenysConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH =
			FabricLoader.getInstance().getConfigDir().resolve("heavenys.json");

	private static HeavenysConfig current;

	private final Map<String, Boolean> enabledModules = new HashMap<>();

	/** Background opacity for HUD panels & menu cards, 0-100 (%). */
	private int backgroundOpacity = 70;
	/** Whether the clean client font is used for Heavenys UI (does not affect the game font). */
	private boolean customFont = true;
	/** Index into the resolution preset list. */
	private int resolutionIndex = 1;

	public static HeavenysConfig get() {
		if (current == null) {
			current = load();
		}
		return current;
	}

	public boolean isEnabled(String moduleId, boolean defaultValue) {
		return enabledModules.getOrDefault(moduleId, defaultValue);
	}

	public void setEnabled(String moduleId, boolean enabled) {
		enabledModules.put(moduleId, enabled);
		save();
	}

	public int getBackgroundOpacity() {
		return backgroundOpacity;
	}

	public void setBackgroundOpacity(int opacity) {
		this.backgroundOpacity = Math.max(0, Math.min(100, opacity));
		save();
	}

	public boolean isCustomFont() {
		return customFont;
	}

	public void setCustomFont(boolean customFont) {
		this.customFont = customFont;
		save();
	}

	public int getResolutionIndex() {
		return resolutionIndex;
	}

	public void setResolutionIndex(int resolutionIndex) {
		this.resolutionIndex = resolutionIndex;
		save();
	}

	public static HeavenysConfig load() {
		HeavenysConfig config = new HeavenysConfig();
		try {
			if (Files.exists(CONFIG_PATH)) {
				Data data = GSON.fromJson(Files.readString(CONFIG_PATH), Data.class);
				if (data != null) {
					if (data.enabledModules != null) {
						config.enabledModules.putAll(data.enabledModules);
					}
					config.backgroundOpacity = Math.max(0, Math.min(100, data.backgroundOpacity));
					config.customFont = data.customFont;
					config.resolutionIndex = data.resolutionIndex;
				}
			}
		} catch (IOException | RuntimeException e) {
			HeavenysClient.LOGGER.warn("[Heavenys Client] Failed to load config, using defaults", e);
		}
		current = config;
		return config;
	}

	public void save() {
		try {
			Data data = new Data();
			data.enabledModules = enabledModules;
			data.backgroundOpacity = backgroundOpacity;
			data.customFont = customFont;
			data.resolutionIndex = resolutionIndex;
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(data));
		} catch (IOException e) {
			HeavenysClient.LOGGER.warn("[Heavenys Client] Failed to save config", e);
		}
	}

	private static final class Data {
		Map<String, Boolean> enabledModules;
		int backgroundOpacity = 70;
		boolean customFont = true;
		int resolutionIndex = 1;
	}
}
