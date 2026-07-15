package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /feed [player]} - restores full hunger and saturation for the
 * sender or another player.
 */
public final class FeedCommand extends AbstractCommand {

    public FeedCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.feed");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        Player target;
        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.feed.others")) {
                return;
            }
            target = findPlayer(sender, args[0]);
            if (target == null) {
                return;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                return;
            }
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0F);
        target.setExhaustion(0.0F);

        if (target.equals(sender)) {
            messages().send(sender, "commands.feed.fed-self");
        } else {
            messages().send(sender, "commands.feed.fed-other",
                    Placeholder.unparsed("target", target.getName()));
            messages().send(target, "commands.feed.fed-by");
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.feed.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
