package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /fly [player]} - toggles flight. */
public final class FlyCommand extends HeavenCommand {

    public FlyCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.fly");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.fly.others")) {
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

        boolean enable = !target.getAllowFlight();
        target.setAllowFlight(enable);
        if (!enable) {
            target.setFlying(false);
        }

        if (sender.equals(target)) {
            messages.send(sender, enable ? "fly.enabled" : "fly.disabled");
        } else {
            messages.send(sender, enable ? "fly.enabled-other" : "fly.disabled-other",
                    Messages.ph("target", target.getName()));
            messages.send(target, "fly.by-other", Messages.ph("sender", sender.getName()));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.fly.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
