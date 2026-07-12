package net.heavenys.client.hud.modules;

import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.hud.HudModule;
import net.heavenys.client.util.HeavenysColors;
import net.heavenys.client.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * FPS counter module.
 *
 * Reads the engine's own FPS counter, identical to what the F3 screen shows.
 * Colored threshold: ≥60 green, ≥30 yellow, <30 red.
 */
public class FpsModule extends HudModule {

    private static final int PADDING  = 4;
    private static final int MODULE_W = 56;
    private static final int MODULE_H = 14;

    private final HeavenysConfig config;

    public FpsModule(HeavenysConfig config) {
        super("fps", "FPS Counter", config.fpsEnabled);
        this.config = config;
    }

    @Override
    public void render(DrawContext ctx, int scaledWidth, int scaledHeight, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int fps  = mc.getCurrentFps();
        String label = fps + " FPS";

        int x = config.fpsTopRight
            ? scaledWidth - MODULE_W - PADDING
            : config.fpsX;
        int y = config.fpsTopRight ? PADDING : config.fpsY;

        RenderUtil.drawPanel(ctx, x, y, MODULE_W, MODULE_H);

        int fpsColor = fps >= 60 ? HeavenysColors.ENABLED_GREEN
                     : fps >= 30 ? HeavenysColors.BUTTER_YELLOW
                     :             HeavenysColors.DISABLED_RED;

        int labelW = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label,
            x + (MODULE_W - labelW) / 2,
            y + (MODULE_H - mc.textRenderer.fontHeight) / 2,
            fpsColor, true);
    }

    @Override public int getPreferredWidth()  { return MODULE_W; }
    @Override public int getPreferredHeight() { return MODULE_H; }
}
