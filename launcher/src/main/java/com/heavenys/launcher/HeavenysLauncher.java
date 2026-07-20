package com.heavenys.launcher;

import com.heavenys.launcher.core.AccountManager;
import com.heavenys.launcher.core.ConfigManager;
import com.heavenys.launcher.core.DiscordRichPresence;
import com.heavenys.launcher.core.LaunchService;
import com.heavenys.launcher.core.MicrosoftAuth;
import com.heavenys.launcher.ui.Animations;
import com.heavenys.launcher.ui.LauncherContext;
import com.heavenys.launcher.ui.LauncherShell;
import com.heavenys.launcher.ui.Theme;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Heavenys Launcher — premium, Heaven-themed Minecraft launcher entrypoint.
 *
 * <p>Wires the core services (config, accounts, launch, auth) into the JavaFX shell.
 */
public class HeavenysLauncher extends Application {

    @Override
    public void start(Stage stage) {
        Theme.init();

        ConfigManager configManager = new ConfigManager();
        configManager.load();
        AccountManager accountManager = new AccountManager(configManager);
        LaunchService launchService = new LaunchService();
        MicrosoftAuth microsoftAuth = new MicrosoftAuth();

        LauncherContext ctx = new LauncherContext(stage, configManager, accountManager, launchService, microsoftAuth);

        LauncherShell shell = new LauncherShell(ctx);
        ctx.applyPreferences = shell::applyPreferences;
        Parent root = shell.buildAndGetRoot();

        Scene scene = new Scene(root, 940, 620);
        scene.setFill(Color.TRANSPARENT);
        var css = getClass().getResource("/css/heaven.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Heavenys Launcher");
        stage.setScene(scene);
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon.png")));
        } catch (Exception ignored) {
        }

        shell.applyPreferences();

        DiscordRichPresence rpc = DiscordRichPresence.create(ctx.cfg().discordRichPresence);
        rpc.connect();
        rpc.update("In the launcher", "Heavenys Client");
        stage.setOnCloseRequest(e -> rpc.close());

        stage.show();
        Animations.fadeIn(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
