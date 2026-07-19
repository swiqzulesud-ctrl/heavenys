package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;

/** {@code /broadcast <message>} (alias {@code /bc}) - message the whole server. */
public final class BroadcastCommand extends HeavenCommand {

    public BroadcastCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.broadcast");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            usage(sender, "/broadcast <message>");
            return;
        }
        String message = String.join(" ", args);
        plugin.audiences().all().sendMessage(messages.render("broadcast.format", Messages.ph("message", message)));
    }
}
