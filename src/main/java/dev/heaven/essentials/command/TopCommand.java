package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.Teleports;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /top} - teleports the sender onto the highest solid block at their
 * current X/Z position.
 */
public final class TopCommand extends AbstractCommand {

    public TopCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.top");
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

        Location current = player.getLocation();
        World world = player.getWorld();
        int highestY = world.getHighestBlockYAt(current.getBlockX(), current.getBlockZ());

        Location destination = current.clone();
        destination.setY(highestY + 1.0);

        Teleports.teleport(plugin.backService(), player, destination);
        messages().send(sender, "commands.top.teleported");
    }
}
