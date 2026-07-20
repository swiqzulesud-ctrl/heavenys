package com.heavenys.launcher.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discord Rich Presence integration point.
 *
 * <p>Shipped as a no-op by default so the launcher never hard-depends on a Discord client being
 * present. A real implementation (via the Discord IPC named pipe / discord-game-sdk) can be
 * dropped in behind this interface without touching callers.
 */
public interface DiscordRichPresence {
    void connect();
    void update(String state, String details);
    void close();

    static DiscordRichPresence create(boolean enabled) {
        return enabled ? new NoOp() : new Disabled();
    }

    class NoOp implements DiscordRichPresence {
        private static final Logger LOG = LoggerFactory.getLogger("Heavenys/Discord");
        @Override public void connect() { LOG.info("Discord RPC ready (no-op; connect a real IPC backend to enable)."); }
        @Override public void update(String state, String details) { LOG.debug("RPC: {} — {}", state, details); }
        @Override public void close() { }
    }

    class Disabled implements DiscordRichPresence {
        @Override public void connect() { }
        @Override public void update(String state, String details) { }
        @Override public void close() { }
    }
}
