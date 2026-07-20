/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 *
 * ----------------------------------------------------------------------------
 * Heavenys Client is original work created for the Fabric mod loader on modern
 * Minecraft. It is inspired by the open-source Sol Client project (GPL-3.0) but
 * contains no code from it, and no proprietary Lunar Client code or assets. See
 * the NOTICE file for attribution details.
 * ----------------------------------------------------------------------------
 */
package com.heavenys.client;

import com.heavenys.client.compat.ModCompat;
import com.heavenys.client.config.ConfigManager;
import com.heavenys.client.hud.HudModule;
import com.heavenys.client.module.ModuleManager;
import com.heavenys.client.module.impl.client.InterfaceModule;
import com.heavenys.client.registry.ModuleRegistry;
import com.heavenys.client.gui.ClickGuiScreen;
import com.heavenys.client.input.InputTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric client entrypoint and service locator for Heavenys Client.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Instantiates and holds the {@link ModuleManager}, {@link ConfigManager},
 *       {@link InputTracker} and the shared {@link InterfaceModule} theme.</li>
 *   <li>Registers keybinds (ClickGUI + HUD editor) and the HUD render layer.</li>
 *   <li>Drives per-tick work: input polling and keybind handling.</li>
 * </ul>
 */
public class HeavenysClient implements ClientModInitializer {

    public static final String MOD_ID = "heavenys";
    public static final String NAME = "Heavenys Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static HeavenysClient instance;

    private final ModuleManager moduleManager = new ModuleManager();
    private final InputTracker inputTracker = new InputTracker();
    private final com.heavenys.client.util.TpsTracker tpsTracker = new com.heavenys.client.util.TpsTracker();
    private final List<HudModule> hudModules = new ArrayList<>();

    private InterfaceModule interfaceModule;
    private ConfigManager configManager;

    private KeyMapping openGuiKey;
    private KeyMapping hudEditorKey;

    public static HeavenysClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initialising {} on Minecraft 26.2 (Fabric)", NAME);

        // Populate every module (theme, HUD, visual, compatibility, voice).
        ModuleRegistry.registerAll(this);
        for (var module : moduleManager.getModules()) {
            if (module instanceof HudModule hud) {
                hudModules.add(hud);
            }
            if (module instanceof InterfaceModule ui) {
                interfaceModule = ui;
            }
        }

        // Config must load after modules exist so their settings can be restored.
        configManager = new ConfigManager(moduleManager);
        configManager.loadAll();

        ModCompat.logDetected();
        registerKeybinds();
        registerHudLayer();
        registerTickHandlers();

        LOGGER.info("{} ready - {} modules registered", NAME, moduleManager.getModules().size());
    }

    private void registerKeybinds() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.heavenys.clickgui", GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        hudEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.heavenys.hudeditor", GLFW.GLFW_KEY_RIGHT_CONTROL, KeyMapping.Category.MISC));
    }

    private void registerHudLayer() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                (GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) -> renderHud(graphics));
    }

    private void registerTickHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            inputTracker.update(mc);

            for (var module : moduleManager.getModules()) {
                module.onClientTick(mc);
            }

            while (openGuiKey.consumeClick()) {
                mc.gui.setScreen(new ClickGuiScreen());
            }
            while (hudEditorKey.consumeClick()) {
                mc.gui.setScreen(new com.heavenys.client.gui.HudEditorScreen());
            }
        });

        ClientTickEvents.START_LEVEL_TICK.register(level -> tpsTracker.onLevelTick());

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> configManager.save());
    }

    /** Draws all enabled HUD modules. Only runs when no full-screen GUI is open. */
    private void renderHud(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        for (HudModule hud : hudModules) {
            if (hud.isEnabled()) {
                try {
                    hud.render(graphics, mc.font);
                } catch (Exception e) {
                    LOGGER.error("HUD module '{}' failed to render", hud.getName(), e);
                    hud.setEnabled(false);
                }
            }
        }
    }

    // -------------------------------------------------------------- accessors

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public InputTracker getInputTracker() {
        return inputTracker;
    }

    public com.heavenys.client.util.TpsTracker getTpsTracker() {
        return tpsTracker;
    }

    public List<HudModule> getHudModules() {
        return hudModules;
    }

    public InterfaceModule getInterface() {
        return interfaceModule;
    }
}
