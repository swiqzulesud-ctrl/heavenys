package dev.heaven.essentials.lifesteal.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marker {@link InventoryHolder} used to identify the Lifesteal admin GUI.
 *
 * <p>Using a dedicated holder instead of title comparison makes GUI detection
 * reliable and cheap regardless of how the title is configured.</p>
 */
public final class HeartsGuiHolder implements InventoryHolder {

    private Inventory inventory;

    /** Links the created inventory back to this holder. */
    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
