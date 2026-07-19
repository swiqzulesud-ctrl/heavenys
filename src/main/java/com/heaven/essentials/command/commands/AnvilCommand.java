package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /anvil} - open an anvil. */
public final class AnvilCommand extends HeavenCommand {

    public AnvilCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.anvil");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        player.openAnvil(null, true);
        messages.send(sender, "anvil.opening");
    }
}
