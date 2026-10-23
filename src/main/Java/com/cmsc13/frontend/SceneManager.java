package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Owns the single window of the game and swaps screens inside it.
 * <p>
 * Every screen is designed at a fixed 1280x720 resolution and is scaled
 * uniformly to fit the window, so the layout looks identical on any display
 * (and in full screen, toggled with F11).
 */
public final class SceneManager {

    public static final double WIDTH = 1280;
    public static final double HEIGHT = 720;

    /** All screens of the game and the FXML view that defines each one. */
    public enum Screen {
        SPLASH("SplashScreen.fxml"),
        MAIN_MENU("MainMenu.fxml"),
        CHECK_IN("StartScreen.fxml"),
        INSTRUCTIONS("Instructions.fxml"),
        SETTINGS("Settings.fxml"),
        GAME("Game.fxml"),
        LEVEL_COMPLETE("LevelComplete.fxml"),
        DEFEAT("Defeat.fxml"),
        CONFESSION("Camera.fxml"),
        REBOOT("Reboot.fxml"),
        VICTORY("Victory.fxml");

        private final String fxml;

        Screen(String fxml) {
            this.fxml = fxml;
        }
    }

    private static Stage stage;
    private static Pane content;
    private static ScreenController currentController;

    private SceneManager() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;

        content = new Pane();
        content.setPrefSize(WIDTH, HEIGHT);
        content.setMinSize(WIDTH, HEIGHT);
        content.setMaxSize(WIDTH, HEIGHT);
        content.getStyleClass().add("app-root");

        Group scaled = new Group(content);
        StackPane viewport = new StackPane(scaled);
        viewport.setStyle("-fx-background-color: #02040a;");

        Scale scale = new Scale(1, 1, 0, 0);
        content.getTransforms().add(scale);
        Runnable fit = () -> {
            double s = Math.min(viewport.getWidth() / WIDTH, viewport.getHeight() / HEIGHT);
            if (s > 0) {
                scale.setX(s);
                scale.setY(s);
            }
        };
        viewport.widthProperty().addListener((o, a, b) -> fit.run());
        viewport.heightProperty().addListener((o, a, b) -> fit.run());

        // Start at the largest 16:9 size that comfortably fits the screen.
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double s = Math.min(1.0, Math.min(bounds.getWidth() * 0.92 / WIDTH, bounds.getHeight() * 0.92 / HEIGHT));
        Scene scene = new Scene(viewport, WIDTH * s, HEIGHT * s, Color.BLACK);
        scene.getStylesheets().add(SceneManager.class.getResource("/css/neon.css").toExternalForm());

        installButtonSounds(scene);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                e.consume();
            }
        });

        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(400);
        stage.setFullScreenExitHint("Press F11 or Esc to leave full screen");
    }

    /** Hover and click sounds for every button in every screen. */
    private static void installButtonSounds(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, e -> {
            if (e.getTarget() instanceof ButtonBase b && !b.isDisabled()) {
                SoundManager.get().play(Sound.HOVER);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && isInsideEnabledButton(e.getTarget())) {
                SoundManager.get().play(Sound.PRESS);
            }
        });
    }

    private static boolean isInsideEnabledButton(Object target) {
        Object node = target;
        while (node instanceof javafx.scene.Node n) {
            if (n instanceof ButtonBase b) {
                return !b.isDisabled();
            }
            node = n.getParent();
        }
        return false;
    }

    /** Shows a screen and returns its controller (for passing data in). */
    @SuppressWarnings("unchecked")
    public static <T> T show(Screen screen) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/" + screen.fxml));
            Parent root = loader.load();
            Object controller = loader.getController();

            if (currentController != null) {
                currentController.onHide();
            }
            currentController = (controller instanceof ScreenController sc) ? sc : null;

            if (root instanceof javafx.scene.layout.Region r) {
                r.setPrefSize(WIDTH, HEIGHT);
                r.setMinSize(WIDTH, HEIGHT);
                r.setMaxSize(WIDTH, HEIGHT);
            }
            content.getChildren().setAll(root);

            FadeTransition fade = new FadeTransition(Duration.millis(260), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

            if (currentController != null) {
                currentController.onShow();
            }
            return (T) controller;
        } catch (IOException e) {
            throw new IllegalStateException("Could not open screen " + screen + ": " + e.getMessage(), e);
        }
    }

    /** Called when the window closes so cameras, timers and audio are released. */
    public static void shutdown() {
        if (currentController != null) {
            currentController.onHide();
            currentController = null;
        }
    }

    static Object currentController() {
        return currentController;
    }

    public static Stage getStage() {
        return stage;
    }
}
