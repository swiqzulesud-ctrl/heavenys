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
 * /build — submit builds, open the vote menu, view results, claim rewards.
 */
public final class BuildCommand implements CommandExecutor, TabCompleter {

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
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        BuildVoteManager votes = plugin.buildVote();
        Text.msg(sender, "<gold>─── ✦ Builder Vote ✦ ───</gold>");
        sender.sendMessage(Text.mm("<white>  /build submit <name> <gray>— enter your build (stand next to it)</gray>"));
        sender.sendMessage(Text.mm("<white>  /build vote <gray>— open the voting menu</gray>"));
        sender.sendMessage(Text.mm("<white>  /build results <gray>— last cycle's winner</gray>"));
        sender.sendMessage(Text.mm("<white>  /build reward <gray>— claim an unclaimed win reward</gray>"));
        String phase = switch (votes.phase()) {
            case WAITING -> "Nominations open in <white>" + Text.duration(votes.secondsUntilNextPhase()) + "</white>.";
            case NOMINATION -> "Nominations are <white>open now</white> for another <white>"
                    + Text.duration(votes.secondsUntilNextPhase()) + "</white>!";
            case VOTING -> "Voting is <white>open now</white> for another <white>"
                    + Text.duration(votes.secondsUntilNextPhase()) + "</white>!";
        };
        sender.sendMessage(Text.mm("<gray>  Current phase: " + phase + "</gray>"));
    }

    private void submit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<gray>Builds can only be submitted in-game, standing near the build.</gray>");
            return;
        }
        if (args.length < 2) {
            Text.msg(player, "<gray>Usage: <gold>/build submit <name></gold> — stand near your build first!</gray>");
            Fx.deny(player);
            return;
        }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        plugin.buildVote().submit(player, name);
    }

    private void vote(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<gray>Voting happens in-game through the vote menu.</gray>");
            return;
        }
        BuildVoteManager votes = plugin.buildVote();
        if (votes.phase() != BuildVoteManager.Phase.VOTING) {
            if (votes.phase() == BuildVoteManager.Phase.NOMINATION) {
                Text.msg(player, "<gray>Voting hasn't started yet — nominations are still open for <white>"
                        + Text.duration(votes.secondsUntilNextPhase()) + "</white>. "
                        + "Submit yours with <gold>/build submit <name></gold>!</gray>");
            } else {
                Text.msg(player, "<gray>There's no vote running right now. The next cycle progresses in <white>"
                        + Text.duration(votes.secondsUntilNextPhase()) + "</white>.</gray>");
            }
            Fx.deny(player);
            return;
        }
        if (votes.submissions().isEmpty()) {
            Text.msg(player, "<gray>No builds were submitted this cycle — there's nothing to vote on.</gray>");
            Fx.deny(player);
            return;
        }
        new VoteGui(plugin, votes, 0).open(player);
    }

    private void results(CommandSender sender) {
        plugin.buildVote().lastResult(result -> {
            if (result == null) {
                Text.msg(sender, "<gray>No builder vote has finished yet — history starts with the first winner!</gray>");
                return;
            }
            String when = DateTimeFormatter.ofPattern("MMM d, yyyy")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(result.finished()));
            Text.msg(sender, "<gold>─── ✦ Last Builder Vote ✦ ───</gold>");
            sender.sendMessage(Text.mm("<white>  Winner: <gold>" + result.winnerName() + "</gold>"));
            sender.sendMessage(Text.mm("<white>  Build: <gold>" + result.buildName() + "</gold>"));
            sender.sendMessage(Text.mm("<white>  Votes: <gold>" + result.votes() + "</gold> <gray>(" + when + ")</gray>"));
        });
    }

    private void reward(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Text.msg(sender, "<gray>Rewards can only be claimed in-game.</gray>");
            return;
        }
        if (!plugin.rewards().hasPendingReward(player.getUniqueId())) {
            Text.msg(player, "<gray>You have no unclaimed Builder Vote reward. Win the next vote!</gray>");
            Fx.deny(player);
            return;
        }
        new RewardGui(plugin, plugin.rewards()).open(player);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("submit", "vote", "results", "reward")
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
