package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.config.Messages;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Base class for every HeavenEssentials command.
 *
 * <p>Centralizes permission checks, player-only handling, target resolution
 * and tab completion so each command implementation stays focused on its
 * actual behavior.</p>
 */
public abstract class AbstractCommand implements CommandExecutor, TabCompleter {

    protected final HeavenEssentials plugin;
    private final String permission;

    protected AbstractCommand(HeavenEssentials plugin, String permission) {
        this.plugin = plugin;
        this.permission = permission;
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                   @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission(permission)) {
            messages().send(sender, "general.no-permission");
            return true;
        }
        execute(sender, label, args);
        return true;
    }

    @Override
    public final @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                      @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission(permission)) {
            return List.of();
        }
        return complete(sender, args);
    }

    /** Executes the command; the base permission has already been checked. */
    protected abstract void execute(CommandSender sender, String label, String[] args);

    /** Provides tab completions; the base permission has already been checked. */
    protected List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    protected final Messages messages() {
        return plugin.messages();
    }

    /**
     * Casts the sender to a player, sending the players-only message and
     * returning {@code null} for console senders.
     */
    protected final Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages().send(sender, "general.players-only");
        return null;
    }

    /**
     * Checks an additional permission (e.g. the {@code .others} node),
     * sending the no-permission message when missing.
     */
    protected final boolean checkPermission(CommandSender sender, String node) {
        if (sender.hasPermission(node)) {
            return true;
        }
        messages().send(sender, "general.no-permission");
        return false;
    }

    /**
     * Resolves an online player by name, sending the player-not-found
     * message and returning {@code null} when absent.
     */
    protected final Player findPlayer(CommandSender sender, String name) {
        Player target = plugin.getServer().getPlayerExact(name);
        if (target == null) {
            messages().send(sender, "general.player-not-found",
                    Placeholder.unparsed("name", name));
        }
        return target;
    }

    /** Sends the invalid-usage message with the given usage string. */
    protected final void sendUsage(CommandSender sender, String usage) {
        messages().send(sender, "general.invalid-usage",
                Placeholder.unparsed("usage", usage));
    }

    /** Names of all online players starting with the given prefix. */
    protected final List<String> completePlayers(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted()
                .toList();
    }

    /** Filters arbitrary options by the given prefix, case-insensitively. */
    protected final List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
