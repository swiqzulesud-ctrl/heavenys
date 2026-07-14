package dev.smplugin.gui;

import dev.smplugin.SMPlugin;
import dev.smplugin.build.BuildVoteManager;
import dev.smplugin.util.Items;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Paginated builder-vote menu: each submission is shown as the builder's head
 * with the build name; clicking casts (or changes) the viewer's vote.
 */
public final class VoteGui extends Gui {

    /** Inner 4x7 grid used for entries (border stays glass). */
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    private final BuildVoteManager votes;
    private final int page;

    public VoteGui(SMPlugin plugin, BuildVoteManager votes, int page) {
        super(plugin, 6, "<white>Builder Vote <gray>— Cast Your Vote</gray>");
        this.votes = votes;
        this.page = page;
    }

    @Override
    protected void build(Player viewer) {
        List<BuildVoteManager.Submission> entries = votes.submissions();
        int perPage = CONTENT_SLOTS.length;
        int pages = Math.max(1, (entries.size() + perPage - 1) / perPage);
        int current = Math.min(page, pages - 1);

        UUID viewerVote = votes.votedFor(viewer.getUniqueId());
        for (int i = 0; i < perPage; i++) {
            int index = current * perPage + i;
            if (index >= entries.size()) {
                break;
            }
            BuildVoteManager.Submission entry = entries.get(index);
            boolean votedForThis = entry.uuid().equals(viewerVote);
            boolean own = entry.uuid().equals(viewer.getUniqueId());
            set(CONTENT_SLOTS[i], entryItem(entry, votedForThis, own),
                    p -> {
                        if (votes.vote(p, entry.uuid())) {
                            new VoteGui(plugin, votes, current).open(p); // refresh
                        }
                    });
        }

        // Info banner (white banner = the vote's standard).
        set(4, Items.gui(Material.WHITE_BANNER, "<gold>✦ Builder Vote ✦</gold>",
                "<white>" + entries.size() + " build" + (entries.size() == 1 ? "" : "s") + " compete this cycle.</white>",
                "<gray>Voting closes in <white>" + Text.duration(votes.secondsUntilNextPhase()) + "</white>.</gray>",
                "",
                "<gray>Click a build to cast your vote.</gray>",
                "<gray>You cannot vote for your own build.</gray>"));

        if (current > 0) {
            set(48, Items.gui(Material.ARROW, "<white>← Previous Page"),
                    p -> new VoteGui(plugin, votes, current - 1).open(p));
        }
        if (current < pages - 1) {
            set(50, Items.gui(Material.ARROW, "<white>Next Page →"),
                    p -> new VoteGui(plugin, votes, current + 1).open(p));
        }
        close(49);
    }

    private ItemStack entryItem(BuildVoteManager.Submission entry, boolean votedForThis, boolean own) {
        List<String> lore = new ArrayList<>();
        lore.add(Text.legacy("<!italic><gray>by <white>" + entry.playerName() + "</white></gray>"));
        lore.add("");
        lore.add(Text.legacy("<!italic><gray>Showcase: <white>" + (int) entry.x() + ", "
                + (int) entry.y() + ", " + (int) entry.z() + "</white> <gray>(" + entry.world() + ")</gray>"));
        lore.add("");
        if (own) {
            lore.add(Text.legacy("<!italic><gray>This is your build — you can't vote for it.</gray>"));
        } else if (votedForThis) {
            lore.add(Text.legacy("<!italic><gold>✔ Your current vote.</gold>"));
        } else {
            lore.add(Text.legacy("<!italic><white>Click to vote for this build!</white>"));
        }

        ItemStack head = Items.gui(Material.PLAYER_HEAD,
                "<gold>" + entry.buildName() + "</gold>", lore);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
            if (votedForThis) {
                skull.setEnchantmentGlintOverride(true);
            }
            head.setItemMeta(skull);
        }
        return head;
    }
}
