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
 * /relic — contrôles administrateur de l'événement de la Relique Souveraine.
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
            Text.msg(sender, "<gray>Seuls les intendants du serveur peuvent commander le Gardien Souverain.</gray>");
            if (sender instanceof Player p) Fx.deny(p);
            return true;
        }
        if (args.length == 0) {
            Text.msg(sender, "<gold>─── ⚔ Relique Souveraine ⚔ ───</gold>");
            Text.raw(sender, "<white>  /relic summon [now] <gray>— déclencher l'événement "
                    + "(ajoutez « now » pour sauter les 10 minutes de préparation)</gray>");
            Text.raw(sender, "<white>  /relic log <gray>— les participants du dernier combat</gray>");
            Text.raw(sender, "<white>  /relic reset <gray>— lever le verrou d'unicité si la hache "
                    + "a été perdue sans que le plugin le remarque</gray>");
            Text.raw(sender, "<gray>  Statut : <white>" + plugin.relic().statusLine() + "</white></gray>");
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
                    Text.msg(sender, "<white>L'éveil a commencé — le <gold>Gardien Souverain</gold> "
                            + "apparaîtra dans <white>10 minutes</white>, quelque part sur la carte.</white>");
                } else {
                    Text.msg(sender, "<white>Le <gold>Gardien Souverain</gold> a été invoqué !</white>");
                }
            }
            case "reset" -> {
                String error = plugin.relic().adminResetRelic();
                if (error != null) {
                    Text.msg(sender, "<gray>" + error + "</gray>");
                    if (sender instanceof Player p) Fx.deny(p);
                } else {
                    Text.msg(sender, "<white>Le verrou de la relique a été levé — le "
                            + "<gold>Gardien Souverain</gold> peut être invoqué à nouveau.</white>");
                }
            }
            case "log" -> plugin.relic().lastParticipants(participants -> {
                Text.msg(sender, "<gold>─── ⚔ Dernier combat du Gardien ⚔ ───</gold>");
                if (participants.isEmpty()) {
                    Text.raw(sender, "<gray>  Aucun combat de Gardien enregistré pour l'instant.</gray>");
                    return;
                }
                for (var participant : participants) {
                    Text.raw(sender, "<white>  " + participant.name() + " <gray>—</gray> <gold>"
                            + String.format("%.1f", participant.damage()) + "</gold> dégâts infligés</white>");
                }
            });
            default -> Text.msg(sender, "<gray>Sous-commande inconnue. Usage : "
                    + "<gold>/relic <summon [now]|log|reset></gold></gray>");
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
            return Stream.of("summon", "log", "reset").filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return List.of("now");
        }
        return List.of();
    }
}
