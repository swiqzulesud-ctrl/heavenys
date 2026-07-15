package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.Teleports;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /spawn [player]} - teleports the sender (or another player, with
 * {@code heaven.command.spawn.others}) to the global spawn point.
 */
public final class SpawnCommand extends AbstractCommand {

    public SpawnCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.spawn");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        Location spawn = plugin.spawnService().getSpawn();
        if (spawn == null || spawn.getWorld() == null) {
            messages().send(sender, "commands.spawn.not-set");
            return;
        }

        Player target;
        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.spawn.others")) {
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

        Teleports.teleport(plugin.backService(), target, spawn);
        if (target.equals(sender)) {
            messages().send(sender, "commands.spawn.teleported");
        } else {
            messages().send(sender, "commands.spawn.teleported-other",
                    Placeholder.unparsed("target", target.getName()));
            messages().send(target, "commands.spawn.teleported-by");
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.spawn.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
