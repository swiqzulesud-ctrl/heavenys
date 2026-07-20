package com.heavenys.launcher.ui.views;

import com.heavenys.launcher.model.LauncherConfig;
import com.heavenys.launcher.ui.LauncherContext;
import com.heavenys.launcher.ui.Ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Optimization page — toggle the Fabulously-Optimized-style mod stack and pick a performance
 * preset. Choices persist to the config so the launcher can enable the matching mods at launch.
 */
public class OptimizationView {
    private record Mod(String id, String name, String description) {
    }

    private static final List<Mod> MODS = List.of(
            new Mod("sodium", "Sodium", "Modern rendering engine — huge FPS gains."),
            new Mod("lithium", "Lithium", "Optimizes game logic & tick performance (no behaviour change)."),
            new Mod("ferritecore", "FerriteCore", "Reduces memory usage of block states & models."),
            new Mod("entityculling", "Entity Culling", "Skips rendering entities/BEs hidden from view."),
            new Mod("immediatelyfast", "ImmediatelyFast", "Speeds up immediate-mode (HUD/text) rendering."),
            new Mod("moreculling", "More Culling", "Extra block/entity culling for higher FPS."),
            new Mod("krypton", "Krypton", "Optimizes the Minecraft networking stack."),
            new Mod("dynamicfps", "Dynamic FPS", "Lowers FPS when the window is unfocused."),
            new Mod("fastquit", "FastQuit", "Return to menu instantly; saves flush in the background."),
            new Mod("noisium", "Noisium", "Faster world generation / chunk loading."),
            new Mod("ebe", "Enhanced Block Entities", "Optimizes block-entity rendering (chests, signs…)."),
            new Mod("memoryleakfix", "Memory Leak Fix", "Patches several known memory leaks."));

    private static final Map<String, List<String>> PRESETS = Map.of(
            "Quality", List.of("sodium", "ferritecore", "ebe", "memoryleakfix"),
            "Balanced", List.of("sodium", "lithium", "ferritecore", "entityculling", "immediatelyfast", "memoryleakfix"),
            "Competitive", List.of("sodium", "lithium", "ferritecore", "entityculling", "immediatelyfast",
                    "moreculling", "krypton", "fastquit", "memoryleakfix"),
            "Ultra FPS", List.of("sodium", "lithium", "ferritecore", "entityculling", "immediatelyfast",
                    "moreculling", "krypton", "dynamicfps", "fastquit", "noisium", "ebe", "memoryleakfix"));

    private final LauncherContext ctx;
    private final VBox modList = new VBox(8);

    public OptimizationView(LauncherContext ctx) {
        this.ctx = ctx;
    }

    public Node getRoot() {
        LauncherConfig cfg = ctx.cfg();
        // Default-enable a sane set on first visit.
        if (cfg.optimizationMods.isEmpty()) {
            applyPreset("Balanced");
        }

        ComboBox<String> preset = new ComboBox<>();
        preset.getStyleClass().add("combo-box");
        preset.getItems().addAll("Quality", "Balanced", "Competitive", "Ultra FPS");
        preset.setValue(cfg.performancePreset);
        preset.valueProperty().addListener((o, a, b) -> {
            cfg.performancePreset = b;
            applyPreset(b);
            rebuild();
        });

        VBox header = Ui.card("Performance preset",
                Ui.label("Pick a starting point, then fine-tune individual optimizations below.", "muted"),
                Ui.labeledRow("Preset", preset));

        rebuild();
        VBox content = new VBox(14, Ui.label("Optimization", "h1"), header, Ui.card("Optimization mods", modList));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        return scroll;
    }

    private void applyPreset(String name) {
        LauncherConfig cfg = ctx.cfg();
        List<String> on = PRESETS.getOrDefault(name, List.of());
        for (Mod m : MODS) {
            cfg.optimizationMods.put(m.id(), on.contains(m.id()));
        }
        cfg.performancePreset = name;
        ctx.save();
    }

    private void rebuild() {
        LauncherConfig cfg = ctx.cfg();
        modList.getChildren().clear();
        for (Mod m : MODS) {
            CheckBox cb = new CheckBox();
            cb.setSelected(cfg.optimizationMods.getOrDefault(m.id(), false));
            cb.setOnAction(e -> {
                cfg.optimizationMods.put(m.id(), cb.isSelected());
                ctx.save();
            });
            VBox text = new VBox(1, Ui.label(m.name(), "h2"), Ui.label(m.description(), "muted"));
            HBox row = new HBox(12, cb, text);
            row.setAlignment(Pos.CENTER_LEFT);
            modList.getChildren().add(row);
        }
    }
}
