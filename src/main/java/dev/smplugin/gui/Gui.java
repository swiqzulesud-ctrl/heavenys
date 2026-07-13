package dev.smplugin.gui;

import dev.smplugin.SMPlugin;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Items;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for all SMPlugin menus: white-glass borders, per-slot click
 * handlers and a shared back/close button, dispatched by {@link GuiListener}.
 */
public abstract class Gui implements InventoryHolder {

    protected final SMPlugin plugin;
    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> handlers = new HashMap<>();

    protected Gui(SMPlugin plugin, int rows, String titleMini) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, rows * 9, Text.legacy(titleMini));
    }

    /** Populates the inventory for the given viewer. */
    protected abstract void build(Player viewer);

    public void open(Player viewer) {
        inventory.clear();
        handlers.clear();
        build(viewer);
        fillEmpty();
        viewer.openInventory(inventory);
        Fx.click(viewer);
    }

    /** Places an item with an optional click action. */
    protected void set(int slot, ItemStack item, Consumer<Player> onClick) {
        inventory.setItem(slot, item);
        if (onClick != null) {
            handlers.put(slot, onClick);
        } else {
            handlers.remove(slot);
        }
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Standard back button that runs the given action. */
    protected void back(int slot, Consumer<Player> onClick) {
        set(slot, Items.backButton(), onClick);
    }

    /** Standard close button (a themed barrier). */
    protected void close(int slot) {
        set(slot, Items.gui(org.bukkit.Material.BARRIER, "<white>✕ Close", "<gray>Close this menu."),
                p -> {
                    p.closeInventory();
                    Fx.click(p);
                });
    }

    /** Fills every empty slot with the white glass filler — no bare slots. */
    private void fillEmpty() {
        ItemStack filler = Items.filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    void handleClick(Player player, int slot) {
        Consumer<Player> handler = handlers.get(slot);
        if (handler != null) {
            handler.accept(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
