package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /speed <1-10> [player]} - sets walk or fly speed depending on state. */
public final class SpeedCommand extends HeavenCommand {

    public SpeedCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.speed");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            usage(sender, "/speed <1-10> [player]");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            messages.send(sender, "speed.invalid");
            return;
        }
        if (level < 1 || level > 10) {
            messages.send(sender, "speed.invalid");
            return;
        }

        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("heaven.speed.others")) {
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

        float speed = level / 10.0f;
        if (target.isFlying() || target.getAllowFlight()) {
            target.setFlySpeed(speed);
        } else {
            target.setWalkSpeed(speed);
        }

        if (sender.equals(target)) {
            messages.send(sender, "speed.set", Messages.ph("speed", String.valueOf(level)));
        } else {
            messages.send(sender, "speed.set-other",
                    Messages.ph("target", target.getName()),
                    Messages.ph("speed", String.valueOf(level)));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return filter(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), args[0]);
        }
        if (args.length == 2 && sender.hasPermission("heaven.speed.others")) {
            return completePlayers(args[1]);
        }
        return List.of();
    }
}
