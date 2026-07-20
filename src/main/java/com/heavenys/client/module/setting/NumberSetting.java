/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.setting;

/** A numeric slider value bounded by a minimum and maximum, with an increment step. */
public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public float getFloat() {
        return getValue().floatValue();
    }

    public int getInt() {
        return (int) Math.round(getValue());
    }

    @Override
    public void setValue(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        if (step > 0) {
            clamped = Math.round(clamped / step) * step;
        }
        super.setValue(clamped);
    }

    /** Sets the value from a normalised [0,1] slider position. */
    public void setFromFraction(double fraction) {
        setValue(min + (max - min) * Math.max(0.0, Math.min(1.0, fraction)));
    }

    /** @return the current value as a normalised [0,1] slider position. */
    public double getFraction() {
        if (max == min) {
            return 0.0;
        }
        return (getValue() - min) / (max - min);
    }

    @Override
    public Object write() {
        return getValue();
    }

    @Override
    public void read(Object serialized) {
        if (serialized instanceof Number n) {
            setValue(n.doubleValue());
        }
    }
}
