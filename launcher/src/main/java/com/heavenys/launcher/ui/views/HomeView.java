package com.heavenys.launcher.ui.views;

import com.heavenys.launcher.model.Account;
import com.heavenys.launcher.model.LauncherConfig;
import com.heavenys.launcher.ui.LauncherContext;
import com.heavenys.launcher.ui.Ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/** Home screen: news / changelog, launch summary and the primary Launch action. */
public class HomeView {
    private final LauncherContext ctx;
    private final Label statusLabel = Ui.label("Ready.", "muted");
    private final ProgressBar progress = new ProgressBar(0);
    private final TextArea console = new TextArea();
    private Button launchButton;

    public HomeView(LauncherContext ctx) {
        this.ctx = ctx;
    }

    public Node getRoot() {
        LauncherConfig cfg = ctx.cfg();

        Label title = Ui.label("Heavenys Client", "h1");
        Label version = Ui.label("v" + cfg.selectedVersion + "  ·  Competitive PvP", "gold");
        VBox header = new VBox(2, title, version);

        VBox news = Ui.card("Changelog / News",
                Ui.label("• Heaven-themed launcher: accounts, RAM, resolution, fonts & opacity.", "muted"),
                Ui.label("• Modular in-game HUD (FPS, CPS, Ping, Armor, Keystrokes, and more).", "muted"),
                Ui.label("• Sodium / Lithium / FerriteCore optimization stack + Simple Voice Chat.", "muted"),
                Ui.label("• 100% original, license-respecting — no proprietary client code or assets.", "muted"));

        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setVisible(false);
        progress.setManaged(false);

        console.setEditable(false);
        console.getStyleClass().add("mono");
        console.setPrefRowCount(7);
        VBox.setVgrow(console, Priority.ALWAYS);

        launchButton = new Button("LAUNCH");
        launchButton.getStyleClass().add("primary-button");
        launchButton.setOnAction(e -> onLaunch());

        Label ramInfo = Ui.label(cfg.minRamMb + "–" + cfg.maxRamMb + " MB", "muted");
        Optional<Account> active = ctx.accountManager.getActiveAccount();
        Label accInfo = Ui.label(active.map(Account::getUsername).orElse("No account"),
                active.isPresent() ? "gold" : "muted");

        HBox launchRow = Ui.row(
                new VBox(2, Ui.label("Account", "muted"), accInfo),
                spacer(24),
                new VBox(2, Ui.label("Memory", "muted"), ramInfo),
                Ui.grow(),
                launchButton);
        launchRow.setAlignment(Pos.CENTER_LEFT);

        VBox launchCard = Ui.card("Launch", launchRow, progress, statusLabel, console);
        VBox.setVgrow(launchCard, Priority.ALWAYS);

        VBox root = new VBox(14, header, news, launchCard);
        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

    private Node spacer(double w) {
        Label l = new Label();
        l.setMinWidth(w);
        return l;
    }

    private void onLaunch() {
        console.clear();
        Optional<Account> active = ctx.accountManager.getActiveAccount();
        LauncherConfig cfg = ctx.cfg();

        List<String> problems = ctx.launchService.validate(cfg, active.orElse(null));
        if (!problems.isEmpty()) {
            statusLabel.setText("Cannot launch — resolve the issues below.");
            problems.forEach(p -> console.appendText("✗ " + p + "\n"));
            return;
        }

        List<String> cmd = ctx.launchService.buildLaunchCommand(cfg, active.get());
        console.appendText("Launch command:\n  " + String.join(" \\\n  ", cmd) + "\n\n");
        console.appendText("Verifying Java runtime & memory arguments (dry-run)...\n");

        launchButton.setDisable(true);
        progress.setVisible(true);
        progress.setManaged(true);
        progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Launching as " + active.get().getUsername() + "...");

        Thread t = new Thread(() -> {
            String output;
            boolean ok;
            try {
                output = ctx.launchService.runDryRun(cfg);
                ok = true;
            } catch (Exception ex) {
                output = "Dry-run failed: " + ex.getMessage();
                ok = false;
            }
            final String out = output;
            final boolean success = ok;
            Platform.runLater(() -> {
                console.appendText(out + "\n");
                progress.setProgress(success ? 1.0 : 0);
                statusLabel.setText(success
                        ? "Runtime verified ✓  (full game bootstrap is handled by the launcher-core layer)"
                        : "Launch verification failed.");
                launchButton.setDisable(false);
            });
        }, "heavenys-launch");
        t.setDaemon(true);
        t.start();
    }
}
