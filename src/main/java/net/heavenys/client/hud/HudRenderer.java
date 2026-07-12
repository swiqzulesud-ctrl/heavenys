package net.heavenys.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Called from {@link net.heavenys.client.mixin.InGameHudMixin} each frame.
 * Iterates all enabled modules and delegates rendering.
 */
public class HudRenderer {

    private final HudModuleRegistry registry;

    public HudRenderer(HudModuleRegistry registry) {
        this.registry = registry;
    }

    /**
     * Renders all enabled HUD modules.
     * This method is safe to call even when the player or world is null
     * (modules individually guard against that).
     */
    public void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;

        // Hide HUD modules while the debug screen or chat is open
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        for (HudModule module : registry.all()) {
            if (module.isEnabled()) {
                module.render(ctx, w, h, tickDelta);
            }
        }
    }
}
