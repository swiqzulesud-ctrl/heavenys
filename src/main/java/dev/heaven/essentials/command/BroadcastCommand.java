package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * {@code /broadcast <message>} - announces a message to every online player
 * and the console. The message itself is parsed as MiniMessage so staff can
 * use colors and formatting.
 */
public final class BroadcastCommand extends AbstractCommand {

    public BroadcastCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.broadcast");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, "/" + label + " <message>");
            return;
        }

        String message = String.join(" ", args);
        plugin.getServer().broadcast(messages().format("commands.broadcast.format",
                Placeholder.component("message", MiniMessage.miniMessage().deserialize(message))));
    }
}
