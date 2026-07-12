package net.heavenys.client.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Reserved for future GameRenderer hooks (e.g. custom post-processing or
 * camera tilt adjustments). Currently empty — the mixin is registered so it
 * can be extended without requiring a new entry in the mixin config JSON.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // Future hooks go here
}
