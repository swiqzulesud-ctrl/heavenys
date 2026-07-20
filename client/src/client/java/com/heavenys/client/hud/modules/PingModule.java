package com.heavenys.client.hud.modules;

import com.heavenys.client.hud.HudModule;
import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Displays the local player's ping (round-trip latency to the current server).
 *
 * <p>Reads only the latency value the vanilla server already reports for the local player
 * via the player list — no timing tricks or extra packets.
 */
public class PingModule extends HudModule {
	public PingModule() {
		super("ping", "heavenys.module.ping", true, 4, 18);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter,
			MinecraftClient client, TextRenderer textRenderer) {
		String value = "N/A";
		ClientPlayNetworkHandler handler = client.getNetworkHandler();
		if (handler != null && client.player != null) {
			PlayerListEntry entry = handler.getPlayerListEntry(client.player.getUuid());
			if (entry != null) {
				value = entry.getLatency() + " ms";
			}
		}

		Text label = HeavenysFont.text("Ping: " + value);
		int width = textRenderer.getWidth(label) + PADDING * 2;
		int height = LINE_HEIGHT + PADDING;

		drawPanel(context, getX(), getY(), width, height);
		context.drawTextWithShadow(textRenderer, label,
				getX() + PADDING + 2, getY() + PADDING, HeavenysTheme.TEXT_PRIMARY);
	}
}
