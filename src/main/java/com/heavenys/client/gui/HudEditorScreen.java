/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.gui;

import com.heavenys.client.HeavenysClient;
import com.heavenys.client.hud.HudModule;
import com.heavenys.client.module.impl.client.InterfaceModule;
import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-and-drop HUD editor. Every enabled HUD element is rendered live in place
 * and can be repositioned by dragging. The selected element gets an accent
 * outline; positions are snapped to the screen bounds and auto-saved on release.
 */
public class HudEditorScreen extends Screen {

    private HudModule dragging;
    private int grabOffsetX;
    private int grabOffsetY;

    public HudEditorScreen() {
        super(Component.literal("Heavenys HUD Editor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private InterfaceModule theme() {
        return HeavenysClient.getInstance().getInterface();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        InterfaceModule theme = theme();
        if (theme.blur()) {
            g.blurBeforeThisStratum();
        }
        UIRenderer.rect(g, 0, 0, g.guiWidth(), g.guiHeight(), 0x88000000);

        // Header banner.
        UIRenderer.roundedRect(g, g.guiWidth() / 2 - 130, 8, 260, 20, theme.cornerRadius(),
                HeavenysColors.scaleAlpha(theme.background(), theme.backgroundOpacity()));
        UIRenderer.centeredText(g, font, "HUD Editor  -  drag elements  -  ESC to save & exit",
                g.guiWidth() / 2, 14, HeavenysColors.withAlpha(theme.accent(), 255));

        for (HudModule hud : HeavenysClient.getInstance().getHudModules()) {
            if (!hud.isEnabled()) {
                continue;
            }
            hud.render(g, font);
            int w = hud.getWidth(font);
            int h = hud.getHeight(font);
            boolean selected = hud == dragging;
            int outline = selected
                    ? HeavenysColors.withAlpha(theme.accent(), 255)
                    : HeavenysColors.withAlpha(theme.outline(), 90);
            UIRenderer.roundedOutline(g, hud.getX() - 1, hud.getY() - 1, w + 2, h + 2, 2, outline);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x();
            int my = (int) event.y();
            // Topmost first: iterate in reverse registration order.
            var huds = HeavenysClient.getInstance().getHudModules();
            for (int i = huds.size() - 1; i >= 0; i--) {
                HudModule hud = huds.get(i);
                if (!hud.isEnabled()) {
                    continue;
                }
                int w = hud.getWidth(font);
                int h = hud.getHeight(font);
                if (mx >= hud.getX() && mx <= hud.getX() + w && my >= hud.getY() && my <= hud.getY() + h) {
                    dragging = hud;
                    grabOffsetX = mx - hud.getX();
                    grabOffsetY = my - hud.getY();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            int newX = (int) event.x() - grabOffsetX;
            int newY = (int) event.y() - grabOffsetY;
            int maxX = width - dragging.getWidth(font);
            int maxY = height - dragging.getHeight(font);
            newX = Math.max(0, Math.min(newX, Math.max(0, maxX)));
            newY = Math.max(0, Math.min(newY, Math.max(0, maxY)));
            dragging.setPosition(newX, newY);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        HeavenysClient.getInstance().getConfigManager().saveIfAuto();
        super.onClose();
    }
}
