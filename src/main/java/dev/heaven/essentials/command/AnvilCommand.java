package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

/**
 * {@code /anvil} - opens a portable anvil.
 */
public final class AnvilCommand extends AbstractCommand {

    public AnvilCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.anvil");
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

        // MenuType is the modern replacement for the deprecated openAnvil;
        // checkReachable(false) keeps the menu open without a real block.
        player.openInventory(MenuType.ANVIL.builder()
                .checkReachable(false)
                .build(player));
        messages().send(sender, "commands.anvil.opened");
    }
}
