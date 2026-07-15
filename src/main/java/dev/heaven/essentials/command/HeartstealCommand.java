package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /heartsteal} - opens the Lifesteal admin GUI, or with the
 * {@code reload} argument reloads config.yml, messages.yml and data.yml.
 */
public final class HeartstealCommand extends AbstractCommand {

    public HeartstealCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.admin");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            messages().send(sender, "general.reloaded");
            return;
        }
        if (args.length >= 1) {
            sendUsage(sender, "/" + label + " [reload]");
            return;
        }

        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        plugin.heartsGui().open(player);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("reload"), args[0].toLowerCase(Locale.ROOT));
        }
        return List.of();
    }
}
