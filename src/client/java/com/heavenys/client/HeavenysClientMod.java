package com.heavenys.client;

import com.heavenys.HeavenysClient;
import com.heavenys.client.config.HeavenysConfig;
import com.heavenys.client.gui.HeavenysConfigScreen;
import com.heavenys.client.hud.HudManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

/**
 * Client entrypoint for Heavenys Client.
 *
 * <p>Bootstraps the modular HUD system and registers the (rebindable) menu key. Everything
 * here is deliberately additive and non-invasive: no mixins into combat/networking, no
 * automation — just a HUD overlay and a settings screen.
 */
public class HeavenysClientMod implements ClientModInitializer {
	private static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		HeavenysConfig config = HeavenysConfig.get();
		HudManager hudManager = new HudManager(config);
		hudManager.init();

		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"heavenys.key.openMenu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.wasPressed()) {
				if (client.player != null) {
					client.setScreen(new HeavenysConfigScreen(client.currentScreen, hudManager));
				}
			}
		});

		HeavenysClient.LOGGER.info("[Heavenys Client] Client ready — press RIGHT SHIFT to open the menu.");
	}
}
