package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.frontend.ui.PixelText;
import com.cmsc13.model.QuestionBank;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Splash screen (mockup 2.1). The real work (parsing the question bank and
 * loading audio) runs on a background thread while an {@link AnimationTimer}
 * with delta-time accumulation fills the loading bar drawn in the mockup.
 */
public class SplashController implements ScreenController {

    /** Inner width of the loading bar in the splash artwork. */
    private static final double BAR_WIDTH = 321;

    @FXML private Rectangle barFill;
    @FXML private PixelText percentText;

    private volatile boolean loaded;
    private volatile String loadError;
    private double progress;
    private double elapsed;
    private long lastTime;
    private boolean finished;
    private int lastPercent = -1;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastTime == 0) {
                lastTime = now;
                return;
            }
            double delta = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            elapsed += delta;
            update(delta);
        }
    };

    @Override
    public void onShow() {
        Thread loader = new Thread(() -> {
            try {
                QuestionBank.getInstance();
            } catch (Exception e) {
                loadError = e.getMessage();
            }
            SoundManager.get().preload();
            loaded = true;
            Platform.runLater(() -> SoundManager.get().playMusic(Sound.MAIN_THEME));
        }, "asset-loader");
        loader.setDaemon(true);
        loader.start();
        timer.start();
    }

    @Override
    public void onHide() {
        timer.stop();
    }

    private void update(double delta) {
        // Ease toward 90% while loading, then sprint to 100% once everything is ready.
        double target = loaded ? 1.0 : Math.min(0.9, elapsed / 3.0);
        double speed = loaded && elapsed > 2.4 ? 0.9 : 0.35;
        progress = Math.min(target, progress + speed * delta);

        barFill.setWidth(BAR_WIDTH * progress);
        int pct = (int) Math.round(progress * 100);
        if (pct != lastPercent) {
            lastPercent = pct;
            percentText.setText(pct + "%");
        }

        if (loadError != null) {
            percentText.setText("ERR");
            percentText.setColor(javafx.scene.paint.Color.web("#ff3b5c"));
            timer.stop();
            System.err.println("Could not load the question bank: " + loadError);
            return;
        }
        if (progress >= 1.0 && !finished) {
            finished = true;
            PauseTransition wait = new PauseTransition(Duration.millis(600));
            wait.setOnFinished(e -> SceneManager.show(Screen.MAIN_MENU));
            wait.play();
        }
    }

    @FXML
    private void skip() {
        if (loaded && loadError == null && !finished) {
            finished = true;
            SceneManager.show(Screen.MAIN_MENU);
        }
    }
}
