package dev.smplugin.command;

import dev.smplugin.SMPlugin;
import dev.smplugin.crowns.CrownType;
import dev.smplugin.crowns.KillTracker;
import dev.smplugin.gui.CrownsGui;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * /crowns — menu principal, classement des éliminations et administration
 * de la Couronne des Richesses.
 */
public final class CrownsCommand implements CommandExecutor, TabCompleter {

    private final SMPlugin plugin;

    public CrownsCommand(SMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                Text.msg(sender, "<gray>Le menu des Couronnes ne peut être ouvert qu'en jeu. "
                        + "Essayez <gold>/crowns kills</gold> depuis la console.</gray>");
                return true;
            }
            new CrownsGui(plugin, plugin.crowns(), plugin.kills()).open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "kills" -> showKillLeaderboard(sender);
            case "setresources" -> setResources(sender, args);
            case "clearresources" -> clearResources(sender);
            default -> Text.msg(sender, "<gray>Sous-commande inconnue. Usage : <gold>/crowns "
                    + "[kills|setresources <joueur>|clearresources]</gold></gray>");
        }
        return true;
    }

    private void showKillLeaderboard(CommandSender sender) {
        List<KillTracker.Entry> top = plugin.kills().top(10);
        Text.msg(sender, "<gold>─── 👑 Couronne du Tueur — Top 10 ───</gold>");
        if (top.isEmpty()) {
            Text.raw(sender, "<gray>  Aucune victoire JcJ enregistrée pour l'instant. "
                    + "La couronne attend son premier tueur...</gray>");
            return;
        }
        var holder = plugin.crowns().holder(CrownType.KILLS);
        for (int i = 0; i < top.size(); i++) {
            KillTracker.Entry entry = top.get(i);
            boolean isHolder = holder != null && holder.uuid().equals(entry.uuid());
            Text.raw(sender, "<white>  " + (i + 1) + ". "
                    + (isHolder ? "<gold>👑 " : "") + entry.name()
                    + (isHolder ? "</gold>" : "") + " <gray>—</gray> <gold>" + entry.kills()
                    + "</gold> élimination" + (entry.kills() == 1 ? "" : "s") + "</white>");
        }
    }

    private void setResources(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smplugin.crowns.admin")) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            Text.msg(sender, "<gray>Usage : <gold>/crowns setresources <joueur></gold></gray>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Text.msg(sender, "<gray>Aucun joueur nommé <white>" + args[1]
                    + "</white> n'a jamais rejoint ce serveur.</gray>");
            if (sender instanceof Player p) Fx.deny(p);
            return;
        }
        String name = target.getName() == null ? args[1] : target.getName();
        plugin.crowns().setHolder(CrownType.RESOURCES, target.getUniqueId(), name, true);
        Text.msg(sender, "<white>La <gold>Couronne des Richesses</gold> orne désormais <gold>"
                + name + "</gold>.</white>");
    }

    private void clearResources(CommandSender sender) {
        if (!sender.hasPermission("smplugin.crowns.admin")) {
            noPermission(sender);
            return;
        }
        if (plugin.crowns().holder(CrownType.RESOURCES) == null) {
            Text.msg(sender, "<gray>La Couronne des Richesses est déjà sans prétendant.</gray>");
            return;
        }
        plugin.crowns().setHolder(CrownType.RESOURCES, null, null, true);
        Text.msg(sender, "<white>La <gold>Couronne des Richesses</gold> a été retirée.</white>");
    }

    private void noPermission(CommandSender sender) {
        Text.msg(sender, "<gray>Vous n'avez pas la permission de gérer la Couronne des Richesses.</gray>");
        if (sender instanceof Player p) Fx.deny(p);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            Stream<String> subs = sender.hasPermission("smplugin.crowns.admin")
                    ? Stream.of("kills", "setresources", "clearresources")
                    : Stream.of("kills");
            return subs.filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setresources")) {
            return null; // default online-player completion
        }
        return List.of();
    }
}
