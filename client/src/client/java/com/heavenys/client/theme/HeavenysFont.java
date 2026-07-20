package com.heavenys.client.theme;

import com.heavenys.HeavenysClient;
import com.heavenys.client.config.HeavenysConfig;

import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Helper for rendering Heavenys UI text in the client's own clean font.
 *
 * <p>The font is a bundled TTF (Poppins) registered at {@code assets/heavenys/font/heavenys.json}.
 * It is applied per-{@link Text} via a font {@link net.minecraft.text.Style}, so it <b>only</b>
 * affects Heavenys' own UI — the vanilla game font is left completely untouched. Users can turn
 * it off in the menu (falls back to the vanilla font).
 */
public final class HeavenysFont {
	public static final Identifier FONT_ID = Identifier.of(HeavenysClient.MOD_ID, "heavenys");

	private HeavenysFont() {
	}

	/** Builds a text literal in the Heavenys font (if enabled). */
	public static MutableText text(String value) {
		return apply(Text.literal(value));
	}

	/** Applies the Heavenys font to an existing mutable text (if enabled). */
	public static MutableText apply(MutableText text) {
		if (HeavenysConfig.get().isCustomFont()) {
			return text.styled(style -> style.withFont(new StyleSpriteSource.Font(FONT_ID)));
		}
		return text;
	}
}
