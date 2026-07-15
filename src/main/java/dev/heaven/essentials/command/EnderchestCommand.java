package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /enderchest [player]} (alias {@code /ec}) - opens the sender's own
 * ender chest, or another player's with {@code heaven.command.enderchest.others}.
 */
public final class EnderchestCommand extends AbstractCommand {

    public EnderchestCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.enderchest");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.enderchest.others")) {
                return;
            }
            Player target = findPlayer(sender, args[0]);
            if (target == null) {
                return;
            }
            player.openInventory(target.getEnderChest());
            messages().send(sender, "commands.enderchest.opened-other",
                    Placeholder.unparsed("target", target.getName()));
            return;
        }

        player.openInventory(player.getEnderChest());
        messages().send(sender, "commands.enderchest.opened-self");
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.enderchest.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
