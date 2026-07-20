package com.heavenys.client.gui;

import com.heavenys.client.config.HeavenysConfig;
import com.heavenys.client.hud.HudManager;
import com.heavenys.client.hud.HudModule;
import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game Heavenys configuration menu.
 *
 * <p>A single, clean Black/Yellow card organised into clear sections:
 * <ul>
 *     <li><b>Appearance</b> — background opacity slider + client-font toggle</li>
 *     <li><b>HUD Modules</b> — a toggle switch per module</li>
 *     <li><b>Game</b> — window resolution + fullscreen</li>
 *     <li><b>System</b> — allocated RAM &amp; signed-in account (launcher-managed, shown for clarity)</li>
 * </ul>
 * Opened via the configurable key bind (default: Right Shift).
 */
public class HeavenysConfigScreen extends Screen {
	private static final int CARD_WIDTH = 360;
	private static final int ROW_H = 18;
	private static final int ROW_GAP = 4;
	private static final int SECTION_GAP = 14;

	// Selectable window resolutions.
	private static final int[][] RESOLUTIONS = {
			{854, 480}, {1280, 720}, {1600, 900}, {1920, 1080}
	};

	private final Screen parent;
	private final HudManager manager;
	private final HeavenysConfig config = HeavenysConfig.get();

	private int cardX;
	private int cardY;
	private int cardHeight;

	private final List<Label> labels = new ArrayList<>();
	private int[] ramPos = {0, 0};
	private int[] accountPos = {0, 0};

	public HeavenysConfigScreen(Screen parent, HudManager manager) {
		super(Text.translatable("heavenys.screen.title"));
		this.parent = parent;
		this.manager = manager;
	}

	@Override
	protected void init() {
		labels.clear();

		int leftContent = SECTION_GAP + (ROW_H + ROW_GAP) * 2 + SECTION_GAP
				+ (ROW_H + ROW_GAP) * manager.getModules().size();
		cardHeight = 52 + leftContent + 34;
		cardX = this.width / 2 - CARD_WIDTH / 2;
		cardY = Math.max(10, this.height / 2 - cardHeight / 2);

		int colW = CARD_WIDTH / 2 - 24;
		int leftX = cardX + 16;
		int rightX = cardX + CARD_WIDTH / 2 + 8;

		// ---- Left column ----
		int y = cardY + 52;
		labels.add(new Label(leftX, y, "Appearance", HeavenysTheme.TEXT_YELLOW));
		y += SECTION_GAP;
		addDrawableChild(new HeavenysSlider(leftX, y, colW, ROW_H, "Opacity",
				config.getBackgroundOpacity(), config::setBackgroundOpacity));
		y += ROW_H + ROW_GAP;
		addDrawableChild(new HeavenysButton(leftX, y, colW, ROW_H,
				() -> "Client Font: " + (config.isCustomFont() ? "ON" : "OFF"),
				() -> config.setCustomFont(!config.isCustomFont())));
		y += ROW_H + ROW_GAP + SECTION_GAP;

		labels.add(new Label(leftX, y - SECTION_GAP, "HUD Modules", HeavenysTheme.TEXT_YELLOW));
		for (HudModule module : manager.getModules()) {
			addDrawableChild(new ModuleToggleWidget(leftX, y, colW, ROW_H, module, manager));
			y += ROW_H + ROW_GAP;
		}

		// ---- Right column ----
		int ry = cardY + 52;
		labels.add(new Label(rightX, ry, "Game", HeavenysTheme.TEXT_YELLOW));
		ry += SECTION_GAP;
		addDrawableChild(new HeavenysButton(rightX, ry, colW, ROW_H,
				() -> {
					int[] r = RESOLUTIONS[config.getResolutionIndex() % RESOLUTIONS.length];
					return r[0] + "x" + r[1];
				},
				this::cycleResolution));
		ry += ROW_H + ROW_GAP;
		addDrawableChild(new HeavenysButton(rightX, ry, colW, ROW_H,
				() -> "Fullscreen", this::toggleFullscreen));
		ry += ROW_H + ROW_GAP + SECTION_GAP;

		labels.add(new Label(rightX, ry - SECTION_GAP, "System", HeavenysTheme.TEXT_YELLOW));
		ramPos = new int[]{rightX, ry};
		ry += 12;
		accountPos = new int[]{rightX, ry};

		// ---- Footer ----
		int buttonW = 120;
		addDrawableChild(new HeavenysButton(this.width / 2 - buttonW / 2, cardY + cardHeight - 26, buttonW, ROW_H,
				() -> Text.translatable("heavenys.screen.done").getString(), this::close));
	}

	private void cycleResolution() {
		int next = (config.getResolutionIndex() + 1) % RESOLUTIONS.length;
		config.setResolutionIndex(next);
		int[] r = RESOLUTIONS[next];
		MinecraftClient client = MinecraftClient.getInstance();
		if (!client.getWindow().isFullscreen()) {
			client.getWindow().setWindowedSize(r[0], r[1]);
		}
	}

	private void toggleFullscreen() {
		MinecraftClient.getInstance().getWindow().toggleFullscreen();
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);

		// Black card body.
		context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + cardHeight, HeavenysTheme.panelBackground(88));
		// Yellow header bar.
		context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + 40, HeavenysTheme.panelBackground(94));
		context.fill(cardX, cardY + 40, cardX + CARD_WIDTH, cardY + 41, HeavenysTheme.YELLOW);
		// Outer yellow accent border.
		context.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + 2, HeavenysTheme.YELLOW);
		context.fill(cardX, cardY + cardHeight - 2, cardX + CARD_WIDTH, cardY + cardHeight, HeavenysTheme.YELLOW);
		context.fill(cardX, cardY, cardX + 2, cardY + cardHeight, HeavenysTheme.YELLOW);
		context.fill(cardX + CARD_WIDTH - 2, cardY, cardX + CARD_WIDTH, cardY + cardHeight, HeavenysTheme.YELLOW);

		// Vertical divider between the two columns.
		int divX = cardX + CARD_WIDTH / 2 - 4;
		context.fill(divX, cardY + 48, divX + 1, cardY + cardHeight - 30, HeavenysTheme.withAlpha(HeavenysTheme.YELLOW, 0x55));

		drawLogo(context, cardX + 12, cardY + 8, 24);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		// Title + subtitle in the client font.
		context.drawTextWithShadow(this.textRenderer, HeavenysFont.text("Heavenys Client"),
				cardX + 44, cardY + 11, HeavenysTheme.YELLOW);
		context.drawTextWithShadow(this.textRenderer, HeavenysFont.text("Clean HUD  ·  Black & Yellow"),
				cardX + 44, cardY + 24, HeavenysTheme.TEXT_MUTED);

		for (Label label : labels) {
			context.drawTextWithShadow(this.textRenderer, HeavenysFont.text(label.text), label.x, label.y, label.color);
		}

		// System info (launcher-managed, shown for clarity).
		MinecraftClient client = MinecraftClient.getInstance();
		long maxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		context.drawTextWithShadow(this.textRenderer, HeavenysFont.text("RAM: " + maxMb + " MB"),
				ramPos[0], ramPos[1], HeavenysTheme.TEXT_PRIMARY);
		String user = client.getSession() != null ? client.getSession().getUsername() : "-";
		context.drawTextWithShadow(this.textRenderer, HeavenysFont.text("User: " + user),
				accountPos[0], accountPos[1], HeavenysTheme.TEXT_PRIMARY);
	}

	/** Draws the simple "H" logo (black tile, yellow border, yellow H). */
	private void drawLogo(DrawContext context, int x, int y, int size) {
		context.fill(x, y, x + size, y + size, HeavenysTheme.panelBackground(95));
		context.fill(x, y, x + size, y + 1, HeavenysTheme.YELLOW);
		context.fill(x, y + size - 1, x + size, y + size, HeavenysTheme.YELLOW);
		context.fill(x, y, x + 1, y + size, HeavenysTheme.YELLOW);
		context.fill(x + size - 1, y, x + size, y + size, HeavenysTheme.YELLOW);

		int bar = Math.max(2, size / 8);
		int inset = size / 4;
		// Left & right legs.
		context.fill(x + inset, y + inset, x + inset + bar, y + size - inset, HeavenysTheme.YELLOW);
		context.fill(x + size - inset - bar, y + inset, x + size - inset, y + size - inset, HeavenysTheme.YELLOW);
		// Crossbar.
		context.fill(x + inset, y + size / 2 - bar / 2, x + size - inset, y + size / 2 + bar / 2 + (bar % 2), HeavenysTheme.YELLOW);
	}

	@Override
	public void close() {
		MinecraftClient.getInstance().setScreen(parent);
	}

	private record Label(int x, int y, String text, int color) {
	}
}
