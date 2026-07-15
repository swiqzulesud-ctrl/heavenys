package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /setspawn} - sets the global spawn point at the sender's location.
 */
public final class SetspawnCommand extends AbstractCommand {

    public SetspawnCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.setspawn");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length != 0) {
            sendUsage(sender, "/" + label);
            return;
        }

        Location location = player.getLocation();
        plugin.spawnService().setSpawn(location);
        messages().send(sender, "commands.setspawn.set",
                Placeholder.unparsed("world", location.getWorld().getName()),
                Placeholder.unparsed("x", String.valueOf(location.getBlockX())),
                Placeholder.unparsed("y", String.valueOf(location.getBlockY())),
                Placeholder.unparsed("z", String.valueOf(location.getBlockZ())));
    }
}
