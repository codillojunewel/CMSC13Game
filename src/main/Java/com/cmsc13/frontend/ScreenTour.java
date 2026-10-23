package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.Settings;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Developer tool: opens every screen in turn and saves a PNG of each one to
 * ./tour-screenshots (handy for the MP documentation). The webcam is NOT used.
 * <p>
 * Run with:  mvn javafx:run -Dapp.main=com.cmsc13.frontend.ScreenTour
 */
public class ScreenTour extends Application {

    private final List<Runnable> steps = new ArrayList<>();
    private final File outDir = new File(System.getProperty("user.dir"), "tour-screenshots");
    private GameController game;
    private Object current;

    @Override
    public void start(Stage stage) {
        outDir.mkdirs();
        Settings.get().setWebcamEnabled(false); // not saved: tour never opens the camera
        com.cmsc13.model.PlayerStats.get().setFrozen(true); // tour must not change career stats
        SoundManager.get().preload();
        SceneManager.init(stage);
        stage.setTitle("Screen tour");
        stage.show();

        step(1500, () -> SceneManager.show(Screen.SPLASH), "01_splash");
        step(1500, () -> SceneManager.show(Screen.MAIN_MENU), "02_main_menu");
        step(900, () -> SceneManager.show(Screen.CHECK_IN), "03_start");
        step(1600, () -> ((CheckInController) current).tourOpen(), "04_check_in");
        step(900, () -> SceneManager.show(Screen.INSTRUCTIONS), "05_how_to_play");
        step(900, () -> SceneManager.show(Screen.SETTINGS), "06_settings");
        step(1400, () -> {
            AppState.setSession(new GameSession("Carl", 1));
            game = SceneManager.show(Screen.GAME);
        }, "07_game_level1");
        step(900, () -> game.tourLifeline(0), "08_game_peek");
        step(500, () -> game.tourSelect(AppState.getSession().getCurrent().getCorrectIndex()), "09_game_selected");
        step(3000, () -> game.tourCommit(), "10_game_result_correct");
        step(1500, () -> game.tourContinue(), "11_level_complete");
        for (int rung : new int[]{2, 4, 6, 8}) {
            final int target = rung;
            step(1500, () -> {
                GameSession s = new GameSession("Carl", 2);
                for (int i = 0; i < target; i++) {
                    s.nextQuestion();
                    s.commit(s.getCurrent().getCorrectIndex());
                }
                AppState.setSession(s);
                game = SceneManager.show(Screen.GAME);
            }, "12_game_level" + (rung + 1));
        }
        step(500, () -> game.tourSelect((AppState.getSession().getCurrent().getCorrectIndex() + 1) % 4), "13_game_wrong_selected");
        step(3200, () -> game.tourCommit(), "14_game_result_crash");
        step(2500, () -> game.tourContinue(), "15_defeat");
        step(1500, () -> SceneManager.show(Screen.CONFESSION), "16_confession");
        step(6500, () -> SceneManager.show(Screen.REBOOT), "17_reboot");
        step(1800, () -> {
            GameSession s = new GameSession("Carl", 3);
            for (int i = 0; i < 10; i++) {
                s.nextQuestion();
                s.commit(s.getCurrent().getCorrectIndex());
            }
            AppState.setSession(s);
            SceneManager.show(Screen.VICTORY);
        }, "18_victory");
        steps.add(Platform::exit);
        runNext();
    }

    private void step(double waitMillis, Runnable action, String shotName) {
        steps.add(() -> {
            action.run();
            current = SceneManager.currentController();
            PauseTransition p = new PauseTransition(Duration.millis(waitMillis));
            p.setOnFinished(e -> {
                snap(shotName);
                runNext();
            });
            p.play();
        });
    }

    private void runNext() {
        if (!steps.isEmpty()) {
            steps.remove(0).run();
        }
    }

    private void snap(String name) {
        try {
            WritableImage img = SceneManager.getStage().getScene().snapshot(null);
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(outDir, name + ".png"));
            System.out.println("[tour] saved " + name);
        } catch (Exception e) {
            System.out.println("[tour] failed " + name + ": " + e);
        }
    }

    @Override
    public void stop() {
        SceneManager.shutdown();
        SoundManager.get().shutdown();
    }
}
