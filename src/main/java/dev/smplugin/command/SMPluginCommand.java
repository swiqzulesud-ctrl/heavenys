package dev.smplugin.command;

import dev.smplugin.SMPlugin;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /smplugin — plugin administration (config reload).
 */
public final class SMPluginCommand implements CommandExecutor, TabCompleter {

    private final SMPlugin plugin;

    public SMPluginCommand(SMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            // Re-apply presentation with fresh config values (glow/hologram toggles).
            Bukkit.getOnlinePlayers().forEach(p -> plugin.crowns().applyPresentation(p));
            Text.msg(sender, "<white>SMPlugin <gold>config.yml</gold> reloaded.</white>");
            return true;
        }
        Text.msg(sender, "<gray>Usage: <gold>/smplugin reload</gold></gray>");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String[] args) {
        return args.length == 1 && "reload".startsWith(args[0].toLowerCase()) ? List.of("reload") : List.of();
    }
}
