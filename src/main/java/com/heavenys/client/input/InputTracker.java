/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.input;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks mouse clicks-per-second and key states by polling GLFW each client
 * frame. Polling avoids any input mixins while still giving accurate rising-edge
 * click detection for the CPS and Keystrokes HUD modules.
 */
public class InputTracker {

    private static final long WINDOW_MS = 1000L;

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    private static final long COMBO_TIMEOUT_MS = 1500L;

    private boolean leftDown;
    private boolean rightDown;

    private int combo;
    private long lastLeftClickMs = -1;

    /** Polls the window; call once per client tick. */
    public void update(Minecraft mc) {
        long handle = mc.getWindow().handle();
        long now = System.currentTimeMillis();

        boolean left = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

        if (left && !leftDown) {
            leftClicks.add(now);
            // Combo tracks consecutive clicks; it resets after a short idle window.
            if (lastLeftClickMs > 0 && now - lastLeftClickMs > COMBO_TIMEOUT_MS) {
                combo = 0;
            }
            combo++;
            lastLeftClickMs = now;
        }
        if (right && !rightDown) {
            rightClicks.add(now);
        }
        leftDown = left;
        rightDown = right;

        if (lastLeftClickMs > 0 && now - lastLeftClickMs > COMBO_TIMEOUT_MS) {
            combo = 0;
        }

        prune(leftClicks, now);
        prune(rightClicks, now);
    }

    public int getCombo() {
        return combo;
    }

    public int getLeftCps() {
        return leftClicks.size();
    }

    public int getRightCps() {
        return rightClicks.size();
    }

    public boolean isLeftDown() {
        return leftDown;
    }

    public boolean isRightDown() {
        return rightDown;
    }

    /** @return whether a GLFW key is currently held. */
    public boolean isKeyDown(Minecraft mc, int glfwKey) {
        return GLFW.glfwGetKey(mc.getWindow().handle(), glfwKey) == GLFW.GLFW_PRESS;
    }

    private static void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MS) {
            clicks.pollFirst();
        }
    }
}
