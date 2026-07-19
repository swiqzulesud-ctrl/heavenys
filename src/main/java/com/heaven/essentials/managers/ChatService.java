package com.heaven.essentials.managers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backs the private-messaging commands ({@code /msg}, {@code /reply},
 * {@code /ignore}). Reply targets and ignore lists are session-scoped.
 */
public final class ChatService {

    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> ignoreLists = new ConcurrentHashMap<>();

    /** Records that {@code from} and {@code to} last exchanged a message. */
    public void setReplyTarget(UUID player, UUID target) {
        replyTargets.put(player, target);
    }

    public UUID getReplyTarget(UUID player) {
        return replyTargets.get(player);
    }

    /**
     * Toggles whether {@code player} is ignoring {@code target}.
     *
     * @return the new state ({@code true} = now ignoring)
     */
    public boolean toggleIgnore(UUID player, UUID target) {
        Set<UUID> set = ignoreLists.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet());
        if (set.contains(target)) {
            set.remove(target);
            return false;
        }
        set.add(target);
        return true;
    }

    public boolean isIgnoring(UUID player, UUID target) {
        Set<UUID> set = ignoreLists.get(player);
        return set != null && set.contains(target);
    }

    /** Removes all session state for a player who has logged out. */
    public void clear(UUID player) {
        replyTargets.remove(player);
        ignoreLists.remove(player);
        replyTargets.values().remove(player);
    }
}
