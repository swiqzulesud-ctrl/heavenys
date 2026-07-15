package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /gm <0|1|2|3|survival|creative|adventure|spectator> [player]} -
 * changes the game mode of the sender or another player.
 */
public final class GamemodeCommand extends AbstractCommand {

    private static final List<String> MODE_OPTIONS = List.of(
            "0", "1", "2", "3", "survival", "creative", "adventure", "spectator");

    public GamemodeCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.gamemode");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sendUsage(sender, "/" + label + " <0|1|2|3|survival|creative|adventure|spectator> [player]");
            return;
        }

        GameMode mode = parseMode(args[0]);
        if (mode == null) {
            messages().send(sender, "commands.gamemode.invalid-mode",
                    Placeholder.unparsed("mode", args[0]));
            return;
        }

        Player target;
        if (args.length == 2) {
            if (!checkPermission(sender, "heaven.command.gamemode.others")) {
                return;
            }
            target = findPlayer(sender, args[1]);
            if (target == null) {
                return;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                return;
            }
        }

        target.setGameMode(mode);
        String modeName = mode.name().toLowerCase(Locale.ROOT);
        if (target.equals(sender)) {
            messages().send(sender, "commands.gamemode.changed-self",
                    Placeholder.unparsed("mode", modeName));
        } else {
            messages().send(sender, "commands.gamemode.changed-other",
                    Placeholder.unparsed("target", target.getName()),
                    Placeholder.unparsed("mode", modeName));
            messages().send(target, "commands.gamemode.changed-by",
                    Placeholder.unparsed("mode", modeName));
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(MODE_OPTIONS, args[0]);
        }
        if (args.length == 2 && sender.hasPermission("heaven.command.gamemode.others")) {
            return completePlayers(args[1]);
        }
        return List.of();
    }

    private GameMode parseMode(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "survival", "s" -> GameMode.SURVIVAL;
            case "1", "creative", "c" -> GameMode.CREATIVE;
            case "2", "adventure", "a" -> GameMode.ADVENTURE;
            case "3", "spectator", "sp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
