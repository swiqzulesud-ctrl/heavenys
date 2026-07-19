package com.heaven.essentials.gui;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.config.Messages;
import net.kyori.adventure.text.Component;
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
 * this menu type safely (rather than matching on the fragile inventory title).</p>
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
        Component title = plugin.messages().render("gui.title");
        this.inventory = plugin.getServer().createInventory(this, SIZE, title);
        render();
    }

    /** (Re)builds every item in the menu to reflect the current maximum. */
    public void render() {
        inventory.clear();

        ItemStack filler = simpleItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Component.empty());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        int max = plugin.configs().getMaxHearts();
        Messages messages = plugin.messages();

        inventory.setItem(INFO_SLOT, named(
                Material.PAPER,
                messages.render("gui.info-name"),
                messages.renderList("gui.info-lore")));

        inventory.setItem(DECREASE_SLOT, named(
                Material.RED_DYE,
                messages.render("gui.decrease-name"),
                messages.renderList("gui.decrease-lore")));

        ItemStack display = named(
                Material.NETHER_STAR,
                messages.render("gui.max-hearts-name"),
                messages.renderList("gui.max-hearts-lore", Messages.ph("max", String.valueOf(max))));
        display.setAmount(Math.max(1, Math.min(max, 64)));
        inventory.setItem(DISPLAY_SLOT, display);

        inventory.setItem(INCREASE_SLOT, named(
                Material.LIME_DYE,
                messages.render("gui.increase-name"),
                messages.renderList("gui.increase-lore")));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private ItemStack simpleItem(Material material, Component name) {
        return named(material, name, List.of());
    }

    private ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            if (!lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> line.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                        .toList());
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }
}
