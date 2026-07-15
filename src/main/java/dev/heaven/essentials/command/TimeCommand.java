package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Shared executor for {@code /day} and {@code /night} - sets the time of
 * the sender's world.
 */
public final class TimeCommand extends AbstractCommand {

    private final long time;
    private final String messageKey;

    /**
     * @param time       ticks to set the world time to (1000 = day, 13000 = night)
     * @param messageKey messages.yml key for the confirmation message
     * @param permission permission node of this variant
     */
    public TimeCommand(HeavenEssentials plugin, long time, String messageKey, String permission) {
        super(plugin, permission);
        this.time = time;
        this.messageKey = messageKey;
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

        World world = player.getWorld();
        world.setTime(time);
        messages().send(sender, messageKey,
                Placeholder.unparsed("world", world.getName()));
    }
}
