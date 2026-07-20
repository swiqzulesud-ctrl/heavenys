package com.heavenys.launcher.ui.views;

import com.heavenys.launcher.core.SystemInfo;
import com.heavenys.launcher.model.LauncherConfig;
import com.heavenys.launcher.ui.LauncherContext;
import com.heavenys.launcher.ui.Theme;
import com.heavenys.launcher.ui.Ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

/** All launcher & game settings, grouped into clear sections. */
public class SettingsView {
    private final LauncherContext ctx;

    public SettingsView(LauncherContext ctx) {
        this.ctx = ctx;
    }

    public Node getRoot() {
        LauncherConfig cfg = ctx.cfg();

        VBox content = new VBox(14,
                Ui.label("Settings", "h1"),
                javaMemoryCard(cfg),
                videoCard(cfg),
                directoriesCard(cfg),
                launcherCard(cfg),
                configCard(cfg));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private VBox javaMemoryCard(LauncherConfig cfg) {
        long total = SystemInfo.totalRamMb();
        long cap = Math.max(4096, total);

        Label minLabel = Ui.label(cfg.minRamMb + " MB", "gold");
        Slider min = slider(512, cap, cfg.minRamMb, 512);
        min.valueProperty().addListener((o, a, b) -> {
            cfg.minRamMb = round512(b.intValue());
            minLabel.setText(cfg.minRamMb + " MB");
            ctx.save();
        });

        Label maxLabel = Ui.label(cfg.maxRamMb + " MB", "gold");
        Slider max = slider(512, cap, cfg.maxRamMb, 512);
        max.valueProperty().addListener((o, a, b) -> {
            cfg.maxRamMb = round512(b.intValue());
            maxLabel.setText(cfg.maxRamMb + " MB");
            ctx.save();
        });

        ComboBox<String> gc = combo(cfg.gcType, "G1GC", "ZGC", "ShenandoahGC");
        gc.valueProperty().addListener((o, a, b) -> { cfg.gcType = b; ctx.save(); });

        TextField extra = new TextField(cfg.extraJvmArgs);
        extra.setPromptText("-XX:+AlwaysPreTouch ...");
        extra.getStyleClass().add("text-field");
        extra.textProperty().addListener((o, a, b) -> { cfg.extraJvmArgs = b; ctx.save(); });

        TextField javaPath = new TextField(cfg.javaPath);
        javaPath.setPromptText("Auto-detected: " + SystemInfo.detectJavaPath());
        javaPath.getStyleClass().add("text-field");
        javaPath.textProperty().addListener((o, a, b) -> { cfg.javaPath = b; ctx.save(); });
        Button browseJava = ghost("Browse", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Java executable");
            File f = fc.showOpenDialog(ctx.stage);
            if (f != null) {
                javaPath.setText(f.getAbsolutePath());
            }
        });

        return Ui.card("Java & Memory",
                Ui.labeledRow("Minimum RAM", Ui.row(min, minLabel)),
                Ui.labeledRow("Maximum RAM", Ui.row(max, maxLabel)),
                Ui.labeledRow("Garbage Collector", gc),
                Ui.labeledRow("JVM Arguments", extra),
                Ui.labeledRow("Java Path", Ui.row(javaPath, browseJava)));
    }

    private VBox videoCard(LauncherConfig cfg) {
        TextField w = numberField(cfg.resolutionWidth, v -> { cfg.resolutionWidth = v; ctx.save(); });
        TextField h = numberField(cfg.resolutionHeight, v -> { cfg.resolutionHeight = v; ctx.save(); });
        ComboBox<String> preset = combo(cfg.resolutionWidth + "x" + cfg.resolutionHeight,
                "854x480", "1280x720", "1600x900", "1920x1080");
        preset.valueProperty().addListener((o, a, b) -> {
            if (b != null && b.contains("x")) {
                String[] p = b.split("x");
                cfg.resolutionWidth = Integer.parseInt(p[0]);
                cfg.resolutionHeight = Integer.parseInt(p[1]);
                w.setText(p[0]);
                h.setText(p[1]);
                ctx.save();
            }
        });
        CheckBox fullscreen = new CheckBox("Fullscreen");
        fullscreen.setSelected(cfg.fullscreen);
        fullscreen.setOnAction(e -> { cfg.fullscreen = fullscreen.isSelected(); ctx.save(); });

        return Ui.card("Video",
                Ui.labeledRow("Preset", preset),
                Ui.labeledRow("Resolution", Ui.row(w, Ui.label("×"), h)),
                Ui.labeledRow("", fullscreen));
    }

    private VBox directoriesCard(LauncherConfig cfg) {
        TextField dir = new TextField(cfg.gameDirectory);
        dir.setPromptText(ctx.launchService.resolveGameDir(cfg).toString());
        dir.getStyleClass().add("text-field");
        Label sub = Ui.label("mods/ · screenshots/ · resourcepacks/ live under the game directory.", "muted");
        dir.textProperty().addListener((o, a, b) -> { cfg.gameDirectory = b; ctx.save(); });
        Button browse = ghost("Browse", () -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select game directory");
            File f = dc.showDialog(ctx.stage);
            if (f != null) {
                dir.setText(f.getAbsolutePath());
            }
        });

        ComboBox<String> version = combo(cfg.selectedVersion,
                "1.21.11", "1.21.10", "1.21.9", "1.21.8");
        version.valueProperty().addListener((o, a, b) -> { cfg.selectedVersion = b; ctx.save(); });

        return Ui.card("Directories & Version",
                Ui.labeledRow("Game Directory", Ui.row(dir, browse)),
                sub,
                Ui.labeledRow("Version", version));
    }

    private VBox launcherCard(LauncherConfig cfg) {
        Label opacityLabel = Ui.label(cfg.launcherOpacity + "%", "gold");
        Slider opacity = slider(20, 100, cfg.launcherOpacity, 5);
        opacity.valueProperty().addListener((o, a, b) -> {
            cfg.launcherOpacity = b.intValue();
            opacityLabel.setText(cfg.launcherOpacity + "%");
            ctx.applyPreferences.run();
            ctx.save();
        });

        ComboBox<String> font = combo(cfg.uiFont, Theme.FONT_OPTIONS.toArray(new String[0]));
        font.valueProperty().addListener((o, a, b) -> {
            cfg.uiFont = b;
            ctx.applyPreferences.run();
            ctx.save();
        });

        Label scaleLabel = Ui.label(String.format("%.0f%%", cfg.uiScale * 100), "gold");
        Slider scale = slider(80, 140, (int) Math.round(cfg.uiScale * 100), 10);
        scale.valueProperty().addListener((o, a, b) -> {
            cfg.uiScale = b.intValue() / 100.0;
            scaleLabel.setText(String.format("%.0f%%", cfg.uiScale * 100));
            ctx.applyPreferences.run();
            ctx.save();
        });

        CheckBox dark = new CheckBox("Dark mode");
        dark.setSelected(cfg.darkMode);
        dark.setOnAction(e -> { cfg.darkMode = dark.isSelected(); ctx.save(); });

        CheckBox discord = new CheckBox("Discord Rich Presence");
        discord.setSelected(cfg.discordRichPresence);
        discord.setOnAction(e -> { cfg.discordRichPresence = discord.isSelected(); ctx.save(); });

        return Ui.card("Launcher",
                Ui.labeledRow("Opacity", Ui.row(opacity, opacityLabel)),
                Ui.labeledRow("UI Font", font),
                Ui.labeledRow("UI Scale", Ui.row(scale, scaleLabel)),
                Ui.labeledRow("", dark),
                Ui.labeledRow("", discord));
    }

    private VBox configCard(LauncherConfig cfg) {
        TextField profileName = new TextField();
        profileName.setPromptText("Profile name");
        profileName.getStyleClass().add("text-field");
        Label status = Ui.label("", "muted");

        Button saveProfile = ghost("Save Profile", () -> {
            if (!profileName.getText().isBlank()) {
                ctx.configManager.saveProfile(profileName.getText());
                status.setText("Saved profile '" + profileName.getText() + "'.");
            }
        });
        ComboBox<String> profiles = new ComboBox<>();
        profiles.getStyleClass().add("combo-box");
        profiles.getItems().addAll(ctx.configManager.listProfiles());
        Button loadProfile = ghost("Load", () -> {
            if (profiles.getValue() != null) {
                ctx.configManager.loadProfile(profiles.getValue());
                ctx.applyPreferences.run();
                status.setText("Loaded profile '" + profiles.getValue() + "'. Reopen Settings to see all values.");
            }
        });
        Button backup = ghost("Backup", () -> status.setText("Backed up to " + ctx.configManager.backup()));
        Button reset = ghost("Reset to defaults", () -> {
            ctx.configManager.restore(ctx.configManager.backup()); // safety backup first
            ctx.configManager.get().accounts.clear();
            status.setText("A safety backup was created. Edit values above to reconfigure.");
        });
        reset.getStyleClass().add("danger-button");

        return Ui.card("Configuration",
                Ui.row(profileName, saveProfile),
                Ui.row(profiles, loadProfile, backup, reset),
                status);
    }

    // ---- helpers ----

    private Slider slider(double min, double max, double value, double block) {
        Slider s = new Slider(min, max, value);
        s.setBlockIncrement(block);
        s.setPrefWidth(260);
        return s;
    }

    private ComboBox<String> combo(String value, String... options) {
        ComboBox<String> c = new ComboBox<>();
        c.getStyleClass().add("combo-box");
        c.getItems().addAll(options);
        c.setValue(value);
        c.setPrefWidth(180);
        return c;
    }

    private TextField numberField(int value, java.util.function.IntConsumer onChange) {
        TextField t = new TextField(String.valueOf(value));
        t.getStyleClass().add("text-field");
        t.setPrefWidth(90);
        t.textProperty().addListener((o, a, b) -> {
            try {
                onChange.accept(Integer.parseInt(b.trim()));
            } catch (NumberFormatException ignored) {
            }
        });
        return t;
    }

    private Button ghost(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("ghost-button");
        b.setOnAction(e -> action.run());
        return b;
    }

    private int round512(int v) {
        return Math.max(512, Math.round(v / 512f) * 512);
    }
}
