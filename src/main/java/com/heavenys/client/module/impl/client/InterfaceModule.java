/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.impl.client;

import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.setting.BooleanSetting;
import com.heavenys.client.module.setting.ColorSetting;
import com.heavenys.client.module.setting.EnumSetting;
import com.heavenys.client.module.setting.NumberSetting;
import com.heavenys.client.theme.AnimationStyle;
import com.heavenys.client.theme.FontChoice;

/**
 * Central "look and feel" module. It owns every setting that drives the Heaven
 * theme: the {@code #FFD54A} accent, {@code #111111} background, white outline,
 * UI background opacity, blur, glow and animation preferences.
 *
 * <p>This module is always enabled; toggling it simply has no gameplay effect.
 * It exists so that the theme settings appear as a normal tab in the ClickGUI.</p>
 */
public class InterfaceModule extends Module {

    /** Heaven theme defaults. */
    public static final int DEFAULT_BACKGROUND = 0xFF111111;
    public static final int DEFAULT_OUTLINE = 0xFFFFFFFF;
    public static final int DEFAULT_ACCENT = 0xFFFFD54A;

    private final ColorSetting accent =
            register(new ColorSetting("Accent", "Primary accent color used across the UI.", DEFAULT_ACCENT));
    private final ColorSetting background =
            register(new ColorSetting("Background", "Base panel background color.", DEFAULT_BACKGROUND));
    private final ColorSetting outline =
            register(new ColorSetting("Outline", "Panel outline color.", DEFAULT_OUTLINE));
    private final NumberSetting backgroundOpacity =
            register(new NumberSetting("Background Opacity", "Opacity of UI backgrounds.", 85, 0, 100, 1));
    private final BooleanSetting blur =
            register(new BooleanSetting("Blur", "Blur the world behind open UI panels.", true));
    private final BooleanSetting glow =
            register(new BooleanSetting("Glow", "Soft glow around accented elements.", true));
    private final BooleanSetting roundedCorners =
            register(new BooleanSetting("Rounded Corners", "Use rounded panel corners.", true));
    private final NumberSetting cornerRadius =
            register(new NumberSetting("Corner Radius", "Panel corner radius in pixels.", 6, 0, 12, 1));
    private final EnumSetting<AnimationStyle> animationStyle =
            register(new EnumSetting<>("Animation", "Easing curve for UI animations.", AnimationStyle.EASE_OUT));
    private final NumberSetting animationSpeed =
            register(new NumberSetting("Animation Speed", "Higher is faster.", 1.0, 0.1, 3.0, 0.1));
    private final EnumSetting<FontChoice> font =
            register(new EnumSetting<>("Font", "Client UI font.", FontChoice.INTER));

    public InterfaceModule() {
        super("Interface", "Heaven theme, colors, blur, glow and animation settings.", Category.INTERFACE);
        setEnabled(true);
    }

    public int accent() {
        return accent.getValue();
    }

    public int background() {
        return background.getValue();
    }

    public int outline() {
        return outline.getValue();
    }

    /** UI background opacity as a fraction in [0,1]. */
    public float backgroundOpacity() {
        return (float) (backgroundOpacity.getValue() / 100.0);
    }

    public boolean blur() {
        return blur.getValue();
    }

    public boolean glow() {
        return glow.getValue();
    }

    public boolean roundedCorners() {
        return roundedCorners.getValue();
    }

    public int cornerRadius() {
        return roundedCorners.getValue() ? cornerRadius.getInt() : 0;
    }

    public AnimationStyle animationStyle() {
        return animationStyle.getValue();
    }

    public double animationSpeed() {
        return animationSpeed.getValue();
    }

    public FontChoice font() {
        return font.getValue();
    }
}
