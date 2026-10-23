package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.PlayerStats;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Final "AUDIT PASSED: Employment Retained" screen with the run's stats. */
public class VictoryController implements ScreenController {

    @FXML private VBox banner;
    @FXML private Label statsLabel;
    @FXML private Button againButton;

    private PauseTransition later;
    private Animation glowAnim;

    @Override
    public void onShow() {
        SoundManager.get().stopMusic();
        SoundManager.get().play(Sound.VICTORY);

        GameSession s = AppState.getSession();
        String name = s != null ? s.getPlayerName() : "Architect";
        long millis = s != null ? s.getElapsedMillis() : 0;
        PlayerStats.get().recordWin(millis);

        statsLabel.setText(String.format("%s KEPT THEIR JOB   ·   TIME %s   ·   ATTEMPTS %d   ·   LIFELINES %d/3   ·   CONFESSIONS ON FILE %d",
                name.toUpperCase(), PlayerStats.formatDuration(millis), s != null ? s.getAttemptNumber() : 1,
                s != null ? s.getLifelinesUsed() : 0, PlayerStats.get().getConfessions()));

        // pulse the banner's glow (scaling would blur the pixel font)
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow(30, javafx.scene.paint.Color.web("#1e7fff"));
        banner.setEffect(glow);
        javafx.animation.Timeline glowPulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO, new javafx.animation.KeyValue(glow.radiusProperty(), 18)),
                new javafx.animation.KeyFrame(Duration.millis(900), new javafx.animation.KeyValue(glow.radiusProperty(), 48)));
        glowPulse.setAutoReverse(true);
        glowPulse.setCycleCount(Animation.INDEFINITE);
        glowPulse.play();
        glowAnim = glowPulse;

        againButton.requestFocus();
        later = new PauseTransition(Duration.seconds(4));
        later.setOnFinished(e -> SoundManager.get().playMusic(Sound.MAIN_THEME));
        later.play();
    }

    @Override
    public void onHide() {
        if (later != null) {
            later.stop();
        }
        if (glowAnim != null) {
            glowAnim.stop();
        }
    }

    @FXML
    private void menu() {
        AppState.setSession(null);
        SceneManager.show(Screen.MAIN_MENU);
    }

    @FXML
    private void again() {
        AppState.setSession(null);
        SceneManager.show(Screen.CHECK_IN);
    }
}
