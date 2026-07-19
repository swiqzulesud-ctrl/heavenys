package com.heaven.essentials.command.commands;

import com.heaven.essentials.HeavenEssentials;
import com.heaven.essentials.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Shared delivery logic for {@code /msg} and {@code /reply}. */
final class MessageDelivery {

    private MessageDelivery() {
    }

    static void send(HeavenEssentials plugin, CommandSender sender, Player target, String message) {
        Messages messages = plugin.messages();

        if (sender instanceof Player player && player.equals(target)) {
            messages.send(sender, "msg.self-error");
            return;
        }

        UUID senderId = (sender instanceof Player player) ? player.getUniqueId() : null;
        if (senderId != null && plugin.chat().isIgnoring(target.getUniqueId(), senderId)) {
            messages.send(sender, "msg.ignored", Messages.ph("target", target.getName()));
            return;
        }

        messages.send(sender, "msg.to-sender",
                Messages.ph("target", target.getName()),
                Messages.ph("message", message));
        messages.send(target, "msg.to-target",
                Messages.ph("sender", sender.getName()),
                Messages.ph("message", message));

        // Enable /reply in both directions where a UUID is available.
        if (senderId != null) {
            plugin.chat().setReplyTarget(senderId, target.getUniqueId());
            plugin.chat().setReplyTarget(target.getUniqueId(), senderId);
        }
    }
}
