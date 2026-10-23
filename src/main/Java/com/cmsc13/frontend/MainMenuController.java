package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.PlayerStats;
import com.cmsc13.model.SurvivalLadder;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Main menu (mockup 2.2): Play Game, How to Play, Settings, Exit. */
public class MainMenuController implements ScreenController {

    @FXML private Pane ledLayer;
    @FXML private Button playButton;
    @FXML private Label statsLabel;

    private final List<Rectangle> leds = new ArrayList<>();
    private final Random random = new Random();
    private Timeline ledBlink;

    @FXML
    private void initialize() {
        // Little blinking lights over the server rack drawn on the left.
        Color[] palette = {Color.web("#43ff6b"), Color.web("#3fc8ff"), Color.web("#ffb000")};
        int[] rowsY = {367, 397, 428, 458, 488, 518, 548, 574};
        for (int r = 0; r < rowsY.length; r++) {
            for (int i = 0; i < 3; i++) {
                Rectangle led = new Rectangle(4, 3, palette[(r + i) % palette.length]);
                led.setLayoutX(66 + i * 22 + (r % 2) * 7);
                led.setLayoutY(rowsY[r]);
                led.setEffect(new DropShadow(6, (Color) led.getFill()));
                led.setOpacity(0);
                leds.add(led);
                ledLayer.getChildren().add(led);
            }
        }
        ledBlink = new Timeline(new KeyFrame(Duration.millis(160), e -> {
            for (Rectangle led : leds) {
                if (random.nextDouble() < 0.15) {
                    led.setOpacity(led.getOpacity() > 0.3 ? 0 : 0.9);
                }
            }
        }));
        ledBlink.setCycleCount(Animation.INDEFINITE);

        PlayerStats stats = PlayerStats.get();
        statsLabel.setText(String.format("BEST: LEVEL %d/%d   ·   AUDITS: %d   ·   CONFESSIONS: %d   ·   SURVIVED: %d",
                stats.getBestRung(), SurvivalLadder.SIZE, stats.getAttempts(), stats.getConfessions(), stats.getWins()));
    }

    @Override
    public void onShow() {
        SoundManager.get().playMusic(Sound.MAIN_THEME);
        ledBlink.play();
        playButton.requestFocus();
    }

    @Override
    public void onHide() {
        ledBlink.stop();
    }

    @FXML
    private void play() {
        SceneManager.show(Screen.CHECK_IN);
    }

    @FXML
    private void howToPlay() {
        SceneManager.show(Screen.INSTRUCTIONS);
    }

    @FXML
    private void settings() {
        SceneManager.show(Screen.SETTINGS);
    }

    @FXML
    private void exit() {
        SceneManager.getStage().close();
    }
}
