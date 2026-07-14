package dev.smplugin.command;

import dev.smplugin.SMPlugin;
import dev.smplugin.build.BuildVoteManager;
import dev.smplugin.gui.RewardGui;
import dev.smplugin.gui.VoteGui;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/**
 * /build — soumettre des constructions, voter, consulter les résultats,
 * réclamer sa récompense, et (admin) piloter le concours à la demande.
 */
public final class BuildCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "smplugin.build.admin";

    private final SMPlugin plugin;

    public BuildCommand(SMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "submit" -> submit(sender, args);
            case "vote" -> vote(sender);
            case "results" -> results(sender);
            case "reward" -> reward(sender);
            case "startnominations", "start" -> adminAction(sender, () -> {
                String error = plugin.buildVote().forceNominations();
                if (error != null) {
                    deny(sender, error);
                } else {
                    Text.msg(sender, "<white>Les candidatures du <gold>Vote de Construction</gold> "
                            + "sont désormais ouvertes.</white>");
                }
            });
            case "startvote" -> adminAction(sender, () -> {
                String error = plugin.buildVote().forceVote();
                if (error != null) {
                    deny(sender, error);
                } else {
                    Text.msg(sender, "<white>La phase de <gold>vote</gold> est désormais ouverte.</white>");
                }
            });
            case "finish" -> adminAction(sender, () -> {
                String error = plugin.buildVote().forceFinish();
                if (error != null) {
                    deny(sender, error);
                } else {
                    Text.msg(sender, "<white>Le vote a été clôturé et les résultats annoncés.</white>");
                }
            });
            case "cancel" -> adminAction(sender, () -> {
                plugin.buildVote().cancelCycle();
                Text.msg(sender, "<white>Le cycle en cours a été annulé ; un nouveau cycle démarre.</white>");
            });
            default -> sendHelp(sender);
        }
        return true;
    }

    private void adminAction(CommandSender sender, Runnable action) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            deny(sender, "Vous n'avez pas la permission de piloter le Vote de Construction.");
            return;
        }
        action.run();
    }

    private void deny(CommandSender sender, String message) {
        Text.msg(sender, "<gray>" + message + "</gray>");
        if (sender instanceof Player p) {
            Fx.deny(p);
        }
    }

    private void sendHelp(CommandSender sender) {
        BuildVoteManager votes = plugin.buildVote();
        Text.msg(sender, "<gold>─── ✦ Vote de Construction ✦ ───</gold>");
        Text.raw(sender, "<white>  /build submit <nom> <gray>— inscrire votre construction (placez-vous devant)</gray>");
        Text.raw(sender, "<white>  /build vote <gray>— ouvrir le menu de vote</gray>");
        Text.raw(sender, "<white>  /build results <gray>— le gagnant du dernier cycle</gray>");
        Text.raw(sender, "<white>  /build reward <gray>— réclamer une récompense en attente</gray>");
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            Text.raw(sender, "<white>  /build startnominations <gray>— (admin) ouvrir les candidatures maintenant</gray>");
            Text.raw(sender, "<white>  /build startvote <gray>— (admin) ouvrir le vote maintenant</gray>");
            Text.raw(sender, "<white>  /build finish <gray>— (admin) clôturer le vote et annoncer les résultats</gray>");
            Text.raw(sender, "<white>  /build cancel <gray>— (admin) annuler le cycle en cours</gray>");
        }
        String phase = switch (votes.phase()) {
            case WAITING -> "Candidatures dans <white>" + Text.duration(votes.secondsUntilNextPhase()) + "</white>.";
            case NOMINATION -> "Candidatures <white>ouvertes</white> pendant encore <white>"
                    + Text.duration(votes.secondsUntilNextPhase()) + "</white> !";
            case VOTING -> "Vote <white>en cours</white> pendant encore <white>"
                    + Text.duration(votes.secondsUntilNextPhase()) + "</white> !";
        };
        Text.raw(sender, "<gray>  Phase actuelle : " + phase + "</gray>");
    }

    private void submit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<gray>Les constructions ne peuvent être inscrites qu'en jeu, "
                    + "près de la construction.</gray>");
            return;
        }
        if (args.length < 2) {
            Text.msg(player, "<gray>Usage : <gold>/build submit <nom></gold> — placez-vous d'abord "
                    + "près de votre construction !</gray>");
            Fx.deny(player);
            return;
        }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        plugin.buildVote().submit(player, name);
    }

    private void vote(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<gray>Le vote se déroule en jeu, via le menu de vote.</gray>");
            return;
        }
        BuildVoteManager votes = plugin.buildVote();
        if (votes.phase() != BuildVoteManager.Phase.VOTING) {
            if (votes.phase() == BuildVoteManager.Phase.NOMINATION) {
                Text.msg(player, "<gray>Le vote n'a pas encore commencé — les candidatures restent ouvertes "
                        + "pendant <white>" + Text.duration(votes.secondsUntilNextPhase()) + "</white>. "
                        + "Inscrivez la vôtre avec <gold>/build submit <nom></gold> !</gray>");
            } else {
                Text.msg(player, "<gray>Aucun vote en cours pour le moment. Le cycle avance dans <white>"
                        + Text.duration(votes.secondsUntilNextPhase()) + "</white>.</gray>");
            }
            Fx.deny(player);
            return;
        }
        if (votes.submissions().isEmpty()) {
            Text.msg(player, "<gray>Aucune construction n'a été proposée ce cycle — il n'y a rien à voter.</gray>");
            Fx.deny(player);
            return;
        }
        new VoteGui(plugin, votes, 0).open(player);
    }

    private void results(CommandSender sender) {
        plugin.buildVote().lastResult(result -> {
            if (result == null) {
                Text.msg(sender, "<gray>Aucun vote de construction ne s'est encore achevé — l'histoire "
                        + "commence avec le premier gagnant !</gray>");
                return;
            }
            String when = DateTimeFormatter.ofPattern("d MMM yyyy")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(result.finished()));
            Text.msg(sender, "<gold>─── ✦ Dernier Vote de Construction ✦ ───</gold>");
            Text.raw(sender, "<white>  Gagnant : <gold>" + result.winnerName() + "</gold>");
            Text.raw(sender, "<white>  Construction : <gold>" + result.buildName() + "</gold>");
            Text.raw(sender, "<white>  Voix : <gold>" + result.votes() + "</gold> <gray>(" + when + ")</gray>");
        });
    }

    private void reward(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<gray>Les récompenses ne peuvent être réclamées qu'en jeu.</gray>");
            return;
        }
        if (!plugin.rewards().hasPendingReward(player.getUniqueId())) {
            Text.msg(player, "<gray>Vous n'avez aucune récompense à réclamer. "
                    + "Remportez le prochain vote !</gray>");
            Fx.deny(player);
            return;
        }
        new RewardGui(plugin, plugin.rewards()).open(player);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            Stream<String> subs = sender.hasPermission(ADMIN_PERMISSION)
                    ? Stream.of("submit", "vote", "results", "reward",
                            "startnominations", "startvote", "finish", "cancel")
                    : Stream.of("submit", "vote", "results", "reward");
            return subs.filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
