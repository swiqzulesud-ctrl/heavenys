package net.heavenys.client.mixin;

import net.heavenys.client.HeavenysClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.7-Style Arm Swing Animation
 * ==============================
 * In modern Minecraft the arm-swing interpolation uses a smooth cosine
 * curve, making the hand feel slow compared to the "snappy" feel of 1.7.
 *
 * This mixin intercepts the {@code swingProgress} value passed to the
 * hand-rendering method and applies a speed multiplier from the Heavenys
 * config — keeping the animation cosmetically faster while NOT changing
 * actual attack timing or server-side behaviour.
 *
 * The default multiplier of 1.0 is a no-op; raising it towards 2.0 gives
 * a more responsive feel. No server packets are sent or modified.
 */
@Mixin(HeldItemRenderer.class)
public class HandRendererMixin {

    @ModifyVariable(
        method = "renderItem(FLnet/minecraft/util/Hand;FLnet/minecraft/entity/player/PlayerEntity;F)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private float heavenys$adjustSwingProgress(float swingProgress) {
        float multiplier = HeavenysClient.getInstance().getConfig().swingSpeedMultiplier;
        if (multiplier == 1.0f) return swingProgress;
        // Clamp to [0, 1] after scaling to avoid animation glitches
        return Math.min(1.0f, swingProgress * Math.max(0.5f, Math.min(2.0f, multiplier)));
    }
}
