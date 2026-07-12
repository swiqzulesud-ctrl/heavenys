package net.heavenys.client.hud.modules;

import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.hud.HudModule;
import net.heavenys.client.util.HeavenysColors;
import net.heavenys.client.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * ArmorStatus module — displays the four armor slots in a compact vertical
 * column with optional durability bars.
 *
 * Layout (top-down per slot):
 * ┌─────────────────────────────┐
 * │  [icon]  Name  [dur bar]    │
 * └─────────────────────────────┘
 */
public class ArmorStatusModule extends HudModule {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private static final int ICON_SIZE    = 16;
    private static final int ROW_HEIGHT   = 18;
    private static final int PADDING      = 3;
    private static final int BAR_H        = 2;
    private static final int MODULE_W     = 110;

    private final HeavenysConfig config;

    public ArmorStatusModule(HeavenysConfig config) {
        super("armor_status", "Armor Status", config.armorStatusEnabled);
        this.config = config;
    }

    @Override
    public void render(DrawContext ctx, int scaledWidth, int scaledHeight, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        PlayerEntity player = mc.player;
        int x = config.armorStatusX;
        int y = config.armorStatusY;
        int moduleH = SLOTS.length * ROW_HEIGHT + PADDING * 2;

        RenderUtil.drawPanel(ctx, x, y, MODULE_W, moduleH);

        int rowY = y + PADDING;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) {
                rowY += ROW_HEIGHT;
                continue;
            }

            // Item icon
            ctx.drawItem(stack, x + PADDING, rowY);

            // Item name (truncated to fit panel)
            String name = stack.getName().getString();
            if (name.length() > 12) name = name.substring(0, 12) + "…";
            ctx.drawText(mc.textRenderer, name,
                x + PADDING + ICON_SIZE + 3, rowY + 4,
                HeavenysColors.BUTTER_YELLOW, true);

            // Durability bar
            if (config.armorDurabilityBar && stack.isDamageable()) {
                int maxDur  = stack.getMaxDamage();
                int curDur  = maxDur - stack.getDamage();
                float ratio = (float) curDur / maxDur;
                int barColor = ratio > 0.5f ? HeavenysColors.ENABLED_GREEN
                             : ratio > 0.2f ? HeavenysColors.WARN_AMBER
                             :                HeavenysColors.DISABLED_RED;

                int barX = x + PADDING + ICON_SIZE + 3;
                int barW = MODULE_W - PADDING * 2 - ICON_SIZE - 3 - PADDING;
                RenderUtil.drawProgressBar(ctx, barX, rowY + ICON_SIZE - BAR_H - 1,
                    barW, BAR_H, ratio, barColor, HeavenysColors.PANEL_BG_ALT);
            }

            rowY += ROW_HEIGHT;
        }
    }

    @Override public int getPreferredWidth()  { return MODULE_W; }
    @Override public int getPreferredHeight() { return SLOTS.length * ROW_HEIGHT + PADDING * 2; }
}
