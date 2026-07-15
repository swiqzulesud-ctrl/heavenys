package dev.heaven.essentials.command;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /speed <1-10> [player]} - sets the walk or fly speed of the sender
 * or another player. The affected speed type follows the target's current
 * movement state: fly speed while flying, walk speed otherwise.
 */
public final class SpeedCommand extends AbstractCommand {

    private static final List<String> SPEED_OPTIONS =
            List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    public SpeedCommand(HeavenEssentials plugin) {
        super(plugin, "heaven.command.speed");
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1 || args.length > 2) {
            sendUsage(sender, "/" + label + " <1-10> [player]");
            return;
        }

        int speed;
        try {
            speed = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            messages().send(sender, "general.invalid-number",
                    Placeholder.unparsed("input", args[0]));
            return;
        }
        if (speed < 1 || speed > 10) {
            messages().send(sender, "commands.speed.invalid");
            return;
        }

        Player target;
        if (args.length == 2) {
            if (!checkPermission(sender, "heaven.command.speed.others")) {
                return;
            }
            target = findPlayer(sender, args[1]);
            if (target == null) {
                return;
            }
        } else {
            target = requirePlayer(sender);
            if (target == null) {
                return;
            }
        }

        boolean flying = target.isFlying();
        // Map 1-10 onto Bukkit's 0.1-1.0 range.
        float value = speed / 10.0F;
        if (flying) {
            target.setFlySpeed(value);
        } else {
            target.setWalkSpeed(value);
        }

        String typeKey = flying ? "commands.speed.type-fly" : "commands.speed.type-walk";
        String type = messages().raw(typeKey);
        if (target.equals(sender)) {
            messages().send(sender, "commands.speed.set-self",
                    Placeholder.unparsed("type", type),
                    Placeholder.unparsed("speed", String.valueOf(speed)));
        } else {
            messages().send(sender, "commands.speed.set-other",
                    Placeholder.unparsed("target", target.getName()),
                    Placeholder.unparsed("type", type),
                    Placeholder.unparsed("speed", String.valueOf(speed)));
            messages().send(target, "commands.speed.set-by",
                    Placeholder.unparsed("type", type),
                    Placeholder.unparsed("speed", String.valueOf(speed)));
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterPrefix(SPEED_OPTIONS, args[0]);
        }
        if (args.length == 2 && sender.hasPermission("heaven.command.speed.others")) {
            return completePlayers(args[1]);
        }
        return List.of();
    }
}
