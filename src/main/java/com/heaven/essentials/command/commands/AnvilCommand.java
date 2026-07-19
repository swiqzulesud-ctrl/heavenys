package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

/** {@code /anvil} - open an anvil. */
public final class AnvilCommand extends HeavenCommand {

    public AnvilCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.anvil");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        // Spigot's HumanEntity has no openAnvil(); create an anvil view instead.
        try {
            Inventory anvil = plugin.getServer().createInventory(player, InventoryType.ANVIL);
            player.openInventory(anvil);
            messages.send(sender, "anvil.opening");
        } catch (IllegalArgumentException ex) {
            messages.send(sender, "anvil.unsupported");
        }
    }
}
