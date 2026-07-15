package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.PrivateMessages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * {@code /reply <message>} - replies to the last player the sender exchanged
 * a private message with.
 */
public final class ReplyCommand extends AbstractCommand {

    public ReplyCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.reply");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length == 0) {
            sendUsage(sender, "/" + label + " <message>");
            return;
        }

        UUID targetId = plugin.privateMessageService().getReplyTarget(player.getUniqueId());
        Player target = targetId == null ? null : plugin.getServer().getPlayer(targetId);
        if (target == null) {
            messages().send(sender, "commands.reply.none");
            return;
        }

        PrivateMessages.deliver(plugin, player, target, String.join(" ", args));
    }
}
