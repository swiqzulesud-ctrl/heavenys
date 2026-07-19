package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles {@code /day} and {@code /night}, distinguished by the command label. */
public final class TimeCommand extends HeavenCommand {

    public TimeCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.time");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        World world = resolveWorld(sender);
        if (label.equals("night")) {
            world.setTime(13000L);
            messages.send(sender, "time.night");
        } else {
            world.setTime(1000L);
            messages.send(sender, "time.day");
        }
    }

    private World resolveWorld(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        return plugin.getServer().getWorlds().get(0);
    }
}
