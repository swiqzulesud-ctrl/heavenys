package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** {@code /reply <message>} (alias {@code /r}) - reply to your last conversation. */
public final class ReplyCommand extends HeavenCommand {

    public ReplyCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.msg");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            usage(sender, "/reply <message>");
            return;
        }
        UUID targetId = plugin.chat().getReplyTarget(player.getUniqueId());
        Player target = targetId == null ? null : plugin.getServer().getPlayer(targetId);
        if (target == null) {
            messages.send(sender, "msg.no-reply-target");
            return;
        }
        MessageDelivery.send(plugin, sender, target, String.join(" ", args));
    }
}
