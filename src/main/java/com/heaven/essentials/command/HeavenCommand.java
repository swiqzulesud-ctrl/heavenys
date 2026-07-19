package com.heaven.essentials.command;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Base class for every HeavenEssentials command.
 *
 * <p>Handles the boilerplate shared by all commands: permission checks, safe
 * error handling, player/target resolution and tab-completion filtering, so
 * each concrete command only implements its own behaviour.</p>
 */
public abstract class HeavenCommand implements TabExecutor {

    protected final HeavenEssentials plugin;
    protected final Messages messages;
    private final String permission;

    protected HeavenCommand(HeavenEssentials plugin, String permission) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.permission = permission;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            messages.send(sender, "general.no-permission");
            return true;
        }
        try {
            execute(sender, args, label.toLowerCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Unexpected error while running /" + label, ex);
        }
        return true;
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        List<String> completions = tab(sender, args, alias.toLowerCase(Locale.ROOT));
        return completions == null ? Collections.emptyList() : completions;
    }

    /** Executes the command. Permission has already been verified. */
    protected abstract void execute(CommandSender sender, String[] args, String label);

    /** Provides tab completions. Defaults to none. */
    protected List<String> tab(CommandSender sender, String[] args, String label) {
        return Collections.emptyList();
    }

    // ---------------------------------------------------------------------
    //  Shared helpers
    // ---------------------------------------------------------------------

    /** Returns the sender as a player, or sends the "player only" message and returns null. */
    protected Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "general.player-only");
        return null;
    }

    /** Resolves an online player by exact name, messaging the sender if not found. */
    protected Player resolve(CommandSender sender, String name) {
        Player target = plugin.getServer().getPlayerExact(name);
        if (target == null) {
            messages.send(sender, "general.unknown-player", Messages.ph("target", name));
        }
        return target;
    }

    protected void usage(CommandSender sender, String usage) {
        messages.send(sender, "general.invalid-usage", Messages.ph("usage", usage));
    }

    /** Online player names starting with the given argument (case-insensitive). */
    protected List<String> completePlayers(String arg) {
        String lower = arg.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                names.add(online.getName());
            }
        }
        Collections.sort(names);
        return names;
    }

    /** Filters a set of literal options by the given argument prefix. */
    protected List<String> filter(List<String> options, String arg) {
        String lower = arg.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
