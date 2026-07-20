package com.heavenys.client.hud.modules;

import com.heavenys.client.hud.HudModule;
import com.heavenys.client.theme.HeavenysFont;
import com.heavenys.client.theme.HeavenysTheme;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lists the local player's active potion effects with their remaining duration.
 *
 * <p>Only reads the player's own {@code StatusEffectInstance}s (the same data shown in the
 * inventory screen). No opponent data is ever accessed.
 */
public class PotionStatusModule extends HudModule {
	public PotionStatusModule() {
		super("potionstatus", "heavenys.module.potionstatus", true, 4, 66);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter,
			MinecraftClient client, TextRenderer textRenderer) {
		if (client.player == null) {
			return;
		}

		List<String> lines = new ArrayList<>();
		Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
		for (StatusEffectInstance effect : effects) {
			String name = Text.translatable(effect.getEffectType().value().getTranslationKey()).getString();
			int level = effect.getAmplifier() + 1;
			if (level > 1) {
				name = name + " " + level;
			}
			String duration = StatusEffectUtil.getDurationText(effect, 1.0f, 20.0f).getString();
			lines.add(name + "  " + duration);
		}

		if (lines.isEmpty()) {
			lines.add("No active effects");
		}

		int maxWidth = 0;
		List<Text> rendered = new ArrayList<>();
		for (String line : lines) {
			Text text = HeavenysFont.text(line);
			rendered.add(text);
			maxWidth = Math.max(maxWidth, textRenderer.getWidth(text));
		}

		int width = maxWidth + PADDING * 2 + 2;
		int height = rendered.size() * LINE_HEIGHT + PADDING * 2;
		drawPanel(context, getX(), getY(), width, height);

		int lineY = getY() + PADDING;
		for (Text text : rendered) {
			context.drawTextWithShadow(textRenderer, text,
					getX() + PADDING + 2, lineY, HeavenysTheme.TEXT_PRIMARY);
			lineY += LINE_HEIGHT;
		}
	}
}
