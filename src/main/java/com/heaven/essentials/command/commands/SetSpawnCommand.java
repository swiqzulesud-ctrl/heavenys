package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /setspawn} - set the global spawn to your current location. */
public final class SetSpawnCommand extends HeavenCommand {

    public SetSpawnCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.setspawn");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        plugin.configs().setSpawn(player.getLocation());
        messages.send(sender, "setspawn.success");
    }
}
