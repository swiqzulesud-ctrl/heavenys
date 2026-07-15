package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /fly [player]} - toggles flight for the sender or another player.
 */
public final class FlyCommand extends AbstractCommand {

    public FlyCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.fly");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        Player target;
        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.fly.others")) {
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

        boolean enabled = !target.getAllowFlight();
        target.setAllowFlight(enabled);
        if (!enabled) {
            target.setFlying(false);
        }

        if (target.equals(sender)) {
            messages().send(sender, enabled ? "commands.fly.enabled-self" : "commands.fly.disabled-self");
        } else {
            messages().send(sender, enabled ? "commands.fly.enabled-other" : "commands.fly.disabled-other",
                    Placeholder.unparsed("target", target.getName()));
            messages().send(target, enabled ? "commands.fly.enabled-by" : "commands.fly.disabled-by");
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.fly.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
