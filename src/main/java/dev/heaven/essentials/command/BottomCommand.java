package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.Teleports;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /bottom} - teleports the sender to the lowest safe air pocket at
 * their current X/Z position (two air blocks above a solid block).
 */
public final class BottomCommand extends AbstractCommand {

    public BottomCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.bottom");
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
        int x = current.getBlockX();
        int z = current.getBlockZ();

        // Scan upward from the world floor for the first safe two-block gap.
        for (int y = world.getMinHeight(); y < current.getBlockY(); y++) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (ground.isSolid() && feet.isPassable() && head.isPassable()
                    && !feet.isLiquid() && !head.isLiquid()) {
                Location destination = current.clone();
                destination.setY(y + 1.0);
                Teleports.teleport(plugin.backService(), player, destination);
                messages().send(sender, "commands.bottom.teleported");
                return;
            }
        }

        messages().send(sender, "commands.bottom.none");
    }
}
