package com.cmsc13;

/**
 * Plain entry point used by the "fat JAR" / .exe build.
 * <p>
 * Java refuses to launch a JavaFX {@code Application} subclass directly from a
 * JAR that bundles JavaFX on the classpath, so this class (which does not extend
 * {@code Application}) is the JAR's Main-Class and simply delegates to {@link Main}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
