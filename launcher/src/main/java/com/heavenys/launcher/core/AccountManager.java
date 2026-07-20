package com.heavenys.launcher.core;

import com.heavenys.launcher.model.Account;
import com.heavenys.launcher.model.LauncherConfig;

import java.util.List;
import java.util.Optional;

/**
 * Manages the launcher's known accounts (add/remove/select), persisting through
 * {@link ConfigManager}. Supports multiple accounts and a single "active" account.
 */
public class AccountManager {
    private final ConfigManager configManager;

    public AccountManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    private LauncherConfig cfg() {
        return configManager.get();
    }

    public List<Account> getAccounts() {
        return cfg().accounts;
    }

    public Optional<Account> getActiveAccount() {
        String id = cfg().activeAccountUuid;
        if (id == null) {
            return Optional.empty();
        }
        return cfg().accounts.stream().filter(a -> id.equals(a.getUuid())).findFirst();
    }

    /** Adds (or replaces by UUID) an account and makes it active. */
    public Account addAccount(Account account) {
        cfg().accounts.removeIf(a -> a.getUuid().equals(account.getUuid()));
        cfg().accounts.add(account);
        cfg().activeAccountUuid = account.getUuid();
        configManager.save();
        return account;
    }

    public Account addOffline(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        if (username.length() > 16 || !username.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid Minecraft username (1-16 letters/digits/underscore)");
        }
        return addAccount(Account.offline(username.trim()));
    }

    public void setActive(String uuid) {
        cfg().accounts.stream().filter(a -> a.getUuid().equals(uuid)).findFirst()
                .ifPresent(a -> {
                    cfg().activeAccountUuid = uuid;
                    configManager.save();
                });
    }

    public void removeAccount(String uuid) {
        cfg().accounts.removeIf(a -> a.getUuid().equals(uuid));
        if (uuid.equals(cfg().activeAccountUuid)) {
            cfg().activeAccountUuid = cfg().accounts.isEmpty() ? null : cfg().accounts.get(0).getUuid();
        }
        configManager.save();
    }
}
