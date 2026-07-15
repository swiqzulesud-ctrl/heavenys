package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Shared executor for {@code /sun} and {@code /rain} - changes the weather
 * of the sender's world.
 */
public final class WeatherCommand extends AbstractCommand {

    private final boolean storm;
    private final String messageKey;

    /**
     * @param storm      whether the world should be set to rain ({@code true}) or clear ({@code false})
     * @param messageKey messages.yml key for the confirmation message
     * @param permission permission node of this variant
     */
    public WeatherCommand(HeavenEssentials plugin, boolean storm, String messageKey, String permission) {
        super(plugin, permission);
        this.storm = storm;
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
        world.setStorm(storm);
        world.setThundering(false);
        messages().send(sender, messageKey,
                Placeholder.unparsed("world", world.getName()));
    }
}
