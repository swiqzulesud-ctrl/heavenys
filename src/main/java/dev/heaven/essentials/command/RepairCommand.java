package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * {@code /repair [all]} - repairs the item in the sender's main hand, or
 * with {@code all} (permission {@code heaven.command.repair.all}) every
 * damageable item in their inventory, armor and off-hand.
 */
public final class RepairCommand extends AbstractCommand {

    public RepairCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.repair");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length > 1 || (args.length == 1 && !args[0].equalsIgnoreCase("all"))) {
            sendUsage(sender, "/" + label + " [all]");
            return;
        }

        if (args.length == 1) {
            if (!checkPermission(sender, "heaven.command.repair.all")) {
                return;
            }
            int repaired = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (repair(item)) {
                    repaired++;
                }
            }
            if (repaired == 0) {
                messages().send(sender, "commands.repair.nothing");
                return;
            }
            messages().send(sender, "commands.repair.repaired-all",
                    Placeholder.unparsed("count", String.valueOf(repaired)));
            return;
        }

        if (!repair(player.getInventory().getItemInMainHand())) {
            messages().send(sender, "commands.repair.nothing");
            return;
        }
        messages().send(sender, "commands.repair.repaired-hand");
    }

    /**
     * Resets the damage of the given item if it is damageable and damaged.
     *
     * @return whether the item was actually repaired
     */
    private boolean repair(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(damageable);
        return true;
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("heaven.command.repair.all")) {
            return filterPrefix(List.of("all"), args[0]);
        }
        return List.of();
    }
}
