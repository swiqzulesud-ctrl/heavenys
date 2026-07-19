package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /ignore <player>} - toggle ignoring a player's private messages. */
public final class IgnoreCommand extends HeavenCommand {

    public IgnoreCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.ignore");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            usage(sender, "/ignore <player>");
            return;
        }
        Player target = resolve(sender, args[0]);
        if (target == null) {
            return;
        }
        if (target.equals(player)) {
            messages.send(sender, "ignore.self-error");
            return;
        }
        boolean ignoring = plugin.chat().toggleIgnore(player.getUniqueId(), target.getUniqueId());
        messages.send(sender, ignoring ? "ignore.added" : "ignore.removed",
                Messages.ph("target", target.getName()));
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
