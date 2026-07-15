package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /god [player]} - toggles invulnerability for the sender or another
 * player. God mode is session-scoped and resets on disconnect.
 */
public final class GodCommand extends AbstractCommand {

    public GodCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.god");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            sendUsage(sender, "/" + label + " [player]");
            return;
        }

        Player target;
        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.god.others")) {
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

        boolean enabled = plugin.godService().toggle(target.getUniqueId());

        if (target.equals(sender)) {
            messages().send(sender, enabled ? "commands.god.enabled-self" : "commands.god.disabled-self");
        } else {
            messages().send(sender, enabled ? "commands.god.enabled-other" : "commands.god.disabled-other",
                    Placeholder.unparsed("target", target.getName()));
            messages().send(target, enabled ? "commands.god.enabled-by" : "commands.god.disabled-by");
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.god.others")) {
            return completePlayers(args[0]);
        }
        return List.of();
    }
}
