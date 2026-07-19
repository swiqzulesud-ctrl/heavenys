package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /enderchest [player]} (alias {@code /ec}) - open an ender chest. */
public final class EnderchestCommand extends HeavenCommand {

    public EnderchestCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.enderchest");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player viewer = asPlayer(sender);
        if (viewer == null) {
            return;
        }

        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.enderchest.others")) {
                messages.send(sender, "general.no-permission");
                return;
            }
            Player target = resolve(sender, args[0]);
            if (target == null) {
                return;
            }
            viewer.openInventory(target.getEnderChest());
            messages.send(sender, "enderchest.opening-other", Messages.ph("target", target.getName()));
            return;
        }

        viewer.openInventory(viewer.getEnderChest());
        messages.send(sender, "enderchest.opening-self");
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.enderchest.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
