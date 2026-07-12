package net.heavenys.client.mixin;

import net.heavenys.client.HeavenysClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the end of {@link InGameHud#render} to draw all Heavenys HUD
 * modules on top of the vanilla HUD without replacing any vanilla elements.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void heavenys$renderHud(DrawContext ctx, RenderTickCounter tickCounter, CallbackInfo ci) {
        HeavenysClient.getInstance()
            .getHudRenderer()
            .render(ctx, tickCounter.getTickDelta(true));
    }
}
