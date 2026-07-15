package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /ignore <player>} - toggles ignoring private messages from another
 * player. Ignore lists are persisted in data.yml.
 */
public final class IgnoreCommand extends AbstractCommand {

    public IgnoreCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.ignore");
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
            messages().send(sender, "commands.ignore.cannot-self");
            return;
        }

        boolean nowIgnoring = plugin.privateMessageService()
                .toggleIgnore(player.getUniqueId(), target.getUniqueId());
        messages().send(sender, nowIgnoring ? "commands.ignore.added" : "commands.ignore.removed",
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
