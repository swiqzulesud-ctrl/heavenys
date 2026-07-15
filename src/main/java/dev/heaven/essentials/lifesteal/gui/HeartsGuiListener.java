package dev.heaven.essentials.lifesteal.gui;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Protects the Lifesteal admin GUI from item movement and routes button
 * clicks to {@link HeartsGui}.
 */
public final class HeartsGuiListener implements Listener {

    private final HeavenEssentials plugin;

    public HeartsGuiListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof HeartsGuiHolder)) {
            return;
        }
        // The GUI is read-only: block every interaction, including shift-clicks
        // from the player's own inventory.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Only handle clicks inside the top (GUI) inventory.
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        plugin.heartsGui().handleClick(player, event.getSlot());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof HeartsGuiHolder) {
            event.setCancelled(true);
        }
    }
}
