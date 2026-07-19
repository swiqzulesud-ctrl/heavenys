package com.heaven.essentials.gui;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.config.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * The Heaven-themed Lifesteal admin GUI opened with {@code /heartsteal}.
 *
 * <p>Implements {@link InventoryHolder} so click events can be routed back to
 * this menu type safely (rather than matching on the fragile inventory title).
 * Titles and item text are rendered from MiniMessage and serialised to legacy
 * (§) strings, because Spigot's inventory/item APIs are string-based.</p>
 */
public final class HeartStealMenu implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int INFO_SLOT = 4;
    public static final int DECREASE_SLOT = 11;
    public static final int DISPLAY_SLOT = 13;
    public static final int INCREASE_SLOT = 15;

    private final HeavenEssentials plugin;
    private final Inventory inventory;

    public HeartStealMenu(HeavenEssentials plugin) {
        this.plugin = plugin;
        String title = plugin.messages().legacy("gui.title");
        this.inventory = plugin.getServer().createInventory(this, SIZE, title);
        render();
    }

    /** (Re)builds every item in the menu to reflect the current maximum. */
    public void render() {
        inventory.clear();

        ItemStack filler = named(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        int max = plugin.configs().getMaxHearts();
        Messages messages = plugin.messages();

        inventory.setItem(INFO_SLOT, named(
                Material.PAPER,
                messages.legacy("gui.info-name"),
                messages.renderListLegacy("gui.info-lore")));

        inventory.setItem(DECREASE_SLOT, named(
                Material.RED_DYE,
                messages.legacy("gui.decrease-name"),
                messages.renderListLegacy("gui.decrease-lore")));

        ItemStack display = named(
                Material.NETHER_STAR,
                messages.legacy("gui.max-hearts-name"),
                messages.renderListLegacy("gui.max-hearts-lore",
                        Messages.ph("max", String.valueOf(max))));
        display.setAmount(Math.max(1, Math.min(max, 64)));
        inventory.setItem(DISPLAY_SLOT, display);

        inventory.setItem(INCREASE_SLOT, named(
                Material.LIME_DYE,
                messages.legacy("gui.increase-name"),
                messages.renderListLegacy("gui.increase-lore")));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }
}
