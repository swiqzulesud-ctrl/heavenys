package com.heavenys.launcher.ui;

import com.heavenys.launcher.core.AccountManager;
import com.heavenys.launcher.core.ConfigManager;
import com.heavenys.launcher.core.LaunchService;
import com.heavenys.launcher.core.MicrosoftAuth;
import com.heavenys.launcher.model.LauncherConfig;

import javafx.stage.Stage;

/** Shared services + hooks handed to every view (lightweight dependency injection). */
public class LauncherContext {
    public final Stage stage;
    public final ConfigManager configManager;
    public final AccountManager accountManager;
    public final LaunchService launchService;
    public final MicrosoftAuth microsoftAuth;

    /** Re-applies live launcher preferences (opacity / font / scale) to the window. */
    public Runnable applyPreferences = () -> { };

    public LauncherContext(Stage stage, ConfigManager configManager, AccountManager accountManager,
                           LaunchService launchService, MicrosoftAuth microsoftAuth) {
        this.stage = stage;
        this.configManager = configManager;
        this.accountManager = accountManager;
        this.launchService = launchService;
        this.microsoftAuth = microsoftAuth;
    }

    public LauncherConfig cfg() {
        return configManager.get();
    }

    public void save() {
        configManager.save();
    }
}
