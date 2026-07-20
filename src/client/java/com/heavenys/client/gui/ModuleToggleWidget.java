package com.heavenys.client.gui;

import com.heavenys.client.hud.HudManager;
import com.heavenys.client.hud.HudModule;
import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * A custom, fully themed toggle row (Black / Yellow) used in the Heavenys menu.
 *
 * <p>Implemented as a lightweight {@link ClickableWidget} so it inherits vanilla's
 * focus/click/narration handling while giving us complete control over the visuals — a
 * clean label on the left and a pill switch on the right.
 */
public class ModuleToggleWidget extends ClickableWidget {
	private final HudModule module;
	private final HudManager manager;

	public ModuleToggleWidget(int x, int y, int width, int height, HudModule module, HudManager manager) {
		super(x, y, width, height, HeavenysFont.apply(Text.translatable(module.getTranslationKey())));
		this.module = module;
		this.manager = manager;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		boolean on = module.isEnabled();
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();

		// Row body + yellow border (brighter on hover).
		context.fill(x, y, x + w, y + h, HeavenysTheme.panelBackground(isHovered() ? 85 : 55));
		int border = isHovered() ? HeavenysTheme.YELLOW_BRIGHT : HeavenysTheme.BORDER;
		context.fill(x, y, x + w, y + 1, border);
		context.fill(x, y + h - 1, x + w, y + h, border);
		context.fill(x, y, x + 1, y + h, border);
		context.fill(x + w - 1, y, x + w, y + h, border);

		// Module label (left aligned).
		context.drawTextWithShadow(textRenderer, getMessage(), x + 8, y + (h - 8) / 2, HeavenysTheme.TEXT_PRIMARY);

		// Toggle pill (right aligned).
		int pillW = 26;
		int pillH = 12;
		int pillX = x + w - pillW - 8;
		int pillY = y + (h - pillH) / 2;
		context.fill(pillX, pillY, pillX + pillW, pillY + pillH, on ? HeavenysTheme.YELLOW : HeavenysTheme.TOGGLE_OFF);

		int knob = 10;
		int knobX = on ? pillX + pillW - knob - 1 : pillX + 1;
		int knobY = pillY + 1;
		context.fill(knobX, knobY, knobX + knob, knobY + knob, on ? 0xFF101010 : HeavenysTheme.WHITE);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		manager.toggle(module);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
