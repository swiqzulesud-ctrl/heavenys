package com.heavenys.client.hud.modules;

import com.heavenys.client.hud.HudModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Shows the local player's currently worn armour (helmet → boots) with vanilla
 * durability / count overlays. This is information the player already sees in their own
 * inventory, so it is fully rule-compliant.
 */
public class ArmorStatusModule extends HudModule {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};
	private static final int SLOT_SIZE = 18;

	public ArmorStatusModule() {
		super("armorstatus", "heavenys.module.armorstatus", true, 4, 40);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter,
			MinecraftClient client, TextRenderer textRenderer) {
		if (client.player == null) {
			return;
		}

		int width = SLOT_SIZE * ARMOR_SLOTS.length + PADDING * 2;
		int height = SLOT_SIZE + PADDING * 2;
		drawPanel(context, getX(), getY(), width, height);

		int slotX = getX() + PADDING;
		int slotY = getY() + PADDING;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = client.player.getEquippedStack(slot);
			if (!stack.isEmpty()) {
				context.drawItem(stack, slotX, slotY);
				context.drawStackOverlay(textRenderer, stack, slotX, slotY);
			}
			slotX += SLOT_SIZE;
		}
	}
}
