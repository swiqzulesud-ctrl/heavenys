package dev.smplugin.gui;

import dev.smplugin.SMPlugin;
import dev.smplugin.build.RewardManager;
import dev.smplugin.build.RewardType;
import dev.smplugin.util.Items;
import net.kyori.adventure.text.Component;
import dev.smplugin.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The winner-only "Choose Your Reward" menu shown after a builder-vote win.
 */
public final class RewardGui extends Gui {

    private static final int[] SLOTS = {10, 12, 13, 14, 16};

    private final RewardManager rewards;

    public RewardGui(SMPlugin plugin, RewardManager rewards) {
        super(plugin, 3, "<white>Choose Your <gold>Reward</gold>");
        this.rewards = rewards;
    }

    @Override
    protected void build(Player viewer) {
        RewardType[] types = RewardType.values();
        for (int i = 0; i < types.length; i++) {
            RewardType type = types[i];
            set(SLOTS[i], rewardItem(viewer, type), p -> {
                p.closeInventory();
                rewards.choose(p, type);
            });
        }
        close(22);
    }

    private org.bukkit.inventory.ItemStack rewardItem(Player viewer, RewardType type) {
        List<Component> lore = new ArrayList<>();
        for (String line : type.description()) {
            lore.add(Text.mm("<!italic><gray>" + line + "</gray>"));
        }
        lore.add(Component.empty());
        if (type == RewardType.HEARTS) {
            int current = rewards.bonusHearts(viewer.getUniqueId());
            int cap = plugin.getConfig().getInt("builder-vote.rewards.max-bonus-hearts", 10);
            lore.add(Text.mm("<!italic><gray>Your bonus hearts: <white>" + current + "</white>/<white>" + cap + "</white></gray>"));
            lore.add(Component.empty());
        }
        lore.add(Text.mm("<!italic><white>Click to claim — one reward per win!</white>"));
        Material icon = type.icon();
        return Items.gui(icon, "<gold>✦ " + type.displayName() + "</gold>", lore);
    }
}
