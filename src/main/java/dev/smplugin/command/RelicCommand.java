package dev.smplugin.command;

import dev.smplugin.SMPlugin;
import dev.smplugin.relic.RelicBoss;
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
            Text.raw(sender, "<white>  /relic summon [now] [boss] <gray>— déclencher l'événement "
                    + "(« now » saute les 10 minutes de préparation ; boss au choix : "
                    + bossIds() + ", aléatoire sinon)</gray>");
            Text.raw(sender, "<white>  /relic log <gray>— les participants du dernier combat</gray>");
            Text.raw(sender, "<white>  /relic reset [boss] <gray>— lever le verrou d'unicité d'une "
                    + "relique (ou de toutes) perdue sans que le plugin le remarque</gray>");
            Text.raw(sender, "<gray>  Statut : <white>" + plugin.relic().statusLine() + "</white></gray>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "summon" -> {
                boolean now = false;
                RelicBoss boss = null;
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("now")) {
                        now = true;
                    } else {
                        boss = RelicBoss.byId(args[i]);
                        if (boss == null) {
                            Text.msg(sender, "<gray>Boss inconnu : <white>" + args[i]
                                    + "</white>. Boss disponibles : " + bossIds() + ".</gray>");
                            if (sender instanceof Player p) Fx.deny(p);
                            return true;
                        }
                    }
                }
                String error = plugin.relic().summon(now, boss);
                if (error != null) {
                    Text.msg(sender, "<gray>" + error + "</gray>");
                    if (sender instanceof Player p) Fx.deny(p);
                } else if (!now) {
                    Text.msg(sender, "<white>L'éveil a commencé — le boss apparaîtra dans "
                            + "<white>10 minutes</white>, quelque part sur la carte.</white>");
                } else {
                    Text.msg(sender, "<white>Le boss a été invoqué !</white>");
                }
            }
            case "reset" -> {
                RelicBoss boss = null;
                if (args.length > 1 && !args[1].equalsIgnoreCase("all")) {
                    boss = RelicBoss.byId(args[1]);
                    if (boss == null) {
                        Text.msg(sender, "<gray>Boss inconnu : <white>" + args[1]
                                + "</white>. Boss disponibles : " + bossIds() + " ou « all ».</gray>");
                        if (sender instanceof Player p) Fx.deny(p);
                        return true;
                    }
                }
                String error = plugin.relic().adminResetRelic(boss);
                if (error != null) {
                    Text.msg(sender, "<gray>" + error + "</gray>");
                    if (sender instanceof Player p) Fx.deny(p);
                } else {
                    Text.msg(sender, "<white>Le verrou a été levé — le boss concerné peut être "
                            + "invoqué à nouveau.</white>");
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
                    + "<gold>/relic <summon [now] [boss]|log|reset [boss|all]></gold></gray>");
        }
        return true;
    }

    private String bossIds() {
        return String.join(", ",
                java.util.Arrays.stream(RelicBoss.values()).map(RelicBoss::id).toList());
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
        if (args[0].equalsIgnoreCase("summon")) {
            Stream<String> options = Stream.concat(Stream.of("now"),
                    java.util.Arrays.stream(RelicBoss.values()).map(RelicBoss::id));
            return options.filter(s -> s.startsWith(args[args.length - 1].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            Stream<String> options = Stream.concat(Stream.of("all"),
                    java.util.Arrays.stream(RelicBoss.values()).map(RelicBoss::id));
            return options.filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
