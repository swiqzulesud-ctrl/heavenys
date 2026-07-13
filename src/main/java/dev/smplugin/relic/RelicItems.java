package dev.smplugin.relic;

import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Builds and recognises the one-of-a-kind relic item, the Crown-Splitter Axe.
 */
public final class RelicItems {

    private RelicItems() {
    }

    /** Creates the unique Crown-Splitter Axe (Sharpness X, Looting IV, Unbreaking V). */
    public static ItemStack createRelic() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        axe.editMeta(meta -> {
            meta.displayName(Text.mm("<!italic><gold><bold>Crown-Splitter Axe</bold></gold>"));
            meta.lore(Text.lore(
                    "<white>Forged in the fall of the Sovereign Guardian.</white>",
                    "<gray>Only one may ever exist.</gray>",
                    "",
                    "<gold>✦</gold> <white>Sharpness X</white>",
                    "<gold>✦</gold> <white>Looting IV</white>",
                    "<gold>✦</gold> <white>Unbreaking V</white>",
                    "",
                    "<gray><italic>The crown answers to whoever holds the axe.</italic></gray>"));
            meta.addEnchant(enchant("sharpness"), 10, true);
            meta.addEnchant(enchant("looting"), 4, true);
            meta.addEnchant(enchant("unbreaking"), 5, true);
            meta.getPersistentDataContainer().set(Keys.RELIC_ITEM, PersistentDataType.BYTE, (byte) 1);
        });
        return axe;
    }

    /** True when the stack carries the relic PDC tag (survives renames/anvils). */
    public static boolean isRelic(ItemStack stack) {
        return stack != null
                && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(Keys.RELIC_ITEM, PersistentDataType.BYTE);
    }

    private static Enchantment enchant(String key) {
        return RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT)
                .getOrThrow(NamespacedKey.minecraft(key));
    }
}
