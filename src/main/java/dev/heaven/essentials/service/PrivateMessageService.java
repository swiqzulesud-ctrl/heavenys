package dev.heaven.essentials.service;

import dev.heaven.essentials.data.DataStore;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks private-message conversations for {@code /reply} and persistent
 * ignore lists for {@code /ignore}.
 */
public final class PrivateMessageService {

    private final DataStore dataStore;

    /** Last conversation partner per player, session-scoped. */
    private final Map<UUID, UUID> lastConversation = new ConcurrentHashMap<>();

    /** Ignore lists, cached from data.yml and written through on change. */
    private final Map<UUID, Set<UUID>> ignoreCache = new ConcurrentHashMap<>();

    public PrivateMessageService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    // ------------------------------------------------------------------
    // Conversations
    // ------------------------------------------------------------------

    /** Records that the two players just exchanged a private message. */
    public void recordConversation(UUID sender, UUID recipient) {
        lastConversation.put(sender, recipient);
        lastConversation.put(recipient, sender);
    }

    /** Returns the last conversation partner, or {@code null} if none. */
    public UUID getReplyTarget(UUID uuid) {
        return lastConversation.get(uuid);
    }

    /** Clears session state for a disconnecting player. */
    public void clearSession(UUID uuid) {
        lastConversation.remove(uuid);
        ignoreCache.remove(uuid);
    }

    // ------------------------------------------------------------------
    // Ignore lists
    // ------------------------------------------------------------------

    /** Returns whether {@code viewer} is ignoring {@code target}. */
    public boolean isIgnoring(UUID viewer, UUID target) {
        return ignores(viewer).contains(target);
    }

    /**
     * Toggles the ignore state of {@code target} for {@code viewer} and
     * persists the change immediately.
     *
     * @return the new state ({@code true} = now ignoring)
     */
    public boolean toggleIgnore(UUID viewer, UUID target) {
        Set<UUID> ignored = ignores(viewer);
        boolean nowIgnoring;
        if (ignored.contains(target)) {
            ignored.remove(target);
            nowIgnoring = false;
        } else {
            ignored.add(target);
            nowIgnoring = true;
        }
        dataStore.setIgnored(viewer, ignored);
        return nowIgnoring;
    }

    private Set<UUID> ignores(UUID viewer) {
        return ignoreCache.computeIfAbsent(viewer, dataStore::getIgnored);
    }
}
