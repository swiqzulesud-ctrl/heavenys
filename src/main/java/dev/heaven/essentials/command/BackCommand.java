package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /back} - returns the sender to their previous location (before
 * their last plugin teleport or death).
 */
public final class BackCommand extends AbstractCommand {

    public BackCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.back");
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

        Location previous = plugin.backService().getLastLocation(player.getUniqueId());
        if (previous == null || previous.getWorld() == null) {
            messages().send(sender, "commands.back.none");
            return;
        }

        // Record the current position so /back can toggle between two spots.
        plugin.backService().recordLocation(player);
        player.teleportAsync(previous);
        messages().send(sender, "commands.back.teleported");
    }
}
