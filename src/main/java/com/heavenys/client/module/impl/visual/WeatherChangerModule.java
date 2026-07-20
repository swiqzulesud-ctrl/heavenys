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

/** Client-side weather override. */
public class WeatherChangerModule extends VisualModule {

    public enum Weather {
        CLEAR, RAIN, THUNDER
    }

    private final EnumSetting<Weather> weather =
            register(new EnumSetting<>("Weather", "Client-side weather.", Weather.CLEAR));

    public WeatherChangerModule() {
        super("Weather Changer", "Overrides the client-side weather.");
    }

    public Weather getWeather() {
        return weather.getValue();
    }
}
