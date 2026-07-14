package dev.smplugin.util;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * ItemStack builders used by every GUI, keeping name/lore theming in one place.
 * Uses legacy strings because Spigot/Arclight item meta predates components.
 */
public final class Items {

    private Items() {
    }

    /** Builds a named, lored item tagged as a GUI element (non-takeable). */
    public static ItemStack gui(Material material, String nameMini, String... loreMini) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.legacy("<!italic>" + nameMini));
            if (loreMini.length > 0) {
                meta.setLore(Text.legacyLore(loreMini));
            }
            meta.getPersistentDataContainer().set(Keys.GUI_ITEM, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Same as {@link #gui} but with a pre-built legacy lore list. */
    public static ItemStack gui(Material material, String nameMini, List<String> legacyLore) {
        ItemStack stack = gui(material, nameMini);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setLore(legacyLore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** The white glass pane used to border every SMPlugin inventory. */
    public static ItemStack filler() {
        return gui(Material.WHITE_STAINED_GLASS_PANE, "<white> ");
    }

    /** Standard back button. */
    public static ItemStack backButton() {
        return gui(Material.ARROW, "<white>← Retour", "<gray>Revenir au menu précédent.");
    }

    /** A player head with the given owner's skin, themed name and lore. */
    public static ItemStack head(OfflinePlayer owner, String nameMini, String... loreMini) {
        ItemStack stack = gui(Material.PLAYER_HEAD, nameMini, loreMini);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
            stack.setItemMeta(skull);
        }
        return stack;
    }
}
