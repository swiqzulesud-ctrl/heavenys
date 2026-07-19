package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /tphere <player>} - teleport a player to you. */
public final class TpHereCommand extends HeavenCommand {

    public TpHereCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.tphere");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player self = asPlayer(sender);
        if (self == null) {
            return;
        }
        if (args.length == 0) {
            usage(sender, "/tphere <player>");
            return;
        }
        Player target = resolve(sender, args[0]);
        if (target == null) {
            return;
        }
        target.teleport(self.getLocation());
        messages.send(sender, "tphere.success", Messages.ph("target", target.getName()));
        messages.send(target, "tphere.notify", Messages.ph("sender", self.getName()));
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
