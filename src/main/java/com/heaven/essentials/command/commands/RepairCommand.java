package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;

/** {@code /repair [all]} - repair the held item or the whole inventory. */
public final class RepairCommand extends HeavenCommand {

    public RepairCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.repair");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("all")) {
            for (ItemStack item : player.getInventory().getContents()) {
                repair(item);
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                repair(item);
            }
            messages.send(sender, "repair.all");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            messages.send(sender, "repair.nothing");
            return;
        }
        if (!repair(hand)) {
            messages.send(sender, "repair.nothing");
            return;
        }
        messages.send(sender, "repair.hand");
    }

    /** Resets the durability of a damageable item; returns true if it changed. */
    private boolean repair(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (item.getItemMeta() instanceof Damageable damageable && damageable.hasDamage()) {
            damageable.setDamage(0);
            item.setItemMeta(damageable);
            return true;
        }
        return false;
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return filter(List.of("all"), args[0]);
        }
        return List.of();
    }
}
