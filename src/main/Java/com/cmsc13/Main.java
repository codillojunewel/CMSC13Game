package com.cmsc13;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.frontend.SceneManager;
import com.cmsc13.frontend.SceneManager.Screen;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Entry point of "Are You Smarter Than a Junior Dev?".
 * <p>
 * Architecture (MVC):
 * <ul>
 *   <li><b>Model</b> - {@code com.cmsc13.model}: questions, ladder, personas, lifelines, session rules.</li>
 *   <li><b>View</b> - FXML layouts in {@code /fxml} styled by {@code /css/neon.css}.</li>
 *   <li><b>Controller</b> - {@code com.cmsc13.frontend}: one controller per screen.</li>
 * </ul>
 */
public class Main extends Application {

    private static HostServices hostServices;

    @Override
    public void start(Stage stage) {
        hostServices = getHostServices();

        stage.setTitle("Are You Smarter Than a Junior Dev?");
        try (InputStream icon = Main.class.getResourceAsStream("/images/icon.png")) {
            if (icon != null) {
                stage.getIcons().add(new Image(icon));
            }
        } catch (Exception ignored) {
        }

        SceneManager.init(stage);
        SceneManager.show(Screen.SPLASH);
        stage.show();
    }

    @Override
    public void stop() {
        SceneManager.shutdown();
        SoundManager.get().shutdown();
    }

    /** Lets screens open folders/links in the OS (e.g. the confessions folder). */
    public static HostServices hostServices() {
        return hostServices;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
