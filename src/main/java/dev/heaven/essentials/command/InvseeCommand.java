package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /invsee <player>} - opens another player's inventory for viewing
 * and editing.
 */
public final class InvseeCommand extends AbstractCommand {

    public InvseeCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.invsee");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length != 1) {
            sendUsage(sender, "/" + label + " <player>");
            return;
        }

        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        if (target.equals(player)) {
            messages().send(sender, "commands.invsee.cannot-self");
            return;
        }

        player.openInventory(target.getInventory());
        messages().send(sender, "commands.invsee.opened",
                Placeholder.unparsed("target", target.getName()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
