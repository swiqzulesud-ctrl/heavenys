package com.heavenys.client.hud.modules;

import com.heavenys.client.hud.HudModule;
import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Minimal WASD + mouse keystroke overlay.
 *
 * <p>Purely visual: it only reflects the state of the player's own configured key
 * bindings ({@link KeyBinding#isPressed()}). It performs no input automation of any kind
 * (no auto-clicker / macro), keeping it competitive-server safe.
 */
public class KeystrokesModule extends HudModule {
	private static final int KEY = 16;
	private static final int GAP = 2;

	public KeystrokesModule() {
		super("keystrokes", "heavenys.module.keystrokes", true, 4, 150);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter,
			MinecraftClient client, TextRenderer textRenderer) {
		if (client.options == null) {
			return;
		}

		int rowWidth = KEY * 3 + GAP * 2;
		int ox = getX();
		int oy = getY();

		// Row 1: W (centered).
		drawKey(context, textRenderer, ox + KEY + GAP, oy, KEY, KEY, "W", client.options.forwardKey.isPressed());
		// Row 2: A S D.
		int row2 = oy + KEY + GAP;
		drawKey(context, textRenderer, ox, row2, KEY, KEY, "A", client.options.leftKey.isPressed());
		drawKey(context, textRenderer, ox + KEY + GAP, row2, KEY, KEY, "S", client.options.backKey.isPressed());
		drawKey(context, textRenderer, ox + (KEY + GAP) * 2, row2, KEY, KEY, "D", client.options.rightKey.isPressed());
		// Row 3: LMB / RMB.
		int row3 = oy + (KEY + GAP) * 2;
		int mouseW = (rowWidth - GAP) / 2;
		drawKey(context, textRenderer, ox, row3, mouseW, KEY, "LMB", client.options.attackKey.isPressed());
		drawKey(context, textRenderer, ox + mouseW + GAP, row3, mouseW, KEY, "RMB", client.options.useKey.isPressed());
		// Row 4: SPACE bar.
		int row4 = oy + (KEY + GAP) * 3;
		drawKey(context, textRenderer, ox, row4, rowWidth, 8, "", client.options.jumpKey.isPressed());
	}

	private void drawKey(DrawContext context, TextRenderer textRenderer,
			int x, int y, int width, int height, String label, boolean pressed) {
		int bg = pressed ? HeavenysTheme.YELLOW : HeavenysTheme.panelBackground();
		// Pressed keys use black-on-yellow, released keys use white-on-black.
		int textColor = pressed ? 0xFF101010 : HeavenysTheme.TEXT_PRIMARY;

		context.fill(x, y, x + width, y + height, bg);
		context.fill(x, y, x + width, y + 1, HeavenysTheme.BORDER);
		context.fill(x, y + height - 1, x + width, y + height, HeavenysTheme.BORDER);
		context.fill(x, y, x + 1, y + height, HeavenysTheme.BORDER);
		context.fill(x + width - 1, y, x + width, y + height, HeavenysTheme.BORDER);

		if (!label.isEmpty()) {
			Text text = HeavenysFont.text(label);
			int textX = x + (width - textRenderer.getWidth(text)) / 2;
			int textY = y + (height - 8) / 2;
			context.drawTextWithShadow(textRenderer, text, textX, textY, textColor);
		}
	}
}
