package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.util.PrivateMessages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * {@code /msg <player> <message>} - sends a private message to another
 * player, respecting ignore lists and recording the conversation for
 * {@code /reply}.
 */
public final class MsgCommand extends AbstractCommand {

    public MsgCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.msg");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sendUsage(sender, "/" + label + " <player> <message>");
            return;
        }

        Player target = findPlayer(sender, args[0]);
        if (target == null) {
            return;
        }
        if (target.equals(player)) {
            messages().send(sender, "commands.msg.cannot-self");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        PrivateMessages.deliver(plugin, player, target, message);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
