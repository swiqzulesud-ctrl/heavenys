package net.heavenys.client.hud.modules;

import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.hud.HudModule;
import net.heavenys.client.util.HeavenysColors;
import net.heavenys.client.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.UUID;

/**
 * Ping display module.
 *
 * Retrieves latency from the vanilla {@link PlayerListEntry} — the same
 * value shown on the TAB list. No packet manipulation.
 *
 * Color thresholds: ≤50ms green, ≤100ms yellow, ≤200ms amber, >200ms red.
 */
public class PingModule extends HudModule {

    private static final int PADDING  = 4;
    private static final int MODULE_W = 64;
    private static final int MODULE_H = 14;

    private final HeavenysConfig config;

    public PingModule(HeavenysConfig config) {
        super("ping", "Ping Display", config.pingEnabled);
        this.config = config;
    }

    @Override
    public void render(DrawContext ctx, int scaledWidth, int scaledHeight, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int ping = getPing(mc);
        String label = ping + " ms";

        int x = config.fpsTopRight
            ? scaledWidth - MODULE_W - PADDING
            : config.pingX;
        int y = config.fpsTopRight ? PADDING + 18 : config.pingY;

        RenderUtil.drawPanel(ctx, x, y, MODULE_W, MODULE_H);

        int pingColor = ping <= 50  ? HeavenysColors.ENABLED_GREEN
                      : ping <= 100 ? HeavenysColors.BUTTER_YELLOW
                      : ping <= 200 ? HeavenysColors.WARN_AMBER
                      :               HeavenysColors.DISABLED_RED;

        int labelW = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label,
            x + (MODULE_W - labelW) / 2,
            y + (MODULE_H - mc.textRenderer.fontHeight) / 2,
            pingColor, true);
    }

    private static int getPing(MinecraftClient mc) {
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null || mc.player == null) return 0;
        UUID id = mc.player.getUuid();
        PlayerListEntry entry = handler.getPlayerListEntry(id);
        return entry != null ? entry.getLatency() : 0;
    }

    @Override public int getPreferredWidth()  { return MODULE_W; }
    @Override public int getPreferredHeight() { return MODULE_H; }
}
