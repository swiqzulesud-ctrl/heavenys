package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** {@code /hat} - wear the held item on your head. */
public final class HatCommand extends HeavenCommand {

    public HatCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.hat");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            messages.send(sender, "hat.no-item");
            return;
        }
        ItemStack currentHelmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(hand.clone());
        player.getInventory().setItemInMainHand(currentHelmet == null ? new ItemStack(org.bukkit.Material.AIR) : currentHelmet);
        messages.send(sender, "hat.success");
    }
}
