package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /god [player]} - toggles invulnerability. */
public final class GodCommand extends HeavenCommand {

    public GodCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.god");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.god.others")) {
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

        boolean god = plugin.god().toggle(target.getUniqueId());
        target.setInvulnerable(god);

        if (sender.equals(target)) {
            messages.send(sender, god ? "god.enabled" : "god.disabled");
        } else {
            messages.send(sender, god ? "god.enabled-other" : "god.disabled-other",
                    Messages.ph("target", target.getName()));
            messages.send(target, "god.by-other", Messages.ph("sender", sender.getName()));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.god.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
