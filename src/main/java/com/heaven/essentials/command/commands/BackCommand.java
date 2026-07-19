package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /back} - return to your previous location. */
public final class BackCommand extends HeavenCommand {

    public BackCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.back");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        Location previous = plugin.back().getPrevious(player.getUniqueId());
        if (previous == null) {
            messages.send(sender, "back.none");
            return;
        }
        // The teleport listener records the current location, so /back toggles.
        player.teleport(previous);
        messages.send(sender, "back.success");
    }
}
