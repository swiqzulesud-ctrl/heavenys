/*
 * Heavenys Client - an open-source Minecraft client for Fabric.
 * Copyright (C) 2026 Heavenys Client contributors
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. See the LICENSE file for details.
 */
package com.heavenys.client.module.impl.compat;

import com.heavenys.client.compat.ModCompat;
import com.heavenys.client.module.Category;
import com.heavenys.client.module.Module;
import com.heavenys.client.module.setting.BooleanSetting;
import com.heavenys.client.module.setting.NumberSetting;
import com.heavenys.client.module.setting.StringSetting;

/**
 * Voice-chat control surface that only becomes active when Simple Voice Chat is
 * detected. Heavenys never bundles the mod; it merely exposes convenient
 * push-to-talk, microphone, volume, overlay, mute and deafen controls that the
 * player can use when the mod is present.
 */
public class VoiceChatModule extends Module {

    private final BooleanSetting pushToTalk =
            register(new BooleanSetting("Push To Talk", "Only transmit while a key is held.", true));
    private final StringSetting microphone =
            register(new StringSetting("Microphone", "Preferred input device name.", "Default"));
    private final NumberSetting volume =
            register(new NumberSetting("Volume", "Voice output volume.", 100, 0, 200, 1));
    private final BooleanSetting overlay =
            register(new BooleanSetting("Voice Overlay", "Show a speaking-indicator overlay.", true));
    private final BooleanSetting mute =
            register(new BooleanSetting("Mute", "Mute your microphone.", false));
    private final BooleanSetting deafen =
            register(new BooleanSetting("Deafen", "Mute all incoming voice.", false));

    public VoiceChatModule() {
        super("Voice Chat", "Simple Voice Chat controls (auto-detected).", Category.VOICE);
    }

    /** @return whether Simple Voice Chat is installed; controls are inert otherwise. */
    public boolean isAvailable() {
        return ModCompat.isVoiceChatPresent();
    }

    public boolean pushToTalk() {
        return pushToTalk.getValue();
    }

    public String microphone() {
        return microphone.getValue();
    }

    public double volume() {
        return volume.getValue() / 100.0;
    }

    public boolean overlay() {
        return overlay.getValue();
    }

    public boolean muted() {
        return mute.getValue();
    }

    public boolean deafened() {
        return deafen.getValue();
    }
}
