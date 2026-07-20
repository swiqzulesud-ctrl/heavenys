package com.heavenys.launcher.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** Small library of smooth, eased UI transitions (fade / slide / scale). */
public final class Animations {
    private Animations() {
    }

    public static void fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    /** Fade + gentle upward slide, used when switching views. */
    public static void enterView(Node node) {
        node.setOpacity(0);
        node.setTranslateY(12);
        FadeTransition ft = new FadeTransition(Duration.millis(240), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition tt = new TranslateTransition(Duration.millis(240), node);
        tt.setFromY(12);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
        tt.play();
    }

    public static void pop(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(0.96);
        st.setFromY(0.96);
        st.setToX(1);
        st.setToY(1);
        st.setInterpolator(Interpolator.EASE_OUT);
        st.play();
    }
}
