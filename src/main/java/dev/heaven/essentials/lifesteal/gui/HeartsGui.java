package dev.heaven.essentials.lifesteal.gui;

import dev.heaven.essentials.HeavenEssentials;
import dev.heaven.essentials.lifesteal.HeartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The Heaven-themed Lifesteal admin GUI opened with {@code /heartsteal}.
 *
 * <p>A 27-slot inventory framed with white, light blue and cyan glass. The
 * center shows the current maximum hearts; gold and blue buttons adjust it
 * in steps of one or five. Every click saves config.yml instantly and clamps
 * players above the new limit.</p>
 */
public final class HeartsGui {

    /** Inventory slot layout. */
    public static final int SLOT_HEADER = 4;
    public static final int SLOT_DECREASE_FIVE = 10;
    public static final int SLOT_DECREASE_ONE = 11;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_INCREASE_ONE = 15;
    public static final int SLOT_INCREASE_FIVE = 16;
    public static final int SLOT_CLOSE = 22;

    private static final int SIZE = 27;

    private final HeavenEssentials plugin;

    public HeartsGui(HeavenEssentials plugin) {
        this.plugin = plugin;
    }

    /** Opens the admin GUI for the given player. */
    public void open(Player player) {
        HeartsGuiHolder holder = new HeartsGuiHolder();
        Inventory inventory = plugin.getServer().createInventory(holder, SIZE,
                plugin.messages().format("lifesteal.gui.title"));
        holder.attach(inventory);
        render(inventory);
        player.openInventory(inventory);
    }

    /** Re-renders every currently open admin GUI (after a value change). */
    public void refreshOpenViews() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder(false) instanceof HeartsGuiHolder) {
                render(top);
            }
        }
    }

    /**
     * Handles a click on a functional slot. Called by the listener with the
     * raw slot index of the click; unknown slots are ignored.
     */
    public void handleClick(Player player, int slot) {
        HeartManager hearts = plugin.heartManager();
        int delta = switch (slot) {
            case SLOT_DECREASE_FIVE -> -5;
            case SLOT_DECREASE_ONE -> -1;
            case SLOT_INCREASE_ONE -> 1;
            case SLOT_INCREASE_FIVE -> 5;
            default -> 0;
        };

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (delta == 0) {
            return;
        }

        int before = hearts.maxHearts();
        int applied = hearts.setMaxHearts(before + delta);
        if (applied != before) {
            plugin.messages().send(player, "lifesteal.max-updated",
                    Placeholder.unparsed("max", String.valueOf(applied)));
        }
        refreshOpenViews();
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void render(Inventory inventory) {
        ItemStack whitePane = pane(Material.WHITE_STAINED_GLASS_PANE);
        ItemStack bluePane = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemStack cyanPane = pane(Material.CYAN_STAINED_GLASS_PANE);

        // Frame: white corners, light blue edges, cyan accents.
        for (int slot = 0; slot < SIZE; slot++) {
            int column = slot % 9;
            if (slot < 9 || slot >= SIZE - 9) {
                inventory.setItem(slot, (column % 2 == 0) ? whitePane : bluePane);
            } else {
                inventory.setItem(slot, cyanPane);
            }
        }

        inventory.setItem(SLOT_HEADER, item(Material.NETHER_STAR,
                plugin.messages().format("lifesteal.gui.header-name"),
                plugin.messages().formatList("lifesteal.gui.header-lore")));

        int max = plugin.heartManager().maxHearts();
        inventory.setItem(SLOT_INFO, item(Material.GOLDEN_APPLE,
                plugin.messages().format("lifesteal.gui.info-name",
                        Placeholder.unparsed("max", String.valueOf(max))),
                plugin.messages().formatList("lifesteal.gui.info-lore")));

        List<Component> buttonLore = plugin.messages().formatList("lifesteal.gui.button-lore");
        inventory.setItem(SLOT_DECREASE_FIVE, item(Material.BLUE_ICE,
                plugin.messages().format("lifesteal.gui.decrease-five-name"), buttonLore));
        inventory.setItem(SLOT_DECREASE_ONE, item(Material.PACKED_ICE,
                plugin.messages().format("lifesteal.gui.decrease-one-name"), buttonLore));
        inventory.setItem(SLOT_INCREASE_ONE, item(Material.GOLD_INGOT,
                plugin.messages().format("lifesteal.gui.increase-one-name"), buttonLore));
        inventory.setItem(SLOT_INCREASE_FIVE, item(Material.GOLD_BLOCK,
                plugin.messages().format("lifesteal.gui.increase-five-name"), buttonLore));

        inventory.setItem(SLOT_CLOSE, item(Material.BARRIER,
                plugin.messages().format("lifesteal.gui.close-name"), List.of()));
    }

    private ItemStack pane(Material material) {
        return item(material, Component.empty(), List.of());
    }

    private ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.customName(name);
        if (!lore.isEmpty()) {
            meta.lore(new ArrayList<>(lore));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** Returns whether the given entity currently views this GUI. */
    public static boolean isViewing(HumanEntity entity) {
        return entity.getOpenInventory().getTopInventory().getHolder(false) instanceof HeartsGuiHolder;
    }
}
