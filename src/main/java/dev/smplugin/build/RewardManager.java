package dev.smplugin.build;

import dev.smplugin.SMPlugin;
import dev.smplugin.data.Database;
import dev.smplugin.gui.RewardGui;
import dev.smplugin.util.Fx;
import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builder-vote rewards: tracks who still owes a reward pick, grants the five
 * reward options, and persists/applies permanent bonus hearts.
 */
public final class RewardManager implements Listener {

    private final SMPlugin plugin;
    private final Database database;

    /** Winners who have not picked their reward yet. */
    private final Set<UUID> pending = new HashSet<>();
    /** Cached bonus hearts per player (1 heart = 2 health points). */
    private final Map<UUID, Integer> bonusHearts = new HashMap<>();

    public RewardManager(SMPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    /** Loads bonus hearts and unclaimed rewards. Called on enable. */
    public void load() {
        database.querySync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, hearts FROM bonus_hearts");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bonusHearts.put(UUID.fromString(rs.getString(1)), rs.getInt(2));
                }
            }
            String csv = Database.getMeta(conn, "pending_rewards");
            if (csv != null && !csv.isBlank()) {
                for (String raw : csv.split(",")) {
                    pending.add(UUID.fromString(raw));
                }
            }
            return null;
        });
        Bukkit.getOnlinePlayers().forEach(this::applyHearts);
    }

    // ----------------------------------------------------------- pending

    public boolean hasPendingReward(UUID uuid) {
        return pending.contains(uuid);
    }

    /** Marks a vote winner as owed a reward and opens the chooser if online. */
    public void grantPendingReward(UUID uuid, String name) {
        pending.add(uuid);
        persistPending();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            Text.msg(player, "<white>You won the <gold>Builder Vote</gold>! Choose your prize...</white>");
            // Small delay so the win fanfare lands before the GUI opens.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    new RewardGui(plugin, this).open(player);
                }
            }, 60L);
        }
    }

    private void clearPending(UUID uuid) {
        pending.remove(uuid);
        persistPending();
    }

    private void persistPending() {
        String csv = pending.stream().map(UUID::toString).collect(Collectors.joining(","));
        database.runAsync(conn -> Database.setMeta(conn, "pending_rewards", csv));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyHearts(player);
        if (hasPendingReward(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    Text.msg(player, "<white>Your <gold>Builder Vote</gold> reward is waiting! "
                            + "Use <gold>/build reward</gold> to choose it.</white>");
                    Fx.fanfare(player);
                }
            }, 40L);
        }
    }

    // ----------------------------------------------------------- choosing

    /**
     * Applies the reward the winner clicked in the GUI. The player-head option
     * defers to a chat prompt; all other options complete immediately.
     */
    public void choose(Player player, RewardType type) {
        if (!hasPendingReward(player.getUniqueId())) {
            Text.msg(player, "<gray>You have no unclaimed Builder Vote reward.</gray>");
            Fx.deny(player);
            return;
        }
        switch (type) {
            case HEARTS -> chooseHearts(player);
            case BEACON -> giveItemReward(player, new ItemStack(Material.BEACON), "a <gold>Beacon</gold>");
            case END_CRYSTAL -> giveItemReward(player, new ItemStack(Material.END_CRYSTAL), "an <gold>End Crystal</gold>");
            case DRAGON_EGG -> giveItemReward(player, new ItemStack(Material.DRAGON_EGG), "a <gold>Dragon Egg</gold>");
            case PLAYER_HEAD -> chooseHead(player);
        }
    }

    private void chooseHearts(Player player) {
        int perWin = plugin.getConfig().getInt("builder-vote.rewards.hearts-per-win", 2);
        int cap = plugin.getConfig().getInt("builder-vote.rewards.max-bonus-hearts", 10);
        int current = bonusHearts.getOrDefault(player.getUniqueId(), 0);
        if (current >= cap) {
            Text.msg(player, "<gray>You've already reached the maximum of <white>" + cap
                    + "</white> bonus hearts — pick a different reward.</gray>");
            Fx.deny(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> new RewardGui(plugin, this).open(player), 20L);
            return;
        }
        int updated = Math.min(cap, current + perWin);
        bonusHearts.put(player.getUniqueId(), updated);
        UUID uuid = player.getUniqueId();
        database.runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO bonus_hearts(uuid, hearts) VALUES(?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET hearts = excluded.hearts""")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, updated);
                ps.executeUpdate();
            }
        });
        applyHearts(player);
        clearPending(player.getUniqueId());
        Text.msg(player, "<white>Your heart swells! You now have <gold>+" + updated
                + " bonus heart" + (updated == 1 ? "" : "s") + "</gold> permanently.</white>");
        Fx.success(player);
        Fx.whiteBurst(player.getLocation());
    }

    private void giveItemReward(Player player, ItemStack item, String describedAs) {
        clearPending(player.getUniqueId());
        var leftover = player.getInventory().addItem(item);
        boolean dropped = !leftover.isEmpty();
        leftover.values().forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        Text.msg(player, "<white>You received " + describedAs + " for winning the Builder Vote."
                + (dropped ? " <gray>(your inventory was full, so it was dropped at your feet)</gray>" : "") + "</white>");
        Fx.success(player);
        Fx.whiteBurst(player.getLocation());
    }

    private void chooseHead(Player player) {
        player.closeInventory();
        Text.msg(player, "<white>Type the <gold>name of the player</gold> whose head you want in chat, "
                + "or type <gray>cancel</gray> to pick a different reward.</white>");
        plugin.chatInput().await(player, input -> {
            if (!input.matches("[a-zA-Z0-9_]{1,16}")) {
                Text.msg(player, "<gray>'" + input + "' isn't a valid Minecraft name. "
                        + "Reopen the chooser with <gold>/build reward</gold>.</gray>");
                Fx.deny(player);
                return;
            }
            Text.msg(player, "<gray>Looking up <white>" + input + "</white>'s skin...</gray>");
            // Profile completion hits Mojang's API; update() runs it async.
            Bukkit.createPlayerProfile(input).update().whenComplete((profile, error) ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) {
                            return;
                        }
                        boolean found = error == null && profile != null
                                && profile.getUniqueId() != null && !profile.getTextures().isEmpty();
                        if (!found) {
                            Text.msg(player, "<gray>No player named <white>" + input + "</white> exists. "
                                    + "Reopen the chooser with <gold>/build reward</gold>.</gray>");
                            Fx.deny(player);
                            return;
                        }
                        if (!hasPendingReward(player.getUniqueId())) {
                            return; // claimed something else meanwhile
                        }
                        giveHead(player, input, profile);
                    }));
        });
    }

    private void giveHead(Player player, String name, PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            skull.setOwnerProfile(profile);
            skull.setDisplayName(Text.legacy("<!italic><white>" + name + "'s Head</white>"));
            skull.setLore(Text.legacyLore("<gray>Claimed as a <gold>Builder Vote</gold> trophy.</gray>"));
            head.setItemMeta(skull);
        }
        giveItemReward(player, head, "<gold>" + name + "'s Head</gold>");
    }

    // ------------------------------------------------------------- hearts

    public int bonusHearts(UUID uuid) {
        return bonusHearts.getOrDefault(uuid, 0);
    }

    /**
     * (Re)applies the bonus-heart attribute modifier. Any previous modifier
     * carrying our key is stripped first, so the value tracked in the
     * database is always authoritative (even across restarts, where Spigot
     * persists attribute modifiers in the player's NBT).
     */
    public void applyHearts(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        for (AttributeModifier modifier : new HashSet<>(attribute.getModifiers())) {
            if (Keys.BONUS_HEARTS.equals(modifier.getKey())) {
                attribute.removeModifier(modifier);
            }
        }
        int hearts = bonusHearts(player.getUniqueId());
        if (hearts > 0) {
            attribute.addModifier(new AttributeModifier(Keys.BONUS_HEARTS,
                    hearts * 2.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
    }
}
