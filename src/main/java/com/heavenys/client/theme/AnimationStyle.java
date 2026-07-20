/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.theme;

import java.util.function.DoubleUnaryOperator;

/** Easing curves used for the smooth UI animations. Input/output are in [0,1]. */
public enum AnimationStyle {
    LINEAR("Linear", t -> t),
    EASE_OUT("Ease Out", t -> 1 - Math.pow(1 - t, 3)),
    EASE_IN_OUT("Ease In/Out", t -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

    private final String displayName;
    private final DoubleUnaryOperator curve;

    AnimationStyle(String displayName, DoubleUnaryOperator curve) {
        this.displayName = displayName;
        this.curve = curve;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Applies the easing curve to a normalised progress value. */
    public double apply(double t) {
        return curve.applyAsDouble(Math.max(0.0, Math.min(1.0, t)));
    }
}
