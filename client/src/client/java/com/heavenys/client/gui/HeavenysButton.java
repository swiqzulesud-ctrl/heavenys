package com.heavenys.client.gui;

import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/**
 * A clean, themed action button (Black / Yellow) with a live-updating label.
 *
 * <p>Used for the "cycle"-style settings (resolution, fullscreen, font toggle). The label is a
 * {@link Supplier} so the button reflects the current value without extra bookkeeping.
 */
public class HeavenysButton extends ClickableWidget {
	private final Supplier<String> labelSupplier;
	private final Runnable action;

	public HeavenysButton(int x, int y, int width, int height, Supplier<String> labelSupplier, Runnable action) {
		super(x, y, width, height, Text.empty());
		this.labelSupplier = labelSupplier;
		this.action = action;
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();

		context.fill(x, y, x + w, y + h, HeavenysTheme.panelBackground(isHovered() ? 90 : 60));
		int border = isHovered() ? HeavenysTheme.YELLOW_BRIGHT : HeavenysTheme.BORDER;
		context.fill(x, y, x + w, y + 1, border);
		context.fill(x, y + h - 1, x + w, y + h, border);
		context.fill(x, y, x + 1, y + h, border);
		context.fill(x + w - 1, y, x + w, y + h, border);

		Text text = HeavenysFont.text(labelSupplier.get());
		context.drawTextWithShadow(textRenderer, text,
				x + (w - textRenderer.getWidth(text)) / 2, y + (h - 8) / 2,
				isHovered() ? HeavenysTheme.YELLOW_BRIGHT : HeavenysTheme.TEXT_PRIMARY);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		action.run();
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
