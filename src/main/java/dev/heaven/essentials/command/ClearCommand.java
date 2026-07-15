package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /clear [player]} - clears the inventory (including armor and
 * off-hand) of the sender or another player.
 */
public final class ClearCommand extends AbstractCommand {

    public ClearCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.clear");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        Player target;
        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.clear.others")) {
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

        target.getInventory().clear();

        if (target.equals(sender)) {
            messages().send(sender, "commands.clear.cleared-self");
        } else {
            messages().send(sender, "commands.clear.cleared-other",
                    Placeholder.unparsed("target", target.getName()));
            messages().send(target, "commands.clear.cleared-by");
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.clear.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
