package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles {@code /sun} and {@code /rain}, distinguished by the command label. */
public final class WeatherCommand extends HeavenCommand {

    public WeatherCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.weather");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        World world = resolveWorld(sender);
        if (label.equals("rain")) {
            world.setStorm(true);
            world.setWeatherDuration(6000);
            messages.send(sender, "weather.rain");
        } else {
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(6000);
            messages.send(sender, "weather.sun");
        }
    }

    private World resolveWorld(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        return plugin.getServer().getWorlds().get(0);
    }
}
