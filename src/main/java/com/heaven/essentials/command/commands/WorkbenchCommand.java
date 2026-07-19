package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /workbench} (aliases {@code /wb}, {@code /craft}) - open a crafting table. */
public final class WorkbenchCommand extends HeavenCommand {

    public WorkbenchCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.workbench");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        player.openWorkbench(null, true);
        messages.send(sender, "workbench.opening");
    }
}
