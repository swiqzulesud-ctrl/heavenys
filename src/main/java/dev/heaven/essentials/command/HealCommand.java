package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /heal [player]} - restores full health, extinguishes fire and
 * refills hunger for the sender or another player.
 */
public final class HealCommand extends AbstractCommand {

    public HealCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.heal");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        Player target;
        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.heal.others")) {
                return;
            }
            target = findPlayer(sender, args[0]);
            if (target == null) {
                return;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                return;
            }
        }

        heal(target);

        if (target.equals(sender)) {
            messages().send(sender, "commands.heal.healed-self");
        } else {
            messages().send(sender, "commands.heal.healed-other",
                    Placeholder.unparsed("target", target.getName()));
            messages().send(target, "commands.heal.healed-by");
        }
    }

    private void heal(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
        player.setFireTicks(0);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.heal.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
