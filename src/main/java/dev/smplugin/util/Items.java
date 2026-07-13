package dev.smplugin.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * ItemStack builders used by every GUI, keeping lore/name theming in one place.
 */
public final class Items {

    private Items() {
    }

    /** Builds a named, lored item tagged as a GUI element (non-takeable). */
    public static ItemStack gui(Material material, String nameMini, String... loreMini) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Text.mm("<!italic>" + nameMini));
            if (loreMini.length > 0) {
                meta.lore(Text.lore(loreMini));
            }
            meta.getPersistentDataContainer().set(Keys.GUI_ITEM, PersistentDataType.BYTE, (byte) 1);
        });
        return stack;
    }

    /** Same as {@link #gui} but with a pre-built lore list. */
    public static ItemStack gui(Material material, String nameMini, List<Component> lore) {
        ItemStack stack = gui(material, nameMini);
        stack.editMeta(meta -> meta.lore(lore));
        return stack;
    }

    /** The white glass pane used to border every SMPlugin inventory. */
    public static ItemStack filler() {
        return gui(Material.WHITE_STAINED_GLASS_PANE, "<white> ");
    }

    /** Standard back button. */
    public static ItemStack backButton() {
        return gui(Material.ARROW, "<white>← Back", "<gray>Return to the previous menu.");
    }

    /** A player head with the given owner's skin, themed name and lore. */
    public static ItemStack head(OfflinePlayer owner, String nameMini, String... loreMini) {
        ItemStack stack = gui(Material.PLAYER_HEAD, nameMini, loreMini);
        stack.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(owner));
        return stack;
    }

    /** A player head resolved by name (skin fetched async by the server). */
    public static ItemStack headByName(String playerName, String nameMini, String... loreMini) {
        return head(Bukkit.getOfflinePlayer(playerName), nameMini, loreMini);
    }
}
