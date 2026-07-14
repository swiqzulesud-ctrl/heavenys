package dev.smplugin.relic;

import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Construit et reconnaît l'objet-relique unique : la Hache Fend-Couronne.
 */
public final class RelicItems {

    private RelicItems() {
    }

    /** Crée l'unique Hache Fend-Couronne (Tranchant X, Butin IV, Solidité V). */
    public static ItemStack createRelic() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.legacy("<!italic><gold><bold>Hache Fend-Couronne</bold></gold>"));
            meta.setLore(Text.legacyLore(
                    "<white>Forgée dans la chute du Gardien Souverain.</white>",
                    "<gray>Il ne peut en exister qu'une seule.</gray>",
                    "",
                    "<gold>✦</gold> <white>Tranchant X</white>",
                    "<gold>✦</gold> <white>Butin IV</white>",
                    "<gold>✦</gold> <white>Solidité V</white>",
                    "",
                    "<gray><italic>La couronne obéit à qui tient la hache.</italic></gray>"));
            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            meta.addEnchant(Enchantment.LOOTING, 4, true);
            meta.addEnchant(Enchantment.UNBREAKING, 5, true);
            meta.getPersistentDataContainer().set(Keys.RELIC_ITEM, PersistentDataType.BYTE, (byte) 1);
            axe.setItemMeta(meta);
        }
        return axe;
    }

    /** True when the stack carries the relic PDC tag (survives renames/anvils). */
    public static boolean isRelic(ItemStack stack) {
        return stack != null
                && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(Keys.RELIC_ITEM, PersistentDataType.BYTE);
    }
}
