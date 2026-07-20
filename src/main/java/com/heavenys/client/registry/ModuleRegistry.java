/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.registry;

import com.heavenys.client.HeavenysClient;
import com.heavenys.client.hud.impl.ArmorStatusHud;
import com.heavenys.client.hud.impl.ArrowCounterHud;
import com.heavenys.client.hud.impl.BossBarModule;
import com.heavenys.client.hud.impl.ClockHud;
import com.heavenys.client.hud.impl.ComboCounterHud;
import com.heavenys.client.hud.impl.CoordinatesHud;
import com.heavenys.client.hud.impl.CpsHud;
import com.heavenys.client.hud.impl.DirectionHud;
import com.heavenys.client.hud.impl.DurabilityHud;
import com.heavenys.client.hud.impl.EndCrystalCounterHud;
import com.heavenys.client.hud.impl.EnderPearlCounterHud;
import com.heavenys.client.hud.impl.FpsHud;
import com.heavenys.client.hud.impl.InventoryHud;
import com.heavenys.client.hud.impl.KeystrokesHud;
import com.heavenys.client.hud.impl.PingHud;
import com.heavenys.client.hud.impl.PotionEffectsHud;
import com.heavenys.client.hud.impl.ScoreboardModule;
import com.heavenys.client.hud.impl.SessionStatsHud;
import com.heavenys.client.hud.impl.TotemCounterHud;
import com.heavenys.client.hud.impl.TpsHud;
import com.heavenys.client.hud.impl.WatermarkHud;
import com.heavenys.client.module.ModuleManager;
import com.heavenys.client.module.impl.client.InterfaceModule;
import com.heavenys.client.module.impl.compat.CompatibilityModule;
import com.heavenys.client.module.impl.compat.VoiceChatModule;
import com.heavenys.client.module.impl.visual.ChatCustomizerModule;
import com.heavenys.client.module.impl.visual.ClearWaterModule;
import com.heavenys.client.module.impl.visual.CrosshairModule;
import com.heavenys.client.module.impl.visual.FullbrightModule;
import com.heavenys.client.module.impl.visual.GuiScaleModule;
import com.heavenys.client.module.impl.visual.ItemPhysicsModule;
import com.heavenys.client.module.impl.visual.MotionBlurModule;
import com.heavenys.client.module.impl.visual.TimeChangerModule;
import com.heavenys.client.module.impl.visual.WeatherChangerModule;
import com.heavenys.client.module.impl.visual.ZoomModule;

/**
 * Single place that wires up every module. Keeping registration here (rather
 * than scattered across constructors) makes the full feature set easy to audit.
 */
public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static void registerAll(HeavenysClient client) {
        ModuleManager m = client.getModuleManager();

        // Interface / theme (must exist for the UI and HUD to read colors).
        m.register(new InterfaceModule());

        // HUD elements.
        m.register(new FpsHud());
        m.register(new PingHud());
        m.register(new TpsHud());
        m.register(new CpsHud());
        m.register(new ComboCounterHud());
        m.register(new CoordinatesHud());
        m.register(new DirectionHud());
        m.register(new ClockHud());
        m.register(new ArmorStatusHud());
        m.register(new InventoryHud());
        m.register(new PotionEffectsHud());
        m.register(new KeystrokesHud());
        m.register(new WatermarkHud());
        m.register(new SessionStatsHud());

        // PvP / survival counters (read-only inventory inspection, no cheats).
        m.register(new TotemCounterHud());
        m.register(new EndCrystalCounterHud());
        m.register(new EnderPearlCounterHud());
        m.register(new ArrowCounterHud());
        m.register(new DurabilityHud());

        // HUD elements that toggle vanilla overlays.
        ScoreboardModule scoreboard = m.register(new ScoreboardModule());
        BossBarModule bossBar = m.register(new BossBarModule());
        scoreboard.install();
        bossBar.install();

        // Visual tweaks.
        m.register(new ZoomModule());
        m.register(new MotionBlurModule());
        m.register(new FullbrightModule());
        m.register(new TimeChangerModule());
        m.register(new WeatherChangerModule());
        m.register(new ClearWaterModule());
        m.register(new ItemPhysicsModule());
        m.register(new GuiScaleModule());
        m.register(new CrosshairModule());
        m.register(new ChatCustomizerModule());

        // Compatibility + voice chat.
        m.register(new CompatibilityModule());
        m.register(new VoiceChatModule());
    }
}
