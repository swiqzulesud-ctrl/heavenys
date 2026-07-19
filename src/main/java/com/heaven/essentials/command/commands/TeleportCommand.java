package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /tp <player> [target]} - teleport to a player, or one player to another. */
public final class TeleportCommand extends HeavenCommand {

    public TeleportCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.tp");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            usage(sender, "/tp <player> [target]");
            return;
        }

        if (args.length == 1) {
            Player self = asPlayer(sender);
            if (self == null) {
                return;
            }
            Player destination = resolve(sender, args[0]);
            if (destination == null) {
                return;
            }
            self.teleport(destination.getLocation());
            messages.send(sender, "tp.to-player", Messages.ph("target", destination.getName()));
            return;
        }

        Player moved = resolve(sender, args[0]);
        Player destination = resolve(sender, args[1]);
        if (moved == null || destination == null) {
            return;
        }
        moved.teleport(destination.getLocation());
        messages.send(sender, "tp.player-to",
                Messages.ph("player", moved.getName()),
                Messages.ph("target", destination.getName()));
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 || args.length == 2) {
            return completePlayers(args[args.length - 1]);
        }
        return List.of();
    }
}
