package com.heavenys.launcher;

import com.heavenys.launcher.core.AccountManager;
import com.heavenys.launcher.core.ConfigManager;
import com.heavenys.launcher.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AccountManagerTest {

    @Test
    void addsSelectsAndRemovesOfflineAccounts(@TempDir Path dir) {
        AccountManager am = new AccountManager(new ConfigManager(dir));
        Account steve = am.addOffline("Steve");

        assertEquals(1, am.getAccounts().size());
        assertTrue(am.getActiveAccount().isPresent());
        assertEquals("Steve", am.getActiveAccount().get().getUsername());
        assertEquals(Account.Type.OFFLINE, steve.getType());

        am.addOffline("Alex");
        am.setActive(steve.getUuid());
        assertEquals("Steve", am.getActiveAccount().get().getUsername());

        am.removeAccount(steve.getUuid());
        assertEquals(1, am.getAccounts().size());
        assertEquals("Alex", am.getActiveAccount().get().getUsername());
    }

    @Test
    void offlineUuidIsDeterministic(@TempDir Path dir) {
        AccountManager am = new AccountManager(new ConfigManager(dir));
        assertEquals(Account.offline("Notch").getUuid(), am.addOffline("Notch").getUuid());
    }

    @Test
    void rejectsInvalidUsernames(@TempDir Path dir) {
        AccountManager am = new AccountManager(new ConfigManager(dir));
        assertThrows(IllegalArgumentException.class, () -> am.addOffline(""));
        assertThrows(IllegalArgumentException.class, () -> am.addOffline("way_too_long_username_here"));
        assertThrows(IllegalArgumentException.class, () -> am.addOffline("bad name!"));
    }
}
