package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.Teleports;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /tp <target>} - teleports the sender to a player.
 * {@code /tp <player> <target>} - teleports one player to another
 * (requires {@code heaven.command.tp.others}).
 */
public final class TpCommand extends AbstractCommand {

    public TpCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.tp");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return;
            }
            Player target = findPlayer(sender, args[0]);
            if (target == null) {
                return;
            }
            if (target.equals(player)) {
                messages().send(sender, "commands.tp.cannot-self");
                return;
            }
            Teleports.teleport(plugin.backService(), player, target.getLocation());
            messages().send(sender, "commands.tp.teleported",
                    Placeholder.unparsed("target", target.getName()));
            return;
        }

        if (args.length == 2) {
            if (!checkPermission(sender, "heaven.command.tp.others")) {
                return;
            }
            Player subject = findPlayer(sender, args[0]);
            if (subject == null) {
                return;
            }
            Player target = findPlayer(sender, args[1]);
            if (target == null) {
                return;
            }
            if (subject.equals(target)) {
                messages().send(sender, "commands.tp.cannot-self");
                return;
            }
            Teleports.teleport(plugin.backService(), subject, target.getLocation());
            messages().send(sender, "commands.tp.teleported-other",
                    Placeholder.unparsed("player", subject.getName()),
                    Placeholder.unparsed("target", target.getName()));
            return;
        }

        sendUsage(sender, "/" + label + " <target> | /" + label + " <player> <target>");
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        if (args.length == 2 && sender.hasPermission("heaven.command.tp.others")) {
            return completePlayers(args[1]);
        }
        return List.of();
    }
}
