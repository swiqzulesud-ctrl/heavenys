package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.gui.HeartStealMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** {@code /heartsteal [reload]} - open the Lifesteal admin GUI or reload configs. */
public final class HeartStealCommand extends HeavenCommand {

    public HeartStealCommand(HeavenEssentials plugin) {
        super(plugin, "plugin.admin");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            messages.send(sender, "general.reload-success");
            return;
        }

        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        new HeartStealMenu(plugin).open(player);
    }

    @Override
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        if (args.length == 1) {
            return filter(List.of("reload"), args[0]);
        }
        return List.of();
    }
}
