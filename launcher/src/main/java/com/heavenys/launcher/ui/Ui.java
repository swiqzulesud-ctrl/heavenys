package com.heavenys.launcher.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Tiny builder helpers for consistently-styled controls. */
public final class Ui {
    private Ui() {
    }

    public static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    public static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox card(String title, Node... children) {
        VBox box = new VBox(12);
        box.getStyleClass().add("card");
        if (title != null) {
            box.getChildren().add(label(title, "h2"));
        }
        box.getChildren().addAll(children);
        return box;
    }

    public static HBox row(Node... children) {
        HBox h = new HBox(10, children);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static HBox labeledRow(String labelText, Node control) {
        Label l = label(labelText);
        l.setMinWidth(150);
        HBox h = new HBox(12, l, control);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static VBox section(Node... children) {
        VBox v = new VBox(10, children);
        v.setPadding(new Insets(2));
        return v;
    }
}
