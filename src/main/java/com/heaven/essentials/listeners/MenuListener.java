package com.heaven.essentials.listeners;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.config.ConfigManager;
import com.heaven.essentials.config.Messages;
import com.heaven.essentials.gui.HeartStealMenu;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Routes clicks inside the {@link HeartStealMenu} to maximum-hearts changes.
 * Because the menu implements {@link InventoryHolder}, this never relies on the
 * (localisable) inventory title.
 */
public final class MenuListener implements Listener {

    private final HeavenEssentials plugin;

    public MenuListener(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof HeartStealMenu menu)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null) {
            return;
        }
        if (!player.hasPermission("plugin.admin")) {
            return;
        }

        int delta;
        switch (event.getRawSlot()) {
            case HeartStealMenu.INCREASE_SLOT -> delta = event.isShiftClick() ? 5 : 1;
            case HeartStealMenu.DECREASE_SLOT -> delta = event.isShiftClick() ? -5 : -1;
            default -> {
                return;
            }
        }

        adjustMaximum(player, menu, delta);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof HeartStealMenu) {
            event.setCancelled(true);
        }
    }

    private void adjustMaximum(Player player, HeartStealMenu menu, int delta) {
        ConfigManager config = plugin.configs();
        Messages messages = plugin.messages();

        int current = config.getMaxHearts();
        int applied = config.setMaxHearts(current + delta);

        if (applied == current) {
            messages.send(player, delta > 0
                    ? "admin.max-limit-reached-high"
                    : "admin.max-limit-reached-low");
            return;
        }

        // If the maximum was lowered, clamp any players that now exceed it.
        if (applied < current) {
            int affected = plugin.hearts().clampAllToMax();
            if (affected > 0 && config.shouldAnnounceClamp()) {
                messages.send(player, "admin.clamp-announce",
                        Messages.ph("max", String.valueOf(applied)));
            }
        }

        menu.render();
        messages.send(player, "admin.max-updated", Messages.ph("max", String.valueOf(applied)));
        player.playSound(Sound.sound(Key.key("minecraft:ui.button.click"), Sound.Source.MASTER, 0.6f, 1.5f));
    }
}
