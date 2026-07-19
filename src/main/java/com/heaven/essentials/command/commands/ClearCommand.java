package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /clear [player]} - clear an inventory. */
public final class ClearCommand extends HeavenCommand {

    public ClearCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.clear");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.clear.others")) {
                messages.send(sender, "general.no-permission");
                return;
            }
            target = resolve(sender, args[0]);
            if (target == null) {
                return;
            }
        } else {
            target = asPlayer(sender);
            if (target == null) {
                return;
            }
        }

        target.getInventory().clear();

        if (sender.equals(target)) {
            messages.send(sender, "clear.self");
        } else {
            messages.send(sender, "clear.other", Messages.ph("target", target.getName()));
            messages.send(target, "clear.by-other", Messages.ph("sender", sender.getName()));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.clear.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
