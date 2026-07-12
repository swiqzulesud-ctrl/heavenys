package net.heavenys.client.hud.modules;

import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.hud.HudModule;
import net.heavenys.client.util.HeavenysColors;
import net.heavenys.client.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PotionStatus module — renders active status effects with icon, name,
 * remaining duration, and amplifier level.
 *
 * Effects are sorted: beneficial first, then by remaining duration descending.
 */
public class PotionStatusModule extends HudModule {

    private static final int ICON_SIZE  = 18;
    private static final int ROW_HEIGHT = 20;
    private static final int PADDING    = 3;
    private static final int MODULE_W   = 140;

    private final HeavenysConfig config;

    public PotionStatusModule(HeavenysConfig config) {
        super("potion_status", "Potion Status", config.potionStatusEnabled);
        this.config = config;
    }

    @Override
    public void render(DrawContext ctx, int scaledWidth, int scaledHeight, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        PlayerEntity player = mc.player;
        List<StatusEffectInstance> effects = sortedEffects(player);

        if (effects.isEmpty()) return;

        int moduleH = effects.size() * ROW_HEIGHT + PADDING * 2;
        int x = config.potionStatusX;
        int y = config.potionStatusY;

        RenderUtil.drawPanel(ctx, x, y, MODULE_W, moduleH);

        int rowY = y + PADDING;
        for (StatusEffectInstance effect : effects) {
            RegistryEntry<StatusEffect> typeEntry = effect.getEffectType();
            StatusEffect type = typeEntry.value();

            // Colored icon square (vanilla sprite would need a sprite atlas; use colored rect as fallback)
            int iconColor = type.isBeneficial()
                ? HeavenysColors.withAlpha(HeavenysColors.BUTTER_YELLOW, 200)
                : HeavenysColors.withAlpha(HeavenysColors.DISABLED_RED, 200);
            RenderUtil.drawFilledRect(ctx, x + PADDING, rowY, ICON_SIZE, ICON_SIZE, iconColor);

            // Effect name
            String name = type.getName().getString();
            if (effect.getAmplifier() > 0) {
                name += " " + toRoman(effect.getAmplifier() + 1);
            }
            if (name.length() > 14) name = name.substring(0, 14) + "…";

            ctx.drawText(mc.textRenderer, name,
                x + PADDING + ICON_SIZE + 3, rowY + 2,
                HeavenysColors.WHITE, true);

            // Duration
            String duration = formatDuration(effect.getDuration());
            ctx.drawText(mc.textRenderer, duration,
                x + PADDING + ICON_SIZE + 3, rowY + 11,
                HeavenysColors.OFF_WHITE, false);

            rowY += ROW_HEIGHT;
        }
    }

    private static List<StatusEffectInstance> sortedEffects(PlayerEntity player) {
        List<StatusEffectInstance> list = new ArrayList<>(player.getStatusEffects());
        list.sort(Comparator
            .<StatusEffectInstance, Boolean>comparing(e -> !e.getEffectType().value().isBeneficial())
            .thenComparingInt(StatusEffectInstance::getDuration).reversed());
        return list;
    }

    /** Formats ticks to mm:ss for display. Permanent effects show "∞". */
    private static String formatDuration(int ticks) {
        if (ticks == Integer.MAX_VALUE || ticks < 0) return "∞";
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    @Override public int getPreferredWidth()  { return MODULE_W; }
}
