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

import java.util.function.IntConsumer;

/**
 * A clean, fully themed 0-100% slider (Black / Yellow) — used for background opacity.
 *
 * <p>Built directly on {@link ClickableWidget} (rather than vanilla's textured SliderWidget)
 * so it matches the Heavenys aesthetic exactly. Click or drag anywhere on the track to choose
 * the value; changes are pushed live through the supplied callback.
 */
public class HeavenysSlider extends ClickableWidget {
	private final String label;
	private final IntConsumer onChange;
	private int value;

	public HeavenysSlider(int x, int y, int width, int height, String label, int value, IntConsumer onChange) {
		super(x, y, width, height, Text.empty());
		this.label = label;
		this.value = Math.max(0, Math.min(100, value));
		this.onChange = onChange;
	}

	private void setFromMouseX(double mouseX) {
		double ratio = (mouseX - (getX() + 2)) / (getWidth() - 4);
		int newValue = (int) Math.round(Math.max(0, Math.min(1, ratio)) * 100);
		if (newValue != value) {
			value = newValue;
			onChange.accept(value);
		}
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		int x = getX();
		int y = getY();
		int w = getWidth();
		int h = getHeight();

		// Track (black) + yellow border.
		context.fill(x, y, x + w, y + h, HeavenysTheme.panelBackground(70));
		context.fill(x, y, x + w, y + 1, HeavenysTheme.BORDER);
		context.fill(x, y + h - 1, x + w, y + h, HeavenysTheme.BORDER);
		context.fill(x, y, x + 1, y + h, HeavenysTheme.BORDER);
		context.fill(x + w - 1, y, x + w, y + h, HeavenysTheme.BORDER);

		// Filled portion + knob.
		int fillW = (int) ((w - 4) * (value / 100.0));
		context.fill(x + 2, y + 2, x + 2 + fillW, y + h - 2, HeavenysTheme.withAlpha(HeavenysTheme.YELLOW, 0x66));
		int knobX = x + 2 + fillW;
		context.fill(knobX - 1, y + 1, knobX + 2, y + h - 1, HeavenysTheme.YELLOW);

		// Centered label.
		Text text = HeavenysFont.text(label + ": " + value + "%");
		context.drawTextWithShadow(textRenderer, text,
				x + (w - textRenderer.getWidth(text)) / 2, y + (h - 8) / 2, HeavenysTheme.TEXT_PRIMARY);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		setFromMouseX(click.x());
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		setFromMouseX(click.x());
		return true;
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}
}
