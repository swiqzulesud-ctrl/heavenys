package com.heavenys.launcher.ui;

import com.heavenys.launcher.ui.views.AccountsView;
import com.heavenys.launcher.ui.views.HomeView;
import com.heavenys.launcher.ui.views.OptimizationView;
import com.heavenys.launcher.ui.views.SettingsView;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Root window: title bar (draggable), sidebar navigation and the animated content area. */
public class LauncherShell {
    private final LauncherContext ctx;
    private final StackPane contentArea = new StackPane();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private VBox root;
    private double dragX;
    private double dragY;

    public LauncherShell(LauncherContext ctx) {
        this.ctx = ctx;
    }

    public Parent buildAndGetRoot() {
        root = new VBox();
        root.getStyleClass().add("app-window");
        root.setPadding(new Insets(6, 10, 10, 10));

        root.getChildren().add(titleBar());

        HBox body = new HBox(12, sidebar(), contentArea);
        body.setPadding(new Insets(6, 4, 4, 4));
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        root.getChildren().add(body);

        select("Home", () -> new HomeView(ctx).getRoot());
        return root;
    }

    private HBox titleBar() {
        ImageView logo = new ImageView();
        try {
            logo.setImage(new Image(getClass().getResourceAsStream("/assets/logo.png")));
            logo.setFitWidth(24);
            logo.setFitHeight(24);
        } catch (Exception ignored) {
        }
        VBox titleText = new VBox(-2,
                Ui.label("Heavenys", "title-text"),
                Ui.label("LAUNCHER", "title-sub"));

        Button min = new Button("—");
        min.getStyleClass().add("win-button");
        min.setOnAction(e -> ctx.stage.setIconified(true));
        Button close = new Button("✕");
        close.getStyleClass().addAll("win-button", "win-close");
        close.setOnAction(e -> ctx.stage.close());

        HBox bar = new HBox(10, logo, titleText, Ui.grow(), min, close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("titlebar");

        bar.setOnMousePressed(e -> {
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });
        bar.setOnMouseDragged(e -> {
            ctx.stage.setX(e.getScreenX() - dragX);
            ctx.stage.setY(e.getScreenY() - dragY);
        });
        return bar;
    }

    private VBox sidebar() {
        VBox nav = new VBox();
        nav.getStyleClass().add("nav");
        nav.getChildren().addAll(
                navButton("Home", () -> new HomeView(ctx).getRoot()),
                navButton("Accounts", () -> new AccountsView(ctx).getRoot()),
                navButton("Optimization", () -> new OptimizationView(ctx).getRoot()),
                navButton("Settings", () -> new SettingsView(ctx).getRoot()));

        Region spring = new Region();
        VBox.setVgrow(spring, Priority.ALWAYS);
        nav.getChildren().add(spring);
        nav.getChildren().add(Ui.label("v" + ctx.cfg().selectedVersion, "muted"));
        return nav;
    }

    private Button navButton(String name, Supplier<Node> viewSupplier) {
        Button b = new Button(name);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> select(name, viewSupplier));
        navButtons.put(name, b);
        return b;
    }

    private void select(String name, Supplier<Node> viewSupplier) {
        navButtons.forEach((n, btn) -> {
            btn.getStyleClass().remove("nav-button-active");
            if (n.equals(name)) {
                btn.getStyleClass().add("nav-button-active");
            }
        });
        Node view = viewSupplier.get();
        contentArea.getChildren().setAll(view);
        Animations.enterView(view);
    }

    public void applyPreferences() {
        ctx.stage.setOpacity(Math.max(0.2, ctx.cfg().launcherOpacity / 100.0));
        if (root != null) {
            Theme.applyFontAndScale(root, ctx.cfg().uiFont, ctx.cfg().uiScale);
        }
    }
}
