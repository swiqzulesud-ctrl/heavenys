package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.Teleports;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /tphere <player>} - teleports another player to the sender.
 */
public final class TphereCommand extends AbstractCommand {

    public TphereCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.tphere");
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
            messages().send(sender, "commands.tphere.cannot-self");
            return;
        }

        Teleports.teleport(plugin.backService(), target, player.getLocation());
        messages().send(sender, "commands.tphere.teleported",
                Placeholder.unparsed("target", target.getName()));
        messages().send(target, "commands.tphere.notify",
                Placeholder.unparsed("player", player.getName()));
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
