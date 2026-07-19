package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** {@code /gm <0|1|2|3|survival|creative|adventure|spectator> [player]} */
public final class GamemodeCommand extends HeavenCommand {

    public GamemodeCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.gamemode");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            usage(sender, "/gm <0|1|2|3|survival|creative|adventure|spectator> [player]");
            return;
        }

        GameMode mode = parse(args[0]);
        if (mode == null) {
            messages.send(sender, "gamemode.invalid", Messages.ph("input", args[0]));
            return;
        }

        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("heaven.gamemode.others")) {
                messages.send(sender, "general.no-permission");
                return;
            }
            target = resolve(sender, args[1]);
            if (target == null) {
                return;
            }
        } else {
            target = asPlayer(sender);
            if (target == null) {
                return;
            }
        }

        target.setGameMode(mode);
        String friendly = friendlyName(mode);

        if (sender.equals(target)) {
            messages.send(sender, "gamemode.changed", Messages.ph("gamemode", friendly));
        } else {
            messages.send(sender, "gamemode.changed-other",
                    Messages.ph("target", target.getName()),
                    Messages.ph("gamemode", friendly));
            messages.send(target, "gamemode.by-other",
                    Messages.ph("gamemode", friendly),
                    Messages.ph("sender", sender.getName()));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return filter(List.of("survival", "creative", "adventure", "spectator", "0", "1", "2", "3"), args[0]);
        }
        if (args.length == 2 && sender.hasPermission("heaven.gamemode.others")) {
            return completePlayers(args[1]);
        }
        return List.of();
    }

    private GameMode parse(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String friendlyName(GameMode mode) {
        String lower = mode.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
