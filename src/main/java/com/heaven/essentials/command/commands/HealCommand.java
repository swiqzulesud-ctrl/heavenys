package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /heal [player]} - fully restores health, fire and saturation. */
public final class HealCommand extends HeavenCommand {

    public HealCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.heal");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission("heaven.heal.others")) {
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

        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        target.setHealth(maxHealth != null ? maxHealth.getValue() : 20.0D);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);
        target.setFreezeTicks(0);

        if (sender.equals(target)) {
            messages.send(sender, "heal.self");
        } else {
            messages.send(sender, "heal.other", Messages.ph("target", target.getName()));
            messages.send(target, "heal.by-other", Messages.ph("sender", sender.getName()));
        }
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1 && sender.hasPermission("heaven.heal.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
