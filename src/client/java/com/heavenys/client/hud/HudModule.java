package com.heavenys.client.hud;

import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Base class for every Heavenys HUD module.
 *
 * <p>Modules are intentionally self-contained and stateless with respect to the game:
 * they only <i>read</i> client-side information (already known to the local player) and
 * draw it. This keeps every module strictly rule-compliant — no module touches server
 * combat data, entity positions, or anything that would constitute an unfair advantage.
 */
public abstract class HudModule {
	protected static final int PADDING = 4;
	protected static final int LINE_HEIGHT = 10;

	private final String id;
	private final String translationKey;
	private final boolean defaultEnabled;

	private boolean enabled;
	private int x;
	private int y;

	protected HudModule(String id, String translationKey, boolean defaultEnabled, int x, int y) {
		this.id = id;
		this.translationKey = translationKey;
		this.defaultEnabled = defaultEnabled;
		this.enabled = defaultEnabled;
		this.x = x;
		this.y = y;
	}

	public String getId() {
		return id;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	public boolean isDefaultEnabled() {
		return defaultEnabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Draws a clean, themed rounded-ish panel (white translucent body + butter accent bar).
	 * Shared by all modules so the whole HUD stays visually consistent.
	 */
	protected void drawPanel(DrawContext context, int x, int y, int width, int height) {
		// Black body at the user-configured opacity.
		context.fill(x, y, x + width, y + height, HeavenysTheme.panelBackground());
		// Yellow accent strip down the left edge.
		context.fill(x, y, x + 2, y + height, HeavenysTheme.YELLOW);
		// Thin yellow border.
		context.fill(x, y, x + width, y + 1, HeavenysTheme.BORDER);
		context.fill(x, y + height - 1, x + width, y + height, HeavenysTheme.BORDER);
		context.fill(x + width - 1, y, x + width, y + height, HeavenysTheme.BORDER);
	}

	/**
	 * Renders this module. Implementations should draw relative to {@link #getX()}/{@link #getY()}.
	 */
	public abstract void render(DrawContext context, RenderTickCounter tickCounter,
			MinecraftClient client, TextRenderer textRenderer);
}
