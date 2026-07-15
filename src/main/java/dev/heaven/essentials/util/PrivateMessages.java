package dev.heaven.essentials.util;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/**
 * Shared delivery logic for {@code /msg} and {@code /reply}: ignore-list
 * checks, formatted delivery to both sides, and conversation tracking.
 */
public final class PrivateMessages {

    private PrivateMessages() {
    }

    /**
     * Delivers a private message from {@code sender} to {@code target},
     * unless the target is ignoring the sender.
     */
    public static void deliver(HeavenEssentials plugin, Player sender, Player target, String message) {
        if (plugin.privateMessageService().isIgnoring(target.getUniqueId(), sender.getUniqueId())) {
            plugin.messages().send(sender, "commands.msg.ignored",
                    Placeholder.unparsed("target", target.getName()));
            return;
        }

        plugin.messages().send(sender, "commands.msg.format-to",
                Placeholder.unparsed("target", target.getName()),
                Placeholder.unparsed("message", message));
        plugin.messages().send(target, "commands.msg.format-from",
                Placeholder.unparsed("player", sender.getName()),
                Placeholder.unparsed("message", message));

        plugin.privateMessageService().recordConversation(sender.getUniqueId(), target.getUniqueId());
    }
}
