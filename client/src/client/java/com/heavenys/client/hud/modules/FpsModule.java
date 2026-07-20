package com.heavenys.client.hud.modules;

import com.heavenys.client.hud.HudModule;
import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Displays the current client frame-rate. Purely a local performance read-out.
 */
public class FpsModule extends HudModule {
	public FpsModule() {
		super("fps", "heavenys.module.fps", true, 4, 4);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter,
			MinecraftClient client, TextRenderer textRenderer) {
		Text label = HeavenysFont.text("FPS: " + client.getCurrentFps());
		int width = textRenderer.getWidth(label) + PADDING * 2;
		int height = LINE_HEIGHT + PADDING;

		drawPanel(context, getX(), getY(), width, height);
		context.drawTextWithShadow(textRenderer, label,
				getX() + PADDING + 2, getY() + PADDING, HeavenysTheme.TEXT_PRIMARY);
	}
}
