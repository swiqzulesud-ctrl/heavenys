package com.heavenys.launcher;

/**
 * Plain bootstrap entrypoint used by the packaged/fat-jar builds.
 *
 * <p>When JavaFX is on the classpath (fat jar / jpackage app-image) rather than the module path,
 * the JVM refuses to run a {@code main} that lives in an {@link javafx.application.Application}
 * subclass ("JavaFX runtime components are missing"). Delegating through this non-Application
 * class avoids that restriction.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        HeavenysLauncher.main(args);
    }
}
