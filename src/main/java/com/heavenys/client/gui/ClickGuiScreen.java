/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 *
 * ----------------------------------------------------------------------------
 * The layout and behaviour of this screen (centered rounded "block" panel, a
 * top search field, scrollable module cards with descriptions, a per-module
 * settings area and a smooth open animation) are adapted from the GUI design of
 * the open-source Sol Client (GPL-3.0) - specifically its ModsScreen. No Sol
 * Client source is reproduced: Sol Client targets Minecraft 1.8.9 with its own
 * component framework, so this is an original re-implementation against the
 * Minecraft 26.2 rendering API. Original design:
 *   Sol Client - Copyright (C) 2021-2023 TheKodeToad and Contributors (GPL-3.0)
 *   https://github.com/Sol-Client/Client
 * ----------------------------------------------------------------------------
 */
package com.heavenys.client.gui;

import com.heavenys.client.HeavenysClient;
import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.ModuleManager;
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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The Heavenys Client ClickGUI. Renders a rounded, glowing "block" panel with a
 * search field, category tabs and a scrollable list of module cards (name +
 * description + toggle) plus an expandable per-module settings area. Drawing
 * uses {@link UIRenderer}; interaction is driven by a per-frame list of
 * {@link Region hit regions} so the layout logic lives in one place.
 *
 * <p>The visual design is adapted from Sol Client's ModsScreen (see the file
 * header for attribution) and re-implemented for Minecraft 26.2.</p>
 */
public class ClickGuiScreen extends Screen {

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 262;
    private static final int TAB_W = 96;
    private static final int HEADER_H = 22;
    private static final int SEARCH_H = 16;
    private static final int CARD_H = 24;
    private static final int SETTING_H = 14;
    private static final long ANIM_MS = 260;

    /** Preset accent palette used when cycling color settings. */
    private static final int[] PALETTE = {
            0xFFFFD54A, 0xFF7FD4FF, 0xFFB388FF, 0xFF80FFB0, 0xFFFF8FA3, 0xFFFFFFFF
    };

    private final List<Region> regions = new ArrayList<>();
    private final long openedAt = System.currentTimeMillis();

    private Category selectedCategory = Category.HUD;
    private Module expanded;
    private int scroll;
    private String search = "";

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

        // Dim tint behind the panel (drawn unscaled). The base Screen background
        // pass already applies the single allowed per-frame blur.
        UIRenderer.rect(g, 0, 0, g.guiWidth(), g.guiHeight(), 0x66000000);

        int px = (g.guiWidth() - PANEL_W) / 2;
        int py = (g.guiHeight() - PANEL_H) / 2;

        // Smooth "pop" open animation: scale from 0.85 -> 1.0 around the panel
        // centre, easing with the configured animation style/speed.
        float progress = animationProgress();
        boolean animating = progress < 1.0f;
        if (animating) {
            float scale = 0.85f + 0.15f * progress;
            float cx = px + PANEL_W / 2f;
            float cy = py + PANEL_H / 2f;
            g.pose().pushMatrix();
            g.pose().translate(cx, cy);
            g.pose().scale(scale, scale);
            g.pose().translate(-cx, -cy);
        }

        regions.clear();
        drawPanel(g, theme, px, py, progress);

        if (animating) {
            g.pose().popMatrix();
        }
    }

    private void drawPanel(GuiGraphicsExtractor g, InterfaceModule theme, int px, int py, float progress) {
        int radius = theme.cornerRadius();
        float fade = Math.max(0.2f, progress);

        int bg = HeavenysColors.scaleAlpha(
                HeavenysColors.scaleAlpha(theme.background(), theme.backgroundOpacity()), fade);
        if (theme.glow()) {
            UIRenderer.glow(g, px, py, PANEL_W, PANEL_H, radius,
                    HeavenysColors.scaleAlpha(HeavenysColors.withAlpha(theme.accent(), 70), fade), 4);
        }
        UIRenderer.roundedRect(g, px, py, PANEL_W, PANEL_H, radius, bg);
        UIRenderer.roundedOutline(g, px, py, PANEL_W, PANEL_H, radius,
                HeavenysColors.scaleAlpha(HeavenysColors.withAlpha(theme.outline(), 60), fade));

        drawHeader(g, theme, px, py);
        drawSearch(g, theme, px + 6, py + HEADER_H, PANEL_W - 12);

        int contentY = py + HEADER_H + SEARCH_H + 4;
        int contentH = PANEL_H - (HEADER_H + SEARCH_H + 4);
        if (search.isBlank()) {
            drawTabs(g, theme, px, contentY);
        }
        int listX = search.isBlank() ? px + TAB_W : px + 6;
        int listW = search.isBlank() ? PANEL_W - TAB_W - 6 : PANEL_W - 12;
        drawModuleList(g, theme, listX, contentY, listW, contentH);
    }

    private void drawHeader(GuiGraphicsExtractor g, InterfaceModule theme, int px, int py) {
        // Accent "H" logo mark in a rounded chip.
        UIRenderer.roundedRect(g, px + 8, py + 5, 14, 14, 3, HeavenysColors.withAlpha(theme.accent(), 255));
        UIRenderer.centeredText(g, font, "H", px + 8 + 7, py + 5 + 3, 0xFF111111);
        UIRenderer.text(g, font, "Heavenys Client", px + 28, py + 8,
                HeavenysColors.withAlpha(theme.outline(), 255), true);

        // Profile chip on the right (click cycles profiles + loads).
        String profile = HeavenysClient.getInstance().getConfigManager().getActiveProfile();
        String label = "Profile: " + profile;
        int chipW = font.width(label) + 12;
        int chipX = px + PANEL_W - chipW - 8;
        UIRenderer.roundedRect(g, chipX, py + 5, chipW, 14, 3, HeavenysColors.withAlpha(theme.outline(), 25));
        UIRenderer.text(g, font, label, chipX + 6, py + 8, HeavenysColors.withAlpha(theme.accent(), 255), true);
        addRegion(chipX, py + 5, chipW, 14, b -> cycleProfile());
    }

    private void drawSearch(GuiGraphicsExtractor g, InterfaceModule theme, int x, int y, int width) {
        UIRenderer.roundedRect(g, x, y, width, SEARCH_H - 2, 3, HeavenysColors.withAlpha(theme.outline(), 18));
        UIRenderer.roundedOutline(g, x, y, width, SEARCH_H - 2, 3, HeavenysColors.withAlpha(theme.outline(), 35));
        boolean empty = search.isBlank();
        String shown = empty ? "Search modules..." : search + "_";
        int color = empty ? HeavenysColors.withAlpha(theme.outline(), 90)
                : HeavenysColors.withAlpha(theme.outline(), 230);
        UIRenderer.text(g, font, shown, x + 6, y + 3, color, false);
    }

    private void drawTabs(GuiGraphicsExtractor g, InterfaceModule theme, int px, int py) {
        int y = py + 2;
        for (Category category : Category.values()) {
            boolean selected = category == selectedCategory;
            int fill = selected ? HeavenysColors.withAlpha(theme.accent(), 210)
                    : HeavenysColors.withAlpha(theme.outline(), 18);
            UIRenderer.roundedRect(g, px + 6, y, TAB_W - 12, 16, 3, fill);
            int textColor = selected ? 0xFF111111 : HeavenysColors.withAlpha(theme.outline(), 220);
            UIRenderer.text(g, font, category.getDisplayName(), px + 12, y + 4, textColor, false);
            addRegion(px + 6, y, TAB_W - 12, 16, b -> {
                selectedCategory = category;
                expanded = null;
                scroll = 0;
            });
            y += 19;
        }
    }

    private void drawModuleList(GuiGraphicsExtractor g, InterfaceModule theme, int x, int y, int width, int height) {
        g.enableScissor(x, y, x + width, y + height);
        int rowY = y + 2 - scroll;
        for (Module module : visibleModules()) {
            drawModuleCard(g, theme, module, x + 2, rowY, width - 4);
            rowY += CARD_H + 2;
            if (module == expanded) {
                for (Setting<?> setting : module.getSettings()) {
                    if (!setting.isVisible()) {
                        continue;
                    }
                    drawSettingRow(g, theme, setting, x + 10, rowY, width - 18);
                    rowY += SETTING_H + 2;
                }
            }
        }
        g.disableScissor();
    }

    private void drawModuleCard(GuiGraphicsExtractor g, InterfaceModule theme, Module module, int x, int y, int width) {
        UIRenderer.roundedRect(g, x, y, width, CARD_H, 3, HeavenysColors.withAlpha(theme.outline(), 16));
        int nameColor = module.isEnabled()
                ? HeavenysColors.withAlpha(theme.accent(), 255)
                : HeavenysColors.withAlpha(theme.outline(), 220);
        UIRenderer.text(g, font, module.getName(), x + 8, y + 4, nameColor, false);
        String desc = truncate(module.getDescription(), width - 46);
        UIRenderer.text(g, font, desc, x + 8, y + 14, HeavenysColors.withAlpha(theme.outline(), 110), false);

        // Toggle pill on the right.
        int pillW = 20;
        int pillH = 10;
        int pillX = x + width - pillW - 6;
        int pillY = y + (CARD_H - pillH) / 2;
        int track = module.isEnabled()
                ? HeavenysColors.withAlpha(theme.accent(), 220)
                : HeavenysColors.withAlpha(theme.outline(), 40);
        UIRenderer.roundedRect(g, pillX, pillY, pillW, pillH, pillH / 2, track);
        int knobX = module.isEnabled() ? pillX + pillW - pillH : pillX;
        UIRenderer.roundedRect(g, knobX, pillY, pillH, pillH, pillH / 2, 0xFFFFFFFF);

        addRegion(pillX, pillY, pillW, pillH, b -> {
            module.toggle();
            HeavenysClient.getInstance().getConfigManager().saveIfAuto();
        });
        addRegion(x, y, width - pillW - 10, CARD_H, b -> expanded = (expanded == module ? null : module));
    }

    private void drawSettingRow(GuiGraphicsExtractor g, InterfaceModule theme, Setting<?> setting, int x, int y, int width) {
        int label = HeavenysColors.withAlpha(theme.outline(), 200);
        UIRenderer.text(g, font, setting.getName(), x, y + 3, label, false);

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
            draggingSlider.setFromFraction((event.x() - sliderX) / (double) sliderW);
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
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (cp >= ' ' && cp != 127) {
            search += event.codepointAsString();
            expanded = null;
            scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningKeybind != null) {
            listeningKeybind.setValue(event.key() == GLFW.GLFW_KEY_ESCAPE ? KeybindSetting.UNBOUND : event.key());
            listeningKeybind = null;
            HeavenysClient.getInstance().getConfigManager().saveIfAuto();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scroll = 0;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (!search.isEmpty()) {
                search = "";
                return true;
            }
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

    private float animationProgress() {
        double speed = Math.max(0.1, theme().animationSpeed());
        double elapsed = (System.currentTimeMillis() - openedAt) * speed;
        double t = Math.min(1.0, elapsed / ANIM_MS);
        return (float) theme().animationStyle().apply(t);
    }

    private List<Module> visibleModules() {
        ModuleManager manager = HeavenysClient.getInstance().getModuleManager();
        if (search.isBlank()) {
            return manager.getByCategory(selectedCategory);
        }
        String q = search.toLowerCase();
        List<Module> matches = new ArrayList<>();
        for (Module module : manager.getModules()) {
            if (module.getName().toLowerCase().contains(q) || module.getDescription().toLowerCase().contains(q)) {
                matches.add(module);
            }
        }
        return matches;
    }

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
                color.setValue(HeavenysColors.withAlpha(PALETTE[(i + 1) % PALETTE.length], alpha));
                return;
            }
        }
        color.setValue(HeavenysColors.withAlpha(PALETTE[0], alpha));
    }

    private void addRegion(int x, int y, int w, int h, Consumer<Integer> action) {
        regions.add(new Region(x, y, w, h, action));
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(sb.toString() + c + "...") > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb + "...";
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
