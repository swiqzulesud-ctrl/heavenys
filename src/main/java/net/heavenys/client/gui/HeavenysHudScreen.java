package net.heavenys.client.gui;

import net.heavenys.client.HeavenysClient;
import net.heavenys.client.hud.HudModule;
import net.heavenys.client.util.HeavenysColors;
import net.heavenys.client.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game HUD configuration screen.
 *
 * Open with the keybind (default: Right Shift).
 * Features:
 * <ul>
 *   <li>List of all registered modules with toggle buttons.</li>
 *   <li>White / Butter Yellow aesthetic panel layout.</li>
 *   <li>Closes on Escape and saves config automatically.</li>
 * </ul>
 */
public class HeavenysHudScreen extends Screen {

    private static final int PANEL_W   = 220;
    private static final int PANEL_PAD = 10;
    private static final int ROW_H     = 24;
    private static final int BTN_W     = 64;
    private static final int BTN_H     = 14;
    private static final int TITLE_H   = 28;

    private final List<HudModule> modules;
    private final List<ToggleButton> buttons = new ArrayList<>();

    public HeavenysHudScreen() {
        super(Text.translatable("heavenys-client.menu.title"));
        this.modules = new ArrayList<>(HeavenysClient.getInstance().getRegistry().all());
    }

    @Override
    protected void init() {
        buttons.clear();

        int panelH = TITLE_H + modules.size() * ROW_H + PANEL_PAD * 2;
        int panelX = (width - PANEL_W) / 2;
        int panelY = (height - panelH) / 2;

        for (int i = 0; i < modules.size(); i++) {
            HudModule m   = modules.get(i);
            int btnX = panelX + PANEL_W - BTN_W - PANEL_PAD;
            int btnY = panelY + TITLE_H + PANEL_PAD + i * ROW_H + (ROW_H - BTN_H) / 2;
            ToggleButton btn = new ToggleButton(m, btnX, btnY, BTN_W, BTN_H);
            buttons.add(btn);
            addDrawableChild(btn);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dim background
        renderBackground(ctx, mouseX, mouseY, delta);

        int panelH = TITLE_H + modules.size() * ROW_H + PANEL_PAD * 2;
        int panelX = (width - PANEL_W) / 2;
        int panelY = (height - panelH) / 2;

        // Main panel
        RenderUtil.drawPanel(ctx, panelX, panelY, PANEL_W, panelH);

        // Title bar background
        ctx.fill(panelX + 1, panelY + 1, panelX + PANEL_W - 1, panelY + TITLE_H,
            HeavenysColors.withAlpha(HeavenysColors.BUTTER_YELLOW, 40));

        // Title text
        String titleStr = "✦ Heavenys Client";
        int titleW = textRenderer.getWidth(titleStr);
        ctx.drawText(textRenderer, titleStr,
            panelX + (PANEL_W - titleW) / 2, panelY + 9,
            HeavenysColors.BUTTER_YELLOW, true);

        // Module rows
        for (int i = 0; i < modules.size(); i++) {
            HudModule m = modules.get(i);
            int rowY = panelY + TITLE_H + PANEL_PAD + i * ROW_H;

            // Alternate row shading
            if (i % 2 == 0) {
                ctx.fill(panelX + 1, rowY, panelX + PANEL_W - 1, rowY + ROW_H,
                    HeavenysColors.withAlpha(HeavenysColors.WHITE, 8));
            }

            // Module name
            ctx.drawText(textRenderer, m.getDisplayName(),
                panelX + PANEL_PAD, rowY + (ROW_H - textRenderer.fontHeight) / 2,
                m.isEnabled() ? HeavenysColors.WHITE : HeavenysColors.OFF_WHITE,
                false);

            // Status dot
            int dotColor = m.isEnabled() ? HeavenysColors.ENABLED_GREEN : HeavenysColors.DISABLED_RED;
            ctx.fill(panelX + PANEL_W - BTN_W - PANEL_PAD - 10,
                rowY + ROW_H / 2 - 2,
                panelX + PANEL_W - BTN_W - PANEL_PAD - 6,
                rowY + ROW_H / 2 + 2,
                dotColor);
        }

        super.render(ctx, mouseX, mouseY, delta);

        // Close hint
        String hint = "[ ESC to close & save ]";
        ctx.drawText(textRenderer, hint,
            panelX + (PANEL_W - textRenderer.getWidth(hint)) / 2,
            panelY + panelH + 6,
            HeavenysColors.withAlpha(HeavenysColors.BUTTER_YELLOW, 160), false);
    }

    @Override
    public void close() {
        HeavenysClient.getInstance().saveConfig();
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }

    // ---- Inner toggle button ------------------------------------------------

    private static class ToggleButton extends net.minecraft.client.gui.widget.ButtonWidget {

        private final HudModule module;

        ToggleButton(HudModule module, int x, int y, int w, int h) {
            super(x, y, w, h,
                Text.of(module.isEnabled() ? "Enabled" : "Disabled"),
                btn -> {
                    module.toggle();
                    btn.setMessage(Text.of(module.isEnabled() ? "Enabled" : "Disabled"));
                },
                DEFAULT_NARRATION_SUPPLIER);
            this.module = module;
        }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean on      = module.isEnabled();
            int bg     = isHovered()
                ? (on ? HeavenysColors.withAlpha(HeavenysColors.BUTTER_YELLOW, 220)
                      : HeavenysColors.withAlpha(HeavenysColors.DISABLED_RED,  180))
                : (on ? HeavenysColors.withAlpha(HeavenysColors.BUTTER_YELLOW, 160)
                      : HeavenysColors.withAlpha(HeavenysColors.DISABLED_RED,  120));

            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
            ctx.fill(getX(), getY(), getX() + getWidth(), getY() + 1, HeavenysColors.BORDER);
            ctx.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), HeavenysColors.BORDER);
            ctx.fill(getX(), getY(), getX() + 1, getY() + getHeight(), HeavenysColors.BORDER);
            ctx.fill(getX() + getWidth() - 1, getY(), getX() + getWidth(), getY() + getHeight(), HeavenysColors.BORDER);

            String label = on ? "Enabled" : "Disabled";
            int tw = net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(label);
            int textColor = on ? HeavenysColors.PANEL_BG : HeavenysColors.WHITE;
            ctx.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, label,
                getX() + (getWidth() - tw) / 2,
                getY() + (getHeight() - net.minecraft.client.MinecraftClient.getInstance().textRenderer.fontHeight) / 2,
                textColor, false);
        }
    }
}
