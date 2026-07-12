package net.heavenys.client.hud;

import net.heavenys.client.config.HeavenysConfig;
import net.heavenys.client.hud.modules.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds every registered {@link HudModule} in insertion order.
 * Modules are instantiated here and wired to the active config.
 */
public class HudModuleRegistry {

    private final Map<String, HudModule> modules = new LinkedHashMap<>();

    public HudModuleRegistry(HeavenysConfig config) {
        register(new ArmorStatusModule(config));
        register(new PotionStatusModule(config));
        register(new KeystrokesModule(config));
        register(new FpsModule(config));
        register(new PingModule(config));
    }

    private void register(HudModule module) {
        modules.put(module.getId(), module);
    }

    /** All registered modules in registration order. */
    public Collection<HudModule> all() {
        return Collections.unmodifiableCollection(modules.values());
    }

    /** Lookup by module id. Returns {@code null} if not found. */
    public HudModule get(String id) {
        return modules.get(id);
    }

    /** Sync enabled states back to the config object (call before config.save()). */
    public void syncToConfig(HeavenysConfig config) {
        config.armorStatusEnabled  = get("armor_status").isEnabled();
        config.potionStatusEnabled = get("potion_status").isEnabled();
        config.keystrokesEnabled   = get("keystrokes").isEnabled();
        config.fpsEnabled          = get("fps").isEnabled();
        config.pingEnabled         = get("ping").isEnabled();
    }
}
