/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.render;

/**
 * Small helper for packing/unpacking and blending ARGB colors. Kept independent
 * of Minecraft so it can be unit-tested and reused across the UI.
 */
public final class HeavenysColors {

    private HeavenysColors() {
    }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int alpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    public static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    public static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    /** Returns {@code argb} with its alpha channel replaced by {@code alpha} (0-255). */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Returns {@code argb} with its alpha scaled by {@code factor} (0.0-1.0). */
    public static int scaleAlpha(int argb, float factor) {
        int a = Math.round(alpha(argb) * Math.max(0f, Math.min(1f, factor)));
        return withAlpha(argb, a);
    }

    /** Linearly interpolates between two ARGB colors. {@code t} is clamped to [0,1]. */
    public static int lerp(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(alpha(from) + (alpha(to) - alpha(from)) * t);
        int r = Math.round(red(from) + (red(to) - red(from)) * t);
        int g = Math.round(green(from) + (green(to) - green(from)) * t);
        int b = Math.round(blue(from) + (blue(to) - blue(from)) * t);
        return argb(a, r, g, b);
    }
}
