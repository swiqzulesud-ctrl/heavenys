package dev.smplugin.util;

import dev.smplugin.SMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One-shot chat prompts: the next chat line a registered player types is
 * captured (and hidden from public chat) and handed to a callback on the
 * main thread. Typing "cancel" aborts the prompt.
 */
public final class ChatInput implements Listener {

    private final SMPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatInput(SMPlugin plugin) {
        this.plugin = plugin;
    }

    /** Waits for the player's next chat message and passes it to {@code onInput}. */
    public void await(Player player, Consumer<String> onInput) {
        pending.put(player.getUniqueId(), onInput);
    }

    public boolean isAwaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> callback = pending.remove(event.getPlayer().getUniqueId());
        if (callback == null) {
            return;
        }
        event.setCancelled(true);
        String input = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("annuler")) {
                Text.msg(event.getPlayer(), "<gray>Saisie annulée.</gray>");
                return;
            }
            callback.accept(input);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
