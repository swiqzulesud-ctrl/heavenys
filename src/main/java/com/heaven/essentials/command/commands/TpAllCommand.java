package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /tpall} - teleport every other online player to you. */
public final class TpAllCommand extends HeavenCommand {

    public TpAllCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.tpall");
    }

    @Override
    protected void execute(CommandSender sender, String[] args, String label) {
        Player self = asPlayer(sender);
        if (self == null) {
            return;
        }

        int count = 0;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.equals(self)) {
                continue;
            }
            online.teleport(self.getLocation());
            messages.send(online, "tpall.notify", Messages.ph("sender", self.getName()));
            count++;
        }
        messages.send(sender, "tpall.success", Messages.ph("count", String.valueOf(count)));
    }
}
