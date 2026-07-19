package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /invsee <player>} - view another player's inventory. */
public final class InvseeCommand extends HeavenCommand {

    public InvseeCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.invsee");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player viewer = asPlayer(sender);
        if (viewer == null) {
            return;
        }
        if (args.length == 0) {
            usage(sender, "/invsee <player>");
            return;
        }
        Player target = resolve(sender, args[0]);
        if (target == null) {
            return;
        }
        if (target.equals(viewer)) {
            messages.send(sender, "invsee.self-error");
            return;
        }
        viewer.openInventory(target.getInventory());
        messages.send(sender, "invsee.opening", Messages.ph("target", target.getName()));
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
