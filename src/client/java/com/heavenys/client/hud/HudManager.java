package com.heavenys.client.hud;

import com.heavenys.HeavenysClient;
import com.heavenys.client.config.HeavenysConfig;
import com.heavenys.client.hud.modules.ArmorStatusModule;
import com.heavenys.client.hud.modules.FpsModule;
import com.heavenys.client.hud.modules.KeystrokesModule;
import com.heavenys.client.hud.modules.PingModule;
import com.heavenys.client.hud.modules.PotionStatusModule;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns every HUD module and wires them into the game's HUD render pipeline.
 *
 * <p>The manager registers a <em>single</em> {@code HudElement} with Fabric's modern
 * {@link HudElementRegistry}; that element then delegates to each enabled module. This keeps
 * the render hook count to exactly one (lightweight) while remaining fully modular — adding a
 * new module is just one {@code register(...)} call.
 */
public final class HudManager {
	private static final Identifier HUD_ID = Identifier.of(HeavenysClient.MOD_ID, "hud_overlay");

	private final List<HudModule> modules = new ArrayList<>();
	private final HeavenysConfig config;

	public HudManager(HeavenysConfig config) {
		this.config = config;
	}

	public void init() {
		register(new FpsModule());
		register(new PingModule());
		register(new ArmorStatusModule());
		register(new PotionStatusModule());
		register(new KeystrokesModule());

		HudElementRegistry.addLast(HUD_ID, (context, tickCounter) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.options.hudHidden) {
				return;
			}

			for (HudModule module : modules) {
				if (module.isEnabled()) {
					module.render(context, tickCounter, client, client.textRenderer);
				}
			}
		});

		HeavenysClient.LOGGER.info("[Heavenys Client] Registered {} HUD modules.", modules.size());
	}

	private void register(HudModule module) {
		module.setEnabled(config.isEnabled(module.getId(), module.isDefaultEnabled()));
		modules.add(module);
	}

	public List<HudModule> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public void toggle(HudModule module) {
		module.setEnabled(!module.isEnabled());
		config.setEnabled(module.getId(), module.isEnabled());
	}
}
