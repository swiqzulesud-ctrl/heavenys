package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** {@code /msg <player> <message>} (aliases {@code /tell}, {@code /w}) - private message. */
public final class MessageCommand extends HeavenCommand {

    public MessageCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.msg");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            usage(sender, "/msg <player> <message>");
            return;
        }
        Player target = resolve(sender, args[0]);
        if (target == null) {
            return;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        MessageDelivery.send(plugin, sender, target, message);
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
