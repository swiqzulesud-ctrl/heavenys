package dev.smplugin.command;

import dev.smplugin.SMPlugin;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * /relic — admin controls for the Sovereign's Relic event.
 */
public final class RelicCommand implements CommandExecutor, TabCompleter {

    private final SMPlugin plugin;

    public RelicCommand(SMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!sender.hasPermission("smplugin.relic.admin")) {
            Text.msg(sender, "<gray>Only the server's stewards may command the Sovereign Guardian.</gray>");
            if (sender instanceof Player p) Fx.deny(p);
            return true;
        }
        if (args.length == 0) {
            Text.msg(sender, "<gold>─── ⚔ Sovereign's Relic ⚔ ───</gold>");
            Text.raw(sender, ("<white>  /relic summon [now] <gray>— trigger the event "
                    + "(add 'now' to skip the 10-minute build-up)</gray>"));
            Text.raw(sender, ("<white>  /relic log <gray>— last event's participants</gray>"));
            Text.raw(sender, ("<gray>  Status: <white>" + plugin.relic().statusLine() + "</white></gray>"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "summon" -> {
                boolean now = args.length > 1 && args[1].equalsIgnoreCase("now");
                String error = plugin.relic().summon(now);
                if (error != null) {
                    Text.msg(sender, "<gray>" + error + "</gray>");
                    if (sender instanceof Player p) Fx.deny(p);
                } else if (!now) {
                    Text.msg(sender, "<white>The build-up has begun — the <gold>Sovereign Guardian</gold> "
                            + "arrives in <white>10 minutes</white>.</white>");
                } else {
                    Text.msg(sender, "<white>The <gold>Sovereign Guardian</gold> has been summoned!</white>");
                }
            }
            case "log" -> plugin.relic().lastParticipants(participants -> {
                Text.msg(sender, "<gold>─── ⚔ Last Guardian Fight ⚔ ───</gold>");
                if (participants.isEmpty()) {
                    Text.raw(sender, ("<gray>  No Guardian fight has been recorded yet.</gray>"));
                    return;
                }
                for (var participant : participants) {
                    Text.raw(sender, ("<white>  " + participant.name() + " <gray>—</gray> <gold>"
                            + String.format("%.1f", participant.damage()) + "</gold> damage dealt</white>"));
                }
            });
            default -> Text.msg(sender, "<gray>Unknown subcommand. Usage: <gold>/relic <summon [now]|log></gold></gray>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String[] args) {
        if (!sender.hasPermission("smplugin.relic.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return Stream.of("summon", "log").filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return List.of("now");
        }
        return List.of();
    }
}
