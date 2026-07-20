package com.heavenys.client.theme;

import com.heavenys.client.config.HeavenysConfig;

/**
 * Central colour palette for Heavenys Client.
 *
 * <p>The client uses a clean <b>Black</b> and <b>Yellow</b> aesthetic. Panel backgrounds are
 * pure black at a user-configurable opacity, with vivid yellow accents/borders and crisp white
 * text for maximum legibility over any world background. Keeping every colour here means the
 * whole UI (HUD overlays + in-game menu) stays consistent and re-theming is a one-file change.
 *
 * <p>All values are packed 0xAARRGGBB integers as expected by {@code DrawContext}.
 */
public final class HeavenysTheme {
	private HeavenysTheme() {
	}

	/** Vivid brand yellow — accents, borders, active state. */
	public static final int YELLOW = 0xFFFFD400;
	/** Brighter yellow for highlights / hover. */
	public static final int YELLOW_BRIGHT = 0xFFFFE24D;
	/** Pure white — primary text. */
	public static final int WHITE = 0xFFFFFFFF;

	/** Crisp white primary text. */
	public static final int TEXT_PRIMARY = 0xFFFFFFFF;
	/** Yellow secondary text / section headers. */
	public static final int TEXT_YELLOW = 0xFFFFD400;
	/** Muted grey for hints. */
	public static final int TEXT_MUTED = 0xFFB4B4B4;

	/** Yellow accent border. */
	public static final int BORDER = 0xFFFFD400;

	/** Disabled toggle colour (muted grey). */
	public static final int TOGGLE_OFF = 0x55FFFFFF;

	/**
	 * Black panel background at the user-configured opacity.
	 */
	public static int panelBackground() {
		int alpha = Math.round(HeavenysConfig.get().getBackgroundOpacity() * 255f / 100f);
		return (alpha << 24);
	}

	/**
	 * Black panel background at an explicit opacity percentage (used for menu chrome).
	 */
	public static int panelBackground(int opacityPercent) {
		int alpha = Math.round(Math.max(0, Math.min(100, opacityPercent)) * 255f / 100f);
		return (alpha << 24);
	}

	/**
	 * Applies an alpha (0-255) to an existing 0xAARRGGBB colour, preserving RGB.
	 */
	public static int withAlpha(int color, int alpha) {
		return (alpha << 24) | (color & 0x00FFFFFF);
	}
}
