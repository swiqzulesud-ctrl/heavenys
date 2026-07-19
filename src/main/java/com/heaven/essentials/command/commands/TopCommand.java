package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /top} - teleport to the highest block above you. */
public final class TopCommand extends HeavenCommand {

    public TopCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.top");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Location origin = player.getLocation();
        int highestY = origin.getWorld().getHighestBlockYAt(origin);
        if (highestY < origin.getWorld().getMinHeight()) {
            messages.send(sender, "top.none");
            return;
        }
        Location destination = new Location(origin.getWorld(),
                origin.getBlockX() + 0.5, highestY + 1, origin.getBlockZ() + 0.5,
                origin.getYaw(), origin.getPitch());
        player.teleport(destination);
        messages.send(sender, "top.success");
    }
}
