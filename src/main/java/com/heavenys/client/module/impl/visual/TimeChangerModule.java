/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.impl.visual;

import com.heavenys.client.module.setting.EnumSetting;

/** Client-side time-of-day override. */
public class TimeChangerModule extends VisualModule {

    /** Preset times mapped to their day-tick value. */
    public enum TimeOfDay {
        DAWN(0), NOON(6000), DUSK(12000), MIDNIGHT(18000);

        private final long ticks;

        TimeOfDay(long ticks) {
            this.ticks = ticks;
        }

        public long getTicks() {
            return ticks;
        }
    }

    private final EnumSetting<TimeOfDay> time =
            register(new EnumSetting<>("Time", "Client-side time of day.", TimeOfDay.NOON));

    public TimeChangerModule() {
        super("Time Changer", "Overrides the client-side time of day.");
    }

    public long getTicks() {
        return time.getValue().getTicks();
    }
}
