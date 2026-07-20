package com.heavenys.launcher.model;

import java.util.UUID;

/**
 * A Minecraft account known to the launcher.
 *
 * <p>Offline accounts derive a deterministic UUID from the username (the same scheme the
 * vanilla server uses for cracked players). Microsoft accounts additionally carry an access
 * token obtained through {@code MicrosoftAuth}.
 */
public class Account {
    public enum Type { OFFLINE, MICROSOFT }

    private String username;
    private String uuid;
    private Type type = Type.OFFLINE;
    private boolean remember = true;

    // Only populated for Microsoft accounts; may be null/expired.
    private String accessToken;
    private long tokenExpiryEpochSeconds;

    public Account() {
        // for Gson
    }

    private Account(String username, String uuid, Type type) {
        this.username = username;
        this.uuid = uuid;
        this.type = type;
    }

    /** Creates an offline account with a deterministic (vanilla-compatible) UUID. */
    public static Account offline(String username) {
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString();
        return new Account(username, uuid, Type.OFFLINE);
    }

    /** Creates a Microsoft account from resolved profile data. */
    public static Account microsoft(String username, String uuid, String accessToken, long expiry) {
        Account a = new Account(username, uuid, Type.MICROSOFT);
        a.accessToken = accessToken;
        a.tokenExpiryEpochSeconds = expiry;
        return a;
    }

    public String getUsername() { return username; }
    public String getUuid() { return uuid; }
    public Type getType() { return type; }
    public boolean isRemember() { return remember; }
    public void setRemember(boolean remember) { this.remember = remember; }
    public String getAccessToken() { return accessToken; }
    public long getTokenExpiryEpochSeconds() { return tokenExpiryEpochSeconds; }

    public boolean isOffline() { return type == Type.OFFLINE; }

    @Override
    public String toString() {
        return username + " (" + type + ")";
    }
}
