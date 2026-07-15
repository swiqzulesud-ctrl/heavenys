package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

/**
 * {@code /workbench} - opens a portable crafting table.
 */
public final class WorkbenchCommand extends AbstractCommand {

    public WorkbenchCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.workbench");
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

        // MenuType is the modern replacement for the deprecated openWorkbench;
        // checkReachable(false) keeps the menu open without a real block.
        player.openInventory(MenuType.CRAFTING.builder()
                .checkReachable(false)
                .build(player));
        messages().send(sender, "commands.workbench.opened");
    }
}
