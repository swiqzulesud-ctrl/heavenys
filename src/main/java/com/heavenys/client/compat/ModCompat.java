/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.compat;

import com.heavenys.client.HeavenysClient;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects popular performance and visual mods so Heavenys Client can integrate
 * with them rather than fight them. Detection is purely read-only via
 * {@link FabricLoader#isModLoaded(String)} - Heavenys never bundles or ships any
 * of these mods.
 */
public final class ModCompat {

    /** A third-party mod Heavenys knows how to co-exist with. */
    public enum Integration {
        SODIUM("sodium", "Sodium"),
        LITHIUM("lithium", "Lithium"),
        FERRITE_CORE("ferritecore", "FerriteCore"),
        ENTITY_CULLING("entityculling", "Entity Culling"),
        IMMEDIATELY_FAST("immediatelyfast", "ImmediatelyFast"),
        DYNAMIC_FPS("dynamic_fps", "Dynamic FPS"),
        MORE_CULLING("moreculling", "More Culling"),
        ENHANCED_BLOCK_ENTITIES("enhancedblockentities", "Enhanced Block Entities"),
        NOISIUM("noisium", "Noisium"),
        SIMPLE_VOICE_CHAT("voicechat", "Simple Voice Chat");

        private final String modId;
        private final String displayName;

        Integration(String modId, String displayName) {
            this.modId = modId;
            this.displayName = displayName;
        }

        public String getModId() {
            return modId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isPresent() {
            return FabricLoader.getInstance().isModLoaded(modId);
        }
    }

    private ModCompat() {
    }

    public static boolean isPresent(Integration integration) {
        return integration.isPresent();
    }

    public static boolean isVoiceChatPresent() {
        return Integration.SIMPLE_VOICE_CHAT.isPresent();
    }

    public static List<Integration> detected() {
        List<Integration> list = new ArrayList<>();
        for (Integration integration : Integration.values()) {
            if (integration.isPresent()) {
                list.add(integration);
            }
        }
        return list;
    }

    /** Logs a one-line summary of detected integrations at startup. */
    public static void logDetected() {
        List<Integration> detected = detected();
        if (detected.isEmpty()) {
            HeavenysClient.LOGGER.info("No known performance/visual integrations detected.");
            return;
        }
        StringBuilder sb = new StringBuilder("Detected integrations: ");
        for (int i = 0; i < detected.size(); i++) {
            sb.append(detected.get(i).getDisplayName());
            if (i < detected.size() - 1) {
                sb.append(", ");
            }
        }
        HeavenysClient.LOGGER.info(sb.toString());
    }
}
