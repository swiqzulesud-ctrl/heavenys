package com.heavenys.launcher;

import com.heavenys.launcher.core.MicrosoftAuth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MicrosoftAuthTest {

    @Test
    void unavailableWithoutClientId() {
        MicrosoftAuth auth = new MicrosoftAuth("");
        assertFalse(auth.isAvailable());
        assertThrows(MicrosoftAuth.AuthUnavailableException.class, auth::requestDeviceCode);
    }

    @Test
    void availableWhenClientIdProvided() {
        assertTrue(new MicrosoftAuth("00000000-0000-0000-0000-000000000000").isAvailable());
    }
}
