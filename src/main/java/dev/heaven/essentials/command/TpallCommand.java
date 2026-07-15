package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.Teleports;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * {@code /tpall} - teleports every other online player to the sender.
 */
public final class TpallCommand extends AbstractCommand {

    public TpallCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.tpall");
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

        int count = 0;
        for (Player target : plugin.getServer().getOnlinePlayers()) {
            if (target.equals(player)) {
                continue;
            }
            Teleports.teleport(plugin.backService(), target, player.getLocation());
            messages().send(target, "commands.tpall.notify",
                    Placeholder.unparsed("player", player.getName()));
            count++;
        }

        if (count == 0) {
            messages().send(sender, "commands.tpall.nobody");
            return;
        }
        messages().send(sender, "commands.tpall.teleported",
                Placeholder.unparsed("count", String.valueOf(count)));
    }
}
