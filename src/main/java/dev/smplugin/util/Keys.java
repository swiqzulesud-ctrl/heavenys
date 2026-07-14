package dev.smplugin.util;

import dev.smplugin.SMPlugin;
import org.bukkit.NamespacedKey;

/**
 * All {@link NamespacedKey}s used for PersistentDataContainer tags and
 * attribute modifiers. Created once at plugin enable.
 */
public final class Keys {

    /** Tags the one-of-a-kind Sovereign's Relic item stack. */
    public static NamespacedKey RELIC_ITEM;
    /** Tags GUI items so shift-click tricks can never extract them. */
    public static NamespacedKey GUI_ITEM;
    /** Tags the Sovereign Guardian entity and its summoned adds. */
    public static NamespacedKey GUARDIAN;
    public static NamespacedKey GUARDIAN_ADD;
    /** Tags crown-holder hologram displays for cleanup on restart. */
    public static NamespacedKey CROWN_HOLO;
    /** Attribute modifier key for builder-vote bonus hearts. */
    public static NamespacedKey BONUS_HEARTS;

    private Keys() {
    }

    public static void init(SMPlugin plugin) {
        RELIC_ITEM = new NamespacedKey(plugin, "relic_item");
        GUI_ITEM = new NamespacedKey(plugin, "gui_item");
        GUARDIAN = new NamespacedKey(plugin, "sovereign_guardian");
        GUARDIAN_ADD = new NamespacedKey(plugin, "guardian_add");
        CROWN_HOLO = new NamespacedKey(plugin, "crown_holo");
        BONUS_HEARTS = new NamespacedKey(plugin, "bonus_hearts");
    }
}
