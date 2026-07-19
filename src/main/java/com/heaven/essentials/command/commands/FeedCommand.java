package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /feed [player]} - refills hunger and saturation. */
public final class FeedCommand extends HeavenCommand {

    public FeedCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.feed");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.feed.others")) {
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

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setExhaustion(0f);

        if (sender.equals(target)) {
            messages.send(sender, "feed.self");
        } else {
            messages.send(sender, "feed.other", Messages.ph("target", target.getName()));
            messages.send(target, "feed.by-other", Messages.ph("sender", sender.getName()));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.feed.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
