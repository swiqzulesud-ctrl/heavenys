package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /spawn [player]} - teleport to the global spawn. */
public final class SpawnCommand extends HeavenCommand {

    public SpawnCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.spawn");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Location spawn = plugin.configs().getSpawn();
        if (spawn == null) {
            messages.send(sender, "spawn.not-set");
            return;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.spawn.others")) {
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

        target.teleport(spawn);
        if (sender.equals(target)) {
            messages.send(sender, "spawn.teleported");
        } else {
            messages.send(sender, "spawn.teleported-other", Messages.ph("target", target.getName()));
            messages.send(target, "spawn.teleported");
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.spawn.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
