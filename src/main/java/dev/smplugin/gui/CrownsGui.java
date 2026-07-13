package dev.smplugin.gui;

import dev.smplugin.SMPlugin;
import dev.smplugin.crowns.CrownManager;
import dev.smplugin.crowns.CrownType;
import dev.smplugin.crowns.KillTracker;
import dev.smplugin.util.Items;
import dev.smplugin.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The main /crowns menu: one entry per crown showing its holder, its stat and
 * how to earn it, plus a shortcut to the kill leaderboard.
 */
public final class CrownsGui extends Gui {

    private final CrownManager crowns;
    private final KillTracker kills;

    public CrownsGui(SMPlugin plugin, CrownManager crowns, KillTracker kills) {
        super(plugin, 3, "<white>The Three <gold>Crowns</gold>");
        this.crowns = crowns;
        this.kills = kills;
    }

    @Override
    protected void build(Player viewer) {
        set(11, crownItem(CrownType.KILLS,
                        "<gray>Top killer: see <gold>/crowns kills</gold> for the top 10.</gray>"),
                p -> {
                    p.closeInventory();
                    p.performCommand("crowns kills");
                });
        set(13, crownItem(CrownType.RESOURCES,
                "<gray>Judged off-platform; presented in-world.</gray>"));
        set(15, crownItem(CrownType.BUILDER,
                "<gray>" + buildPhaseHint() + "</gray>"));
        close(22);
    }

    private ItemStack crownItem(CrownType type, String footer) {
        List<Component> lore = new ArrayList<>();
        lore.add(Text.mm("<!italic><gray>Crown of " + type.category() + "</gray>"));
        lore.add(Component.empty());
        lore.add(crowns.holderLine(type).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        if (type == CrownType.KILLS) {
            CrownManager.Holder holder = crowns.holder(type);
            if (holder != null) {
                lore.add(Text.mm("<!italic><gray>PvP kills: <white>" + kills.kills(holder.uuid()) + "</white></gray>"));
            }
        }
        lore.add(Component.empty());
        for (String line : type.description()) {
            lore.add(Text.mm("<!italic><white>" + line + "</white>"));
        }
        lore.add(Component.empty());
        lore.add(Text.mm("<!italic>" + footer));

        CrownManager.Holder holder = crowns.holder(type);
        if (holder != null) {
            // Show the holder's face when the crown is claimed.
            ItemStack head = Items.gui(Material.PLAYER_HEAD, "<gold>👑 " + type.crownName() + "</gold>", lore);
            head.editMeta(org.bukkit.inventory.meta.SkullMeta.class,
                    meta -> meta.setOwningPlayer(Bukkit.getOfflinePlayer(holder.uuid())));
            return head;
        }
        return Items.gui(type.icon(), "<gold>👑 " + type.crownName() + "</gold>", lore);
    }

    private String buildPhaseHint() {
        var votes = plugin.buildVote();
        return switch (votes.phase()) {
            case WAITING -> "Nominations open in " + Text.duration(votes.secondsUntilNextPhase()) + ".";
            case NOMINATION -> "Nominations open now — /build submit <name>!";
            case VOTING -> "Voting open now — /build vote!";
        };
    }
}
