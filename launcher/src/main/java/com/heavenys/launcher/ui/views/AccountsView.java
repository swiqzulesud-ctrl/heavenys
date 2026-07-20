package com.heavenys.launcher.ui.views;

import com.heavenys.launcher.core.MicrosoftAuth;
import com.heavenys.launcher.model.Account;
import com.heavenys.launcher.ui.Animations;
import com.heavenys.launcher.ui.LauncherContext;
import com.heavenys.launcher.ui.Ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Account management: offline + Microsoft login, multiple accounts, active selection. */
public class AccountsView {
    private final LauncherContext ctx;
    private final VBox list = new VBox(8);
    private final Label msaStatus = Ui.label("", "muted");

    public AccountsView(LauncherContext ctx) {
        this.ctx = ctx;
    }

    public Node getRoot() {
        Label title = Ui.label("Accounts", "h1");

        TextField username = new TextField();
        username.setPromptText("Offline username");
        username.getStyleClass().add("text-field");
        username.setPrefWidth(220);
        Button addOffline = new Button("Add Offline");
        addOffline.getStyleClass().add("ghost-button");
        addOffline.setOnAction(e -> {
            try {
                ctx.accountManager.addOffline(username.getText());
                username.clear();
                msaStatus.setText("");
                refresh();
            } catch (IllegalArgumentException ex) {
                msaStatus.setText("✗ " + ex.getMessage());
            }
        });

        Button addMicrosoft = new Button("Sign in with Microsoft");
        addMicrosoft.getStyleClass().add("ghost-button");
        addMicrosoft.setOnAction(e -> startMicrosoftLogin());

        CheckBox remember = new CheckBox("Remember accounts");
        remember.setSelected(ctx.cfg().rememberAccounts);
        remember.setOnAction(e -> {
            ctx.cfg().rememberAccounts = remember.isSelected();
            ctx.save();
        });

        VBox addCard = Ui.card("Add account",
                Ui.row(username, addOffline),
                Ui.row(addMicrosoft),
                msaStatus,
                remember);

        VBox listCard = Ui.card("Your accounts", list);

        refresh();
        return new VBox(14, title, addCard, listCard);
    }

    private void startMicrosoftLogin() {
        MicrosoftAuth auth = ctx.microsoftAuth;
        if (!auth.isAvailable()) {
            msaStatus.setText("⚠ Microsoft login is not configured. Set the HEAVENYS_MSA_CLIENT_ID "
                    + "environment variable to your Azure app client id to enable it.");
            return;
        }
        msaStatus.setText("Requesting device code...");
        Thread t = new Thread(() -> {
            try {
                MicrosoftAuth.DeviceCode dc = auth.requestDeviceCode();
                Platform.runLater(() -> msaStatus.setText(
                        "Go to " + dc.verificationUri() + " and enter code: " + dc.userCode()));
                Account account = auth.completeLogin(dc, s -> Platform.runLater(() -> msaStatus.setText(s)));
                Platform.runLater(() -> {
                    ctx.accountManager.addAccount(account);
                    msaStatus.setText("✓ Signed in as " + account.getUsername());
                    refresh();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> msaStatus.setText("✗ " + ex.getMessage()));
            }
        }, "heavenys-msa");
        t.setDaemon(true);
        t.start();
    }

    private void refresh() {
        list.getChildren().clear();
        String activeId = ctx.cfg().activeAccountUuid;
        if (ctx.accountManager.getAccounts().isEmpty()) {
            list.getChildren().add(Ui.label("No accounts yet — add one above.", "muted"));
        }
        for (Account a : ctx.accountManager.getAccounts()) {
            boolean active = a.getUuid().equals(activeId);
            Label name = Ui.label(a.getUsername(), active ? "gold" : "h2");
            Label badge = Ui.label(a.getType().name() + (active ? "  •  ACTIVE" : ""), "muted");
            VBox info = new VBox(1, name, badge);

            Button use = new Button(active ? "Active" : "Use");
            use.getStyleClass().add("ghost-button");
            use.setDisable(active);
            use.setOnAction(e -> { ctx.accountManager.setActive(a.getUuid()); refresh(); });

            Button remove = new Button("Remove");
            remove.getStyleClass().addAll("ghost-button", "danger-button");
            remove.setOnAction(e -> { ctx.accountManager.removeAccount(a.getUuid()); refresh(); });

            HBox rowBox = new HBox(10, info, Ui.grow(), use, remove);
            rowBox.setAlignment(Pos.CENTER_LEFT);
            rowBox.getStyleClass().add("card");
            list.getChildren().add(rowBox);
            Animations.fadeIn(rowBox);
        }
    }
}
