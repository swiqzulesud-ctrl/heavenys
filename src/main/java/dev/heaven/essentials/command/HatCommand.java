package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * {@code /hat} - moves the item in the sender's main hand onto their head,
 * swapping any current helmet back into the hand.
 */
public final class HatCommand extends AbstractCommand {

    public HatCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.hat");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length != 0) {
            sendUsage(sender, "/" + label);
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (hand.isEmpty()) {
            messages().send(sender, "commands.hat.no-item");
            return;
        }

        ItemStack helmet = inventory.getHelmet();
        inventory.setHelmet(hand);
        inventory.setItemInMainHand(helmet);
        messages().send(sender, "commands.hat.equipped");
    }
}
