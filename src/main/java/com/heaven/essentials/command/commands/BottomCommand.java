package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /bottom} - teleport to the lowest safe standing spot below you. */
public final class BottomCommand extends HeavenCommand {

    public BottomCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.bottom");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Location origin = player.getLocation();
        World world = origin.getWorld();
        int x = origin.getBlockX();
        int z = origin.getBlockZ();

        for (int y = world.getMinHeight(); y < origin.getBlockY(); y++) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (ground.getType().isSolid() && feet.isPassable() && head.isPassable()) {
                Location destination = new Location(world, x + 0.5, y + 1, z + 0.5,
                        origin.getYaw(), origin.getPitch());
                player.teleport(destination);
                messages.send(sender, "bottom.success");
                return;
            }
        }
        messages.send(sender, "bottom.none");
    }
}
