package dev.smplugin.gui;

import dev.smplugin.SMPlugin;
import dev.smplugin.crowns.CrownManager;
import dev.smplugin.crowns.CrownType;
import dev.smplugin.crowns.KillTracker;
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

/**
 * The main /crowns menu: one entry per crown showing its holder, its stat and
 * how to earn it, plus a shortcut to the kill leaderboard.
 */
public final class CrownsGui extends Gui {

    private final CrownManager crowns;
    private final KillTracker kills;

    public CrownsGui(SMPlugin plugin, CrownManager crowns, KillTracker kills) {
        super(plugin, 3, "<white>Les Trois <gold>Couronnes</gold>");
        this.crowns = crowns;
        this.kills = kills;
    }

    @Override
    protected void build(Player viewer) {
        set(11, crownItem(CrownType.KILLS,
                        "<gray>Meilleur tueur : <gold>/crowns kills</gold> pour le top 10.</gray>"),
                p -> {
                    p.closeInventory();
                    p.performCommand("crowns kills");
                });
        set(13, crownItem(CrownType.RESOURCES,
                "<gray>Jugée hors-jeu ; célébrée en jeu.</gray>"));
        set(15, crownItem(CrownType.BUILDER,
                "<gray>" + buildPhaseHint() + "</gray>"));
        close(22);
    }

    private ItemStack crownItem(CrownType type, String footer) {
        List<String> lore = new ArrayList<>();
        lore.add(Text.legacy("<!italic><gray>Domaine : " + type.category() + "</gray>"));
        lore.add("");
        lore.add(Text.legacy("<!italic>" + crowns.holderLineMini(type)));
        CrownManager.Holder holder = crowns.holder(type);
        if (type == CrownType.KILLS && holder != null) {
            lore.add(Text.legacy("<!italic><gray>Éliminations JcJ : <white>"
                    + kills.kills(holder.uuid()) + "</white></gray>"));
        }
        lore.add("");
        for (String line : type.description()) {
            lore.add(Text.legacy("<!italic><white>" + line + "</white>"));
        }
        lore.add("");
        lore.add(Text.legacy("<!italic>" + footer));

        if (holder != null) {
            // Show the holder's face when the crown is claimed.
            ItemStack head = Items.gui(Material.PLAYER_HEAD, "<gold>👑 " + type.crownName() + "</gold>", lore);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(holder.uuid()));
                head.setItemMeta(skull);
            }
            return head;
        }
        return Items.gui(type.icon(), "<gold>👑 " + type.crownName() + "</gold>", lore);
    }

    private String buildPhaseHint() {
        var votes = plugin.buildVote();
        return switch (votes.phase()) {
            case WAITING -> "Candidatures dans " + Text.duration(votes.secondsUntilNextPhase()) + ".";
            case NOMINATION -> "Candidatures ouvertes — /build submit <nom> !";
            case VOTING -> "Vote en cours — /build vote !";
        };
    }
}
