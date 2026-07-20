/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.util;

/**
 * Estimates the effective server tick rate from the interval between received
 * level ticks. This is a client-side approximation (it reflects how fast the
 * world is actually advancing), not a privileged server metric.
 */
public class TpsTracker {

    private long lastTickMs = -1;
    private double averageInterval = 50.0; // 50ms == 20 TPS

    /** Call once per received level tick. */
    public void onLevelTick() {
        long now = System.currentTimeMillis();
        if (lastTickMs > 0) {
            double interval = now - lastTickMs;
            // Exponential moving average keeps the readout stable.
            averageInterval = averageInterval * 0.9 + interval * 0.1;
        }
        lastTickMs = now;
    }

    public double getTps() {
        if (averageInterval <= 0) {
            return 20.0;
        }
        return Math.min(20.0, 1000.0 / averageInterval);
    }

    public void reset() {
        lastTickMs = -1;
        averageInterval = 50.0;
    }
}
