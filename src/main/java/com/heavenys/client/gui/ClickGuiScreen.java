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
import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.impl.client.InterfaceModule;
import com.heavenys.client.module.setting.BooleanSetting;
import com.heavenys.client.module.setting.ColorSetting;
import com.heavenys.client.module.setting.EnumSetting;
import com.heavenys.client.module.setting.KeybindSetting;
import com.heavenys.client.module.setting.NumberSetting;
import com.heavenys.client.module.setting.Setting;
import com.heavenys.client.render.HeavenysColors;
import com.heavenys.client.render.UIRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Heavenys Client ClickGUI. Renders a rounded, blurred, glowing panel with
 * category tabs on the left and a scrollable list of modules and their settings
 * on the right. All widgets are drawn with {@link UIRenderer}; interaction is
 * driven by a per-frame list of {@link Region hit regions} so the layout logic
 * lives in exactly one place.
 */
public class ClickGuiScreen extends Screen {

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 250;
    private static final int TAB_W = 96;
    private static final int ROW_H = 16;
    private static final int SETTING_H = 14;

    /** Preset accent palette used when cycling color settings. */
    private static final int[] PALETTE = {
            0xFFFFD54A, 0xFF7FD4FF, 0xFFB388FF, 0xFF80FFB0, 0xFFFF8FA3, 0xFFFFFFFF
    };

    private final List<Region> regions = new ArrayList<>();

    private Category selectedCategory = Category.HUD;
    private Module expanded;
    private int scroll;

    private NumberSetting draggingSlider;
    private int sliderX;
    private int sliderW;
    private KeybindSetting listeningKeybind;

    public ClickGuiScreen() {
        super(Component.literal("Heavenys Client"));
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
        // The base Screen background pass already applies the vanilla in-world
        // blur (governed by the menu-blur option) exactly once per frame, so we
        // must not call blurBeforeThisStratum() again here. We simply add our own
        // dim tint on top for extra contrast behind the panel.
        UIRenderer.rect(g, 0, 0, g.guiWidth(), g.guiHeight(), 0x66000000);

        int px = (g.guiWidth() - PANEL_W) / 2;
        int py = (g.guiHeight() - PANEL_H) / 2;
        int radius = theme.cornerRadius();

        regions.clear();

        int bg = HeavenysColors.scaleAlpha(theme.background(), theme.backgroundOpacity());
        if (theme.glow()) {
            UIRenderer.glow(g, px, py, PANEL_W, PANEL_H, radius, HeavenysColors.withAlpha(theme.accent(), 70), 4);
        }
        UIRenderer.roundedRect(g, px, py, PANEL_W, PANEL_H, radius, bg);
        UIRenderer.roundedOutline(g, px, py, PANEL_W, PANEL_H, radius, HeavenysColors.withAlpha(theme.outline(), 60));

        drawHeader(g, px, py);
        drawTabs(g, px, py + 24);
        drawModuleList(g, px + TAB_W, py + 24, PANEL_W - TAB_W, PANEL_H - 24, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphicsExtractor g, int px, int py) {
        InterfaceModule theme = theme();
        // Accent "H" logo mark in a rounded chip.
        UIRenderer.roundedRect(g, px + 8, py + 6, 14, 14, 3, HeavenysColors.withAlpha(theme.accent(), 255));
        UIRenderer.centeredText(g, font, "H", px + 8 + 7, py + 6 + 3, 0xFF111111);
        UIRenderer.text(g, font, "Heavenys Client", px + 28, py + 9,
                HeavenysColors.withAlpha(theme.outline(), 255), true);

        // Profile chip on the right (click cycles profiles + saves).
        String profile = HeavenysClient.getInstance().getConfigManager().getActiveProfile();
        String label = "Profile: " + profile;
        int chipW = font.width(label) + 12;
        int chipX = px + PANEL_W - chipW - 8;
        UIRenderer.roundedRect(g, chipX, py + 6, chipW, 14, 3, HeavenysColors.withAlpha(theme.outline(), 25));
        UIRenderer.text(g, font, label, chipX + 6, py + 9, HeavenysColors.withAlpha(theme.accent(), 255), true);
        addRegion(chipX, py + 6, chipW, 14, b -> cycleProfile());
    }

    private void drawTabs(GuiGraphicsExtractor g, int px, int py) {
        InterfaceModule theme = theme();
        int y = py + 4;
        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            int fill = selected ? HeavenysColors.withAlpha(theme.accent(), 210)
                    : HeavenysColors.withAlpha(theme.outline(), 18);
            UIRenderer.roundedRect(g, px + 6, y, TAB_W - 12, ROW_H, 3, fill);
            int textColor = selected ? 0xFF111111 : HeavenysColors.withAlpha(theme.outline(), 220);
            UIRenderer.text(g, font, category.getDisplayName(), px + 12, y + (ROW_H - font.lineHeight) / 2 + 1, textColor, false);
            addRegion(px + 6, y, TAB_W - 12, ROW_H, b -> {
                selectedCategory = category;
                expanded = null;
                scroll = 0;
            });
            y += ROW_H + 3;
        }
    }

    private void drawModuleList(GuiGraphicsExtractor g, int x, int y, int width, int height, int mouseX, int mouseY) {
        InterfaceModule theme = theme();
        g.enableScissor(x, y, x + width, y + height);
        int rowY = y + 4 - scroll;
        List<Module> modules = HeavenysClient.getInstance().getModuleManager().getByCategory(selectedCategory);
        for (Module module : modules) {
            drawModuleRow(g, module, x + 4, rowY, width - 8);
            rowY += ROW_H + 2;
            if (module == expanded) {
                for (Setting<?> setting : module.getSettings()) {
                    if (!setting.isVisible()) {
                        continue;
                    }
                    drawSettingRow(g, setting, x + 12, rowY, width - 20);
                    rowY += SETTING_H + 2;
                }
            }
        }
        g.disableScissor();
    }

    private void drawModuleRow(GuiGraphicsExtractor g, Module module, int x, int y, int width) {
        InterfaceModule theme = theme();
        UIRenderer.roundedRect(g, x, y, width, ROW_H, 3, HeavenysColors.withAlpha(theme.outline(), 16));
        int nameColor = module.isEnabled()
                ? HeavenysColors.withAlpha(theme.accent(), 255)
                : HeavenysColors.withAlpha(theme.outline(), 210);
        UIRenderer.text(g, font, module.getName(), x + 8, y + (ROW_H - font.lineHeight) / 2 + 1, nameColor, false);

        // Toggle pill on the right.
        int pillW = 20;
        int pillX = x + width - pillW - 6;
        int pillY = y + 4;
        int track = module.isEnabled()
                ? HeavenysColors.withAlpha(theme.accent(), 220)
                : HeavenysColors.withAlpha(theme.outline(), 40);
        UIRenderer.roundedRect(g, pillX, pillY, pillW, ROW_H - 8, (ROW_H - 8) / 2, track);
        int knobX = module.isEnabled() ? pillX + pillW - (ROW_H - 8) : pillX;
        UIRenderer.roundedRect(g, knobX, pillY, ROW_H - 8, ROW_H - 8, (ROW_H - 8) / 2, 0xFFFFFFFF);

        addRegion(pillX, pillY, pillW, ROW_H - 8, b -> {
            module.toggle();
            HeavenysClient.getInstance().getConfigManager().saveIfAuto();
        });
        // Clicking the row body expands/collapses settings.
        addRegion(x, y, width - pillW - 10, ROW_H, b -> expanded = (expanded == module ? null : module));
    }

    private void drawSettingRow(GuiGraphicsExtractor g, Setting<?> setting, int x, int y, int width) {
        InterfaceModule theme = theme();
        int label = HeavenysColors.withAlpha(theme.outline(), 200);
        UIRenderer.text(g, font, setting.getName(), x, y + (SETTING_H - font.lineHeight) / 2 + 1, label, false);

        if (setting instanceof BooleanSetting bool) {
            String state = bool.getValue() ? "ON" : "OFF";
            int color = bool.getValue() ? HeavenysColors.withAlpha(theme.accent(), 255)
                    : HeavenysColors.withAlpha(theme.outline(), 120);
            UIRenderer.text(g, font, state, x + width - font.width(state), y + 3, color, false);
            addRegion(x, y, width, SETTING_H, b -> {
                bool.toggle();
                HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            });
        } else if (setting instanceof NumberSetting number) {
            int barW = 90;
            int barX = x + width - barW;
            int barY = y + SETTING_H / 2 - 1;
            UIRenderer.roundedRect(g, barX, barY, barW, 3, 1, HeavenysColors.withAlpha(theme.outline(), 40));
            int fillW = (int) Math.round(barW * number.getFraction());
            UIRenderer.roundedRect(g, barX, barY, fillW, 3, 1, HeavenysColors.withAlpha(theme.accent(), 230));
            String value = trim(number.getValue());
            UIRenderer.text(g, font, value, barX - font.width(value) - 4, y + 3,
                    HeavenysColors.withAlpha(theme.outline(), 220), false);
            addRegion(barX, y, barW, SETTING_H, b -> {
                draggingSlider = number;
                sliderX = barX;
                sliderW = barW;
            });
        } else if (setting instanceof EnumSetting<?> enumSetting) {
            String value = enumName(enumSetting.getValue());
            UIRenderer.text(g, font, value, x + width - font.width(value), y + 3,
                    HeavenysColors.withAlpha(theme.accent(), 255), false);
            addRegion(x, y, width, SETTING_H, b -> {
                enumSetting.cycle();
                HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            });
        } else if (setting instanceof ColorSetting color) {
            UIRenderer.roundedRect(g, x + width - 24, y + 2, 24, SETTING_H - 4, 2,
                    HeavenysColors.withAlpha(color.getValue(), 255));
            addRegion(x, y, width, SETTING_H, b -> {
                cyclePalette(color);
                HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            });
        } else if (setting instanceof KeybindSetting keybind) {
            boolean listening = keybind == listeningKeybind;
            String value = listening ? "..." : keyName(keybind.getValue());
            UIRenderer.text(g, font, value, x + width - font.width(value), y + 3,
                    HeavenysColors.withAlpha(theme.accent(), 255), false);
            addRegion(x, y, width, SETTING_H, b -> listeningKeybind = (listening ? null : keybind));
        }
    }

    // ------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int mx = (int) event.x();
            int my = (int) event.y();
            // Iterate in reverse so widgets drawn last (on top) win.
            for (int i = regions.size() - 1; i >= 0; i--) {
                Region region = regions.get(i);
                if (region.contains(mx, my)) {
                    region.action.accept(event.button());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingSlider != null && sliderW > 0) {
            double fraction = (event.x() - sliderX) / (double) sliderW;
            draggingSlider.setFromFraction(fraction);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider != null) {
            draggingSlider = null;
            HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) (scrollY * 12));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningKeybind != null) {
            listeningKeybind.setValue(event.key() == GLFW.GLFW_KEY_ESCAPE ? KeybindSetting.UNBOUND : event.key());
            listeningKeybind = null;
            HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            return true;
        }
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

    // ----------------------------------------------------------- helpers

    private void cycleProfile() {
        var config = HeavenysClient.getInstance().getConfigManager();
        List<String> profiles = config.listProfiles();
        int index = profiles.indexOf(config.getActiveProfile());
        String next = profiles.get((index + 1) % profiles.size());
        config.loadProfile(next);
    }

    private void cyclePalette(ColorSetting color) {
        int current = color.getValue();
        int alpha = color.getAlpha();
        for (int i = 0; i < PALETTE.length; i++) {
            if ((PALETTE[i] & 0x00FFFFFF) == (current & 0x00FFFFFF)) {
                int next = PALETTE[(i + 1) % PALETTE.length];
                color.setValue(HeavenysColors.withAlpha(next, alpha));
                return;
            }
        }
        color.setValue(HeavenysColors.withAlpha(PALETTE[0], alpha));
    }

    private void addRegion(int x, int y, int w, int h, Consumer<Integer> action) {
        regions.add(new Region(x, y, w, h, action));
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }

    private static String enumName(Object value) {
        return value == null ? "-" : value.toString().replace('_', ' ');
    }

    private static String keyName(int key) {
        if (key == KeybindSetting.UNBOUND) {
            return "None";
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : "KEY " + key;
    }

    /** A rectangular hit region with an associated click action. */
    private record Region(int x, int y, int w, int h, Consumer<Integer> action) {
        boolean contains(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }
}
